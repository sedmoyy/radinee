package ru.sedmoyy.radinee;

import net.minecraft.client.MinecraftClient;

public final class FreeLookState {
    private static boolean active;
    private static float yaw;
    private static float pitch;
    private static float sensitivity = 1.5f;
    private static boolean invertY;

    private FreeLookState() {}

    public static boolean isActive() { return active; }

    public static void begin(MinecraftClient client) {
        if (!active && client.player != null) {
            yaw = client.player.getYaw();
            pitch = client.player.getPitch();
        }
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static void configure(float sensitivityValue, boolean invertYValue) {
        sensitivity = Math.max(0.1f, Math.min(5.0f, sensitivityValue));
        invertY = invertYValue;
    }

    public static void update(double dx, double dy) {
        float factor = sensitivity * 0.1f;
        yaw += (float) (dx * factor);
        pitch += (float) (dy * factor * (invertY ? -1.0f : 1.0f));
        pitch = Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }
}