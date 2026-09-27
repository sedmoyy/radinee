package ru.sedmoyy.launcher;

import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import net.lenni0451.commons.httpclient.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.io.IOException;

public final class RadineeLauncher {
    private static final String NAME = "Radinee Client";
    private static final Path ROOT = Path.of(System.getProperty("user.home"), ".radinee-client");
    private static final Path GAME = ROOT.resolve("game");
    private static final Path AUTH_FILE = ROOT.resolve("microsoft-auth.json");
    private static final String MC_VERSION = "1.21.11";
    private static final Preferences PREFS = Preferences.userRoot().node("ru.sedmoyy.radinee");

    private final JTextField username = new JTextField(PREFS.get("username", ""));
    private final JSpinner ram = new JSpinner(new SpinnerNumberModel(PREFS.getInt("ram", 4096), 2048, 16384, 512));
    private final JLabel status = new JLabel("Ready to launch.");
    private volatile JavaAuthManager authManager;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RadineeLauncher().show());
    }

    private void show() {
        try { Files.createDirectories(GAME); } catch (Exception ignored) {}

        JFrame frame = new JFrame(NAME);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 560));
        frame.setSize(980, 620);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(12, 14, 20));

        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new EmptyBorder(28, 22, 28, 22));
        sidebar.setBackground(new Color(18, 21, 30));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("RADINEE");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 26));
        sidebar.add(logo);

        JLabel sub = new JLabel("CLIENT");
        sub.setForeground(new Color(120, 150, 255));
        sub.setFont(new Font("SansSerif", Font.BOLD, 12));
        sidebar.add(sub);
        sidebar.add(Box.createVerticalStrut(38));
        sidebar.add(sideButton("PLAY", true));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sideButton("SETTINGS", false));
        sidebar.add(Box.createVerticalGlue());

        JLabel version = new JLabel("Radinee Client 0.1.0");
        version.setForeground(new Color(130, 135, 150));
        sidebar.add(version);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBorder(new EmptyBorder(34, 40, 30, 40));
        content.setBackground(new Color(12, 14, 20));

        JLabel title = new JLabel("Radinee Client");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 34));

        JLabel description = new JLabel("<html>Dedicated client instance for your visual modules.<br>It uses its own game directory and does not touch your normal Minecraft setup.</html>");
        description.setForeground(new Color(175, 180, 195));
        description.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(Box.createVerticalStrut(10));
        top.add(description);

        JPanel card = new JPanel();
        card.setBackground(new Color(20, 24, 34));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(label("Minecraft username"));
        card.add(Box.createVerticalStrut(7));
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(username);
        card.add(Box.createVerticalStrut(18));

        card.add(label("RAM (MB)"));
        card.add(Box.createVerticalStrut(7));
        ram.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(ram);
        card.add(Box.createVerticalStrut(22));

        JButton login = new JButton("Войти через Microsoft");
        login.setAlignmentX(Component.LEFT_ALIGNMENT);
        login.setMaximumSize(new Dimension(260, 42));
        login.setFocusPainted(false);
        login.addActionListener(e -> loginMicrosoft());
        card.add(login);
        card.add(Box.createVerticalStrut(10));

        JButton play = new JButton("Запустить");
        play.setAlignmentX(Component.LEFT_ALIGNMENT);
        play.setPreferredSize(new Dimension(220, 46));
        play.setMaximumSize(new Dimension(220, 46));
        play.setBackground(new Color(92, 116, 255));
        play.setForeground(Color.WHITE);
        play.setFocusPainted(false);
        play.addActionListener(e -> launch());
        card.add(play);
        card.add(Box.createVerticalStrut(16));

        status.setForeground(new Color(150, 155, 170));
        card.add(status);

        content.add(top, BorderLayout.NORTH);
        content.add(card, BorderLayout.CENTER);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private JButton sideButton(String text, boolean selected) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(selected ? Color.WHITE : new Color(155, 160, 175));
        b.setBackground(selected ? new Color(38, 45, 65) : new Color(18, 21, 30));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        return b;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(205, 210, 225));
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        return l;
    }

    private void loginMicrosoft() {
        status.setText("Ожидание входа Microsoft...");
        Thread thread = new Thread(() -> {
            try {
                Files.createDirectories(ROOT);
                HttpClient httpClient = MinecraftAuth.createHttpClient("Radinee Client/" + MC_VERSION);
                JavaAuthManager manager;

                if (Files.exists(AUTH_FILE)) {
                    try {
                        manager = JavaAuthManager.fromJson(httpClient,
                                com.google.gson.JsonParser.parseString(Files.readString(AUTH_FILE, StandardCharsets.UTF_8)).getAsJsonObject());
                        manager.getMinecraftToken().getUpToDate();
                    } catch (Exception ignored) {
                        manager = loginWithDeviceCode(httpClient);
                    }
                } else {
                    manager = loginWithDeviceCode(httpClient);
                }

                authManager = manager;
                saveAuth(manager);
                String profileName = manager.getMinecraftProfile().getUpToDate().getName();

                SwingUtilities.invokeLater(() -> {
                    username.setText(profileName);
                    PREFS.put("username", profileName);
                    status.setText("Выполнен вход: " + profileName);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> status.setText("Ошибка входа: " + ex.getMessage()));
            }
        }, "radinee-microsoft-login");
        thread.setDaemon(true);
        thread.start();
    }

    private JavaAuthManager loginWithDeviceCode(HttpClient httpClient) throws IOException, InterruptedException {
        Consumer<MsaDeviceCode> callback = code -> SwingUtilities.invokeLater(() -> {
            String url = code.getDirectVerificationUri();
            status.setText("Открой ссылку Microsoft для входа.");

            JTextArea area = new JTextArea("Открой эту ссылку в браузере:\n\n" + url + "\n\nВойди в свой Microsoft-аккаунт и вернись сюда.");
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBorder(new EmptyBorder(12, 12, 12, 12));

            JButton open = new JButton("Открыть Microsoft");
            open.addActionListener(e -> {
                try { Desktop.getDesktop().browse(URI.create(url)); } catch (Exception ignored) {}
            });

            JPanel panel = new JPanel(new BorderLayout(0, 10));
            panel.add(area, BorderLayout.CENTER);
            panel.add(open, BorderLayout.SOUTH);
            JOptionPane.showMessageDialog(null, panel, "Вход Microsoft", JOptionPane.INFORMATION_MESSAGE);
        });

        return JavaAuthManager.create(httpClient).login(DeviceCodeMsaAuthService::new, callback);
    }

    private void saveAuth(JavaAuthManager manager) throws Exception {
        Files.writeString(AUTH_FILE, JavaAuthManager.toJson(manager).toString(), StandardCharsets.UTF_8);
    }

    private void launch() {
        status.setText("Проверка авторизации...");

        Thread thread = new Thread(() -> {
            try {
                if (authManager == null) {
                    if (!Files.exists(AUTH_FILE)) throw new IllegalStateException("Сначала войди через Microsoft.");
                    HttpClient http = MinecraftAuth.createHttpClient("Radinee Client/" + MC_VERSION);
                    authManager = JavaAuthManager.fromJson(http,
                            com.google.gson.JsonParser.parseString(Files.readString(AUTH_FILE, StandardCharsets.UTF_8)).getAsJsonObject());
                }

                authManager.getMinecraftToken().getUpToDate();
                authManager.getMinecraftProfile().getUpToDate();
                saveAuth(authManager);

                int memory = (Integer) ram.getValue();
                MinecraftRuntime runtime = new MinecraftRuntime(GAME);
                runtime.launch(authManager, memory, message ->
                        SwingUtilities.invokeLater(() -> status.setText(message)));
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                SwingUtilities.invokeLater(() -> status.setText("Ошибка: " + message));
            }
        }, "radinee-game-launch");

        thread.setDaemon(true);
        thread.start();
    }
}