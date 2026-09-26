package ru.sedmoyy.launcher;

import com.google.gson.*;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipInputStream;

final class MinecraftRuntime {
    private static final String MC = "1.21.11";
    private static final String LOADER = "0.19.5";
    private static final String META =
            "https://meta.fabricmc.net/v2/versions/loader/" + MC + "/" + LOADER + "/profile/json";
    private static final String MANIFEST =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final Path game;
    private final Path libraries;
    private final Path assets;
    private final Path natives;
    private final HttpClient http;
    private final Gson gson = new Gson();

    MinecraftRuntime(Path game) {
        this.game = game;
        this.libraries = game.resolve("libraries");
        this.assets = game.resolve("assets");
        this.natives = game.resolve("natives");
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }

    void launch(JavaAuthManager auth, int ramMb, java.util.function.Consumer<String> status) throws Exception {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            throw new IllegalStateException("Radinee Launcher currently supports Windows.");
        }
        checkJava();
        Files.createDirectories(game);
        Files.createDirectories(libraries);
        Files.createDirectories(assets);
        Files.createDirectories(natives);

        MinecraftToken token = auth.getMinecraftToken().getUpToDate();
        MinecraftProfile profile = auth.getMinecraftProfile().getUpToDate();
        if (token == null || profile == null) {
            throw new IllegalStateException("Microsoft account has no valid Minecraft Java profile.");
        }

        status.accept("Получение профилей Minecraft...");
        JsonObject vanilla = getVanillaProfile();
        JsonObject fabric = getJson(META);

        status.accept("Скачивание Minecraft client...");
        Path clientJar = downloadClient(vanilla);

        status.accept("Скачивание библиотек...");
        List<Path> classpath = new ArrayList<>();
        classpath.add(clientJar);
        downloadLibraries(vanilla, classpath, false);
        downloadLibraries(fabric, classpath, true);

        status.accept("Подготовка natives...");
        extractNatives(vanilla);
        extractNatives(fabric);

        status.accept("Скачивание assets...");
        prepareAssets(vanilla);

        String mainClass = fabric.has("mainClass")
                ? fabric.get("mainClass").getAsString()
                : "net.fabricmc.loader.impl.launch.knot.KnotClient";

        String cp = String.join(File.pathSeparator,
                classpath.stream().map(Path::toString).distinct().toList());

        List<String> command = new ArrayList<>();
        command.add(javaBin());
        command.add("-Xms512M");
        command.add("-Xmx" + Math.max(2048, ramMb) + "M");
        command.add("-Djava.library.path=" + natives.toAbsolutePath());
        command.add("-cp");
        command.add(cp);
        command.add(mainClass);

        command.add("--username"); command.add(profile.getName());
        command.add("--uuid"); command.add(profile.getId().toString());
        command.add("--accessToken"); command.add(token.getToken());
        command.add("--userType"); command.add("msa");
        command.add("--version"); command.add(MC);
        command.add("--versionType"); command.add("release");
        command.add("--gameDir"); command.add(game.toAbsolutePath().toString());
        command.add("--assetsDir"); command.add(assets.toAbsolutePath().toString());

        JsonObject assetIndex = vanilla.getAsJsonObject("assetIndex");
        command.add("--assetIndex"); command.add(assetIndex.get("id").getAsString());

