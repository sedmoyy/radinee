package ru.sedmoyy.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public final class RadineeLauncher {
    private static final String NAME = "Radinee Client";
    private static final Path ROOT = Path.of(System.getProperty("user.home"), ".radinee-client");
    private static final Path GAME = ROOT.resolve("game");
    private static final String MC_VERSION = "1.21.11";
    private static final String FABRIC_INSTALLER = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.2/fabric-installer-1.1.2.jar";
    private static final Preferences PREFS = Preferences.userRoot().node("ru.sedmoyy.radinee");

    private final JTextField username = new JTextField(PREFS.get("username", ""));
    private final JSpinner ram = new JSpinner(new SpinnerNumberModel(PREFS.getInt("ram", 4096), 2048, 16384, 512));
    private final JLabel status = new JLabel("Ready to launch.");

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

        JPanel root = new JPanel(new BorderLayout(0, 0));
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

        JLabel account = label("Minecraft username");
        card.add(account);
        card.add(Box.createVerticalStrut(7));
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(username);
        card.add(Box.createVerticalStrut(18));

        JLabel memory = label("RAM (MB)");
        card.add(memory);
        card.add(Box.createVerticalStrut(7));
        ram.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(ram);
        card.add(Box.createVerticalStrut(22));

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

    private String javaBin() {\n        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "javaw.exe" : "java";\n        Path javaHome = Path.of(System.getProperty("java.home"), "bin", exe);\n        return Files.exists(javaHome) ? javaHome.toString() : exe;\n    }\n\n    private void launch() {
        String name = username.getText().trim();
        if (name.isEmpty()) {
            status.setText("Enter your Minecraft username first.");
            return;
        }

        PREFS.put("username", name);
        PREFS.putInt("ram", (Integer) ram.getValue());

        status.setText("Подготовка Radinee Client...");
        try {
            Files.createDirectories(GAME);
            Path installer = ROOT.resolve("fabric-installer.jar");
            if (!Files.exists(installer)) {
                status.setText("Скачивание Fabric Installer...");
                HttpClient http = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create(FABRIC_INSTALLER)).build();
                http.send(HttpRequest.newBuilder(URI.create(FABRIC_INSTALLER)).build(), HttpResponse.BodyHandlers.ofFile(installer));
            }
            status.setText("Установка Fabric " + MC_VERSION + "...");
            Process p = new ProcessBuilder(
                javaBin(), "-jar", installer.toString(), "client",
                "-mcversion", MC_VERSION, "-dir", GAME.toString(), "-noprofile"
            ).redirectErrorStream(true).start();
            int code = p.waitFor();
            if (code != 0) throw new IllegalStateException("Fabric installer завершился с кодом " + code);
            status.setText("Fabric " + MC_VERSION + " установлен. Готов к запуску.");
            JOptionPane.showMessageDialog(null,
                "Radinee Client подготовлен.\n\nMinecraft: " + MC_VERSION + "\nFabric установлен в:\n" + GAME,
                NAME, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            status.setText("Ошибка: " + ex.getMessage());
        }
        JOptionPane.showMessageDialog(
            null,
            "The launcher UI is ready, but the authenticated Minecraft runtime still needs to be bundled/configured.\n\nGame directory:\n" + GAME,
            NAME,
            JOptionPane.INFORMATION_MESSAGE
        );
    }
}
