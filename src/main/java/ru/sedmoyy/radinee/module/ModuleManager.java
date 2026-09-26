package ru.sedmoyy.radinee.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        modules.add(new FullbrightModule());
        modules.add(new NoHurtCamModule());
        modules.add(new CrosshairModule());
        modules.add(new FPSModule());
    }

    public List<Module> getModules() { return modules; }

    public void tick(MinecraftClient client) {
        for (Module module : modules)
            if (module.isEnabled()) module.onTick(client);
    }

    public void toggleMenu(MinecraftClient client) {
        if (client.currentScreen instanceof VisualsScreen) client.setScreen(null);
        else if (client.currentScreen == null) client.setScreen(new VisualsScreen(this));
    }

    private static class VisualsScreen extends Screen {
        private final ModuleManager manager;
        private int panelX, panelY, panelW, rowH;
        protected VisualsScreen(ModuleManager manager) {
            super(Text.literal("Radinee Visuals"));
            this.manager = manager;
        }
        @Override protected void init() {
            panelW = Math.min(430, width - 40);
            rowH = 34;
            panelX = (width - panelW) / 2;
            panelY = (height - (70 + manager.modules.size() * rowH)) / 2;
        }
        @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            renderBackground(ctx, mouseX, mouseY, delta);
            int h = 60 + manager.modules.size() * rowH;
            ctx.fill(panelX, panelY, panelX + panelW, panelY + h, 0xEE101218);
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 3, 0xFF7C4DFF);
            ctx.drawTextWithShadow(textRenderer, "RADINEE", panelX + 18, panelY + 14, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "VISUALS", panelX + 18, panelY + 30, 0xFFB9A7FF);
            int y = panelY + 54;
            for (Module module : manager.modules) {
                boolean hovered = mouseX >= panelX + 10 && mouseX <= panelX + panelW - 10
                    && mouseY >= y && mouseY < y + rowH;
                ctx.fill(panelX + 10, y, panelX + panelW - 10, y + rowH - 2,
                    hovered ? 0xFF252936 : 0xFF1A1D25);
                ctx.drawTextWithShadow(textRenderer, module.getName(), panelX + 22, y + 10,
                    module.isEnabled() ? 0xFFEFE7FF : 0xFFD2D5DE);
                ctx.drawTextWithShadow(textRenderer, module.isEnabled() ? "ON" : "OFF",
                    panelX + panelW - 55, y + 10,
                    module.isEnabled() ? 0xFF8DFFB2 : 0xFF777D8A);
                y += rowH;
            }
            ctx.drawTextWithShadow(textRenderer, "Right Shift • click a visual to toggle",
                panelX + 18, panelY + h - 18, 0xFF777D8A);
            super.render(ctx, mouseX, mouseY, delta);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
            int y = panelY + 54;
            for (Module module : manager.modules) {
                if (mouseX >= panelX + 10 && mouseX <= panelX + panelW - 10
                    && mouseY >= y && mouseY < y + rowH) {
                    module.toggle(client);
                    return true;
                }
                y += rowH;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        @Override public boolean shouldPause() { return false; }
    }

    private static class FullbrightModule extends Module {
        FullbrightModule() { super("Fullbright"); }
        @Override public void onEnable(MinecraftClient client) {
            if (client.options != null) client.options.getGamma().setValue(16.0);
        }
        @Override public void onDisable(MinecraftClient client) {
            if (client.options != null) client.options.getGamma().setValue(1.0);
        }
    }
    private static class NoHurtCamModule extends Module { NoHurtCamModule() { super("No HurtCam"); } }
    private static class CrosshairModule extends Module { CrosshairModule() { super("Crosshair"); } }
    private static class FPSModule extends Module { FPSModule() { super("FPS Counter"); } }
}
