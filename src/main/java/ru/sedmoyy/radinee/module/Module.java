package ru.sedmoyy.radinee.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    private final String name;
    private final String category;
    private final String description;
    private boolean enabled;

    protected Module(String name, String category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    protected Module(String name) {
        this(name, "Visuals", "");
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }

    public void toggle(MinecraftClient client) {
        enabled = !enabled;
        if (enabled) onEnable(client);
        else onDisable(client);
    }

    public void onEnable(MinecraftClient client) {}
    public void onDisable(MinecraftClient client) {}
    public void onTick(MinecraftClient client) {}
}