        status.accept("Запуск Minecraft...");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(game.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(game.resolve("radinee-launch.log").toFile());
        Process process = pb.start();

        status.accept("Minecraft запущен. Лог: radinee-launch.log");

        CompletableFuture.runAsync(() -> {
            try {
                int exit = process.waitFor();
                if (exit != 0) {
                    status.accept("Minecraft завершился с кодом " + exit +
                            ". Открой radinee-launch.log.");
                } else {
                    status.accept("Minecraft завершён.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void checkJava() {
        int feature = Runtime.version().feature();
        if (feature < 21) {
            throw new IllegalStateException("Нужна Java 21+. Найдена Java " + feature);
        }
    }

    private String javaBin() {
        Path p = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
        if (!Files.exists(p)) {
            p = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        }
        if (!Files.exists(p)) throw new IllegalStateException("Java executable not found.");
        return p.toString();
    }

    private JsonObject getVanillaProfile() throws Exception {
        JsonObject manifest = getJson(MANIFEST);
        for (JsonElement e : manifest.getAsJsonArray("versions")) {
            JsonObject v = e.getAsJsonObject();
            if (MC.equals(v.get("id").getAsString())) {
                return getJson(v.get("url").getAsString());
            }
        }
        throw new IllegalStateException("Minecraft " + MC + " was not found in Mojang manifest.");
    }

    private JsonObject getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(60)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("HTTP " + res.statusCode() + " from " + url);
        }
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private Path downloadClient(JsonObject vanilla) throws Exception {
        JsonObject d = vanilla.getAsJsonObject("downloads").getAsJsonObject("client");
        Path out = game.resolve("versions").resolve(MC).resolve(MC + ".jar");
        return downloadVerified(d, out);
    }

    private void downloadLibraries(JsonObject profile, List<Path> classpath, boolean fabric) throws Exception {
        if (!profile.has("libraries")) return;
        for (JsonElement e : profile.getAsJsonArray("libraries")) {
            JsonObject lib = e.getAsJsonObject();
            if (!allowed(lib)) continue;
            JsonObject downloads = lib.has("downloads") ? lib.getAsJsonObject("downloads") : null;
            if (downloads == null || !downloads.has("artifact")) continue;
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            String path = artifact.get("path").getAsString();
            Path out = libraries.resolve(path);
            downloadVerified(artifact, out);
            classpath.add(out);
        }
    }

    private boolean allowed(JsonObject lib) {
        if (!lib.has("rules")) return true;
        boolean allowed = false;
        for (JsonElement e : lib.getAsJsonArray("rules")) {
            JsonObject rule = e.getAsJsonObject();
            boolean osMatch = !rule.has("os") ||
                    ("windows".equals(rule.getAsJsonObject("os").has("name")
                            ? rule.getAsJsonObject("os").get("name").getAsString() : ""));
            if (osMatch) {
                allowed = "allow".equals(rule.get("action").getAsString());
            }
        }
        return allowed;
    }

    private Path downloadVerified(JsonObject item, Path out) throws Exception {
        Files.createDirectories(out.getParent());
        if (Files.exists(out) && (!item.has("sha1") ||
                sha1(out).equalsIgnoreCase(item.get("sha1").getAsString()))) {
            return out;
        }
        String url = item.get("url").getAsString();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofMinutes(5)).GET().build();
        Path tmp = out.resolveSibling(out.getFileName() + ".part");
        HttpResponse<Path> res = http.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
        if (res.statusCode() != 200) {
            Files.deleteIfExists(tmp);
            throw new IOException("Download failed HTTP " + res.statusCode() + ": " + url);
        }
        if (item.has("sha1") && !sha1(tmp).equalsIgnoreCase(item.get("sha1").getAsString())) {
            Files.deleteIfExists(tmp);
            throw new IOException("SHA-1 mismatch: " + url);
        }
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        return out;
    }

    private void extractNatives(JsonObject profile) throws Exception {
        if (!profile.has("libraries")) return;
        for (JsonElement e : profile.getAsJsonArray("libraries")) {
            JsonObject lib = e.getAsJsonObject();
            if (!allowed(lib)) continue;
            JsonObject downloads = lib.has("downloads") ? lib.getAsJsonObject("downloads") : null;
            if (downloads == null || !downloads.has("classifiers")) continue;
            JsonObject classifiers = downloads.getAsJsonObject("classifiers");
            if (!classifiers.has("natives-windows")) continue;
            Path jar = downloadVerified(classifiers.getAsJsonObject("natives-windows"),
                    libraries.resolve(classifiers.getAsJsonObject("natives-windows").get("path").getAsString()));
            unzipNatives(jar);
        }
    }

    private void unzipNatives(Path jar) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String n = entry.getName();
                if (entry.isDirectory() || n.startsWith("META-INF/")) continue;
                Path out = natives.resolve(Path.of(n).getFileName().toString());
                if (out.getFileName().toString().endsWith(".dll") ||
                        out.getFileName().toString().endsWith(".so") ||
                        out.getFileName().toString().endsWith(".dylib")) {
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void prepareAssets(JsonObject vanilla) throws Exception {
        JsonObject idx = vanilla.getAsJsonObject("assetIndex");
        Path indexFile = assets.resolve("indexes").resolve(idx.get("id").getAsString() + ".json");
        downloadVerified(idx, indexFile);
        JsonObject index = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        JsonObject objects = index.getAsJsonObject("objects");
        int count = 0;
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            String hash = obj.get("hash").getAsString();
            Path out = assets.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
            if (!Files.exists(out)) {
                Files.createDirectories(out.getParent());
                HttpRequest req = HttpRequest.newBuilder(
                        URI.create("https://resources.download.minecraft.net/" +
                                hash.substring(0, 2) + "/" + hash)).GET().build();
                Path tmp = out.resolveSibling(hash + ".part");
                HttpResponse<Path> res = http.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
                if (res.statusCode() != 200) throw new IOException("Asset download failed: " + hash);
                if (!sha1(tmp).equalsIgnoreCase(hash)) {
                    Files.deleteIfExists(tmp);
                    throw new IOException("Asset hash mismatch: " + hash);
                }
                Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
            }
            if (++count % 500 == 0) {
                // Keeps the launcher responsive while a large asset set is downloaded.
            }
        }
    }

    private static String sha1(Path p) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
