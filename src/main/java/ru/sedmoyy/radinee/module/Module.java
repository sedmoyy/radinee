package ru.sedmoyy.radinee.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    private final String name;
    private boolean enabled;
    protected Module(String name) { this.name = name; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public void toggle(MinecraftClient client) {
        enabled = !enabled;
        if (enabled) onEnable(client); else onDisable(client);
    }
    public void onEnable(MinecraftClient client) {}
    public void onDisable(MinecraftClient client) {}
    public void onTick(MinecraftClient client) {}
}
