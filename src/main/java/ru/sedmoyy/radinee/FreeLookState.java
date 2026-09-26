package ru.sedmoyy.radinee;

import net.minecraft.client.MinecraftClient;

public final class FreeLookState {
    private static boolean active;
    private static float yaw;
    private static float pitch;

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

    public static void update(double dx, double dy) {
        yaw += (float) (dx * 0.15);
        pitch += (float) (dy * 0.15);
        pitch = Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }
}