package ru.sedmoyy.radinee.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        modules.add(new VisualModule("Player ESP", "Visuals", "Highlights players around you"));
        modules.add(new VisualModule("Item ESP", "Visuals", "Highlights dropped items"));
        modules.add(new VisualModule("Tracers", "Visuals", "Draws lines toward entities"));
        modules.add(new VisualModule("NameTags", "Visuals", "Enhanced entity name tags"));
        modules.add(new VisualModule("Target ESP", "Visuals", "Target highlight effect"));
        modules.add(new VisualModule("Armor HUD", "Visuals", "Shows your armor and durability"));
        modules.add(new VisualModule("China Hat", "Visuals", "Cosmetic player hat"));
        modules.add(new VisualModule("Hit Color", "Visuals", "Custom damage tint"));
        modules.add(new VisualModule("Block Outline", "Visuals", "Custom block selection outline"));
        modules.add(new FullbrightModule());
        modules.add(new VisualModule("No HurtCam", "Visuals", "Removes camera shake"));
        modules.add(new VisualModule("Crosshair", "Visuals", "Custom crosshair"));
        modules.add(new VisualModule("FPS Counter", "HUD", "FPS indicator"));
        modules.add(new VisualModule("CPS Counter", "HUD", "Clicks per second"));
        modules.add(new VisualModule("Keystrokes", "HUD", "Movement and mouse keys"));
    }

    public List<Module> getModules() { return modules; }

    public void tick(MinecraftClient client) {
        for (Module module : modules) if (module.isEnabled()) module.onTick(client);
    }

    public void toggleMenu(MinecraftClient client) {
        if (client.currentScreen instanceof VisualsScreen) client.setScreen(null);
        else if (client.currentScreen == null) client.setScreen(new VisualsScreen(this));
    }

    private static class VisualsScreen extends Screen {
        private final ModuleManager manager;
        private final List<String> categories = Arrays.asList("Visuals", "HUD");
        private int category = 0;
        private String search = "";
        private int panelX, panelY, panelW, panelH;
        private boolean dragging;

        protected VisualsScreen(ModuleManager manager) {
            super(Text.literal("Radinee Visuals"));
            this.manager = manager;
        }

        @Override
        protected void init() {
            panelW = Math.min(720, width - 30);
            panelH = Math.min(500, height - 30);
            panelX = (width - panelW) / 2;
            panelY = (height - panelH) / 2;
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            renderBackground(ctx, mouseX, mouseY, delta);

            ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF20D0F14);
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF8A5CFF);

            // Sidebar
            int sideW = 145;
            ctx.fill(panelX, panelY, panelX + sideW, panelY + panelH, 0xFF12151C);
            ctx.drawTextWithShadow(textRenderer, "RADINEE", panelX + 18, panelY + 18, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "VISUAL CLIENT", panelX + 18, panelY + 33, 0xFF777C89);

            int cy = panelY + 70;
            for (int i = 0; i < categories.size(); i++) {
                boolean selected = i == category;
                boolean hover = mouseX >= panelX + 10 && mouseX < panelX + sideW - 10
                    && mouseY >= cy && mouseY < cy + 34;
                if (selected) ctx.fill(panelX + 10, cy, panelX + sideW - 10, cy + 34, 0xFF2B243C);
                else if (hover) ctx.fill(panelX + 10, cy, panelX + sideW - 10, cy + 34, 0xFF1D2028);
                ctx.drawTextWithShadow(textRenderer, categories.get(i), panelX + 22, cy + 11,
                    selected ? 0xFFDCCBFF : 0xFFB8BBC5);
                cy += 38;
            }

            int contentX = panelX + sideW + 18;
            int contentW = panelW - sideW - 36;
            ctx.drawTextWithShadow(textRenderer, categories.get(category), contentX, panelY + 20, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "Visual modules", contentX, panelY + 35, 0xFF777C89);

            // Search field
            int searchY = panelY + 52;
            ctx.fill(contentX, searchY, contentX + contentW, searchY + 28, 0xFF171A21);
            ctx.drawTextWithShadow(textRenderer,
                search.isEmpty() ? "Search modules..." : search,
                contentX + 10, searchY + 9, search.isEmpty() ? 0xFF666B78 : 0xFFE3E5EA);

            int y = searchY + 38;
            for (Module module : manager.modules) {
                if (!module.getCategory().equals(categories.get(category))) continue;
                if (!search.isEmpty() && !module.getName().toLowerCase().contains(search.toLowerCase())) continue;

                boolean hover = mouseX >= contentX && mouseX < contentX + contentW
                    && mouseY >= y && mouseY < y + 48;

                ctx.fill(contentX, y, contentX + contentW, y + 48,
                    hover ? 0xFF232731 : 0xFF191C23);

                ctx.drawTextWithShadow(textRenderer, module.getName(),
                    contentX + 12, y + 9,
                    module.isEnabled() ? 0xFFECE5FF : 0xFFE0E2E7);

                String desc = module.getDescription();
                if (!desc.isEmpty())
                    ctx.drawTextWithShadow(textRenderer, desc, contentX + 12, y + 25, 0xFF747986);

                int sw = 34, sh = 16;
                int sx = contentX + contentW - sw - 12;
                int sy = y + 16;
                ctx.fill(sx, sy, sx + sw, sy + sh, module.isEnabled() ? 0xFF7952C9 : 0xFF343842);
                ctx.fill(module.isEnabled() ? sx + 20 : sx + 2, sy + 2,
                    module.isEnabled() ? sx + 32 : sx + 14, sy + 14, 0xFFEDEAF5);

                y += 54;
                if (y > panelY + panelH - 25) break;
            }

            ctx.drawTextWithShadow(textRenderer,
                "Right Shift  •  click module  •  ESC closes",
                panelX + 18, panelY + panelH - 16, 0xFF666B78);

            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '-' || chr == '_') {
                search += chr;
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 259 && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (keyCode == 256) {
                client.setScreen(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

            int sideW = 145;
            int cy = panelY + 70;
            for (int i = 0; i < categories.size(); i++) {
                if (mouseX >= panelX + 10 && mouseX < panelX + sideW - 10
                    && mouseY >= cy && mouseY < cy + 34) {
                    category = i;
                    return true;
                }
                cy += 38;
            }

            int contentX = panelX + sideW + 18;
            int contentW = panelW - sideW - 36;
            int y = panelY + 52 + 38;

            for (Module module : manager.modules) {
                if (!module.getCategory().equals(categories.get(category))) continue;
                if (!search.isEmpty() && !module.getName().toLowerCase().contains(search.toLowerCase())) continue;

                if (mouseX >= contentX && mouseX < contentX + contentW
                    && mouseY >= y && mouseY < y + 48) {
                    module.toggle(client);
                    return true;
                }
                y += 54;
                if (y > panelY + panelH - 25) break;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override public boolean shouldPause() { return false; }
    }

    private static class VisualModule extends Module {
        VisualModule(String name, String category, String description) {
            super(name, category, description);
        }
    }

    private static class FullbrightModule extends Module {
        FullbrightModule() { super("Fullbright", "Visuals", "Maximum client brightness"); }

        @Override public void onEnable(MinecraftClient client) {
            if (client.options != null) client.options.getGamma().setValue(16.0);
        }

        @Override public void onDisable(MinecraftClient client) {
            if (client.options != null) client.options.getGamma().setValue(1.0);
        }
    }
}
