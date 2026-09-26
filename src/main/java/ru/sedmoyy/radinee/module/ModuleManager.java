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
        modules.add(new VisualModule("NameTags", "Visuals", "Enhanced entity name tags"));
        modules.add(new VisualModule("Target ESP", "Combat", "Target highlight effect"));
        modules.add(new VisualModule("Armor HUD", "HUD", "Shows your armor and durability"));
        modules.add(new VisualModule("China Hat", "Visuals", "Cosmetic player hat"));
        modules.add(new VisualModule("Hit Color", "Visuals", "Custom damage tint"));
        modules.add(new VisualModule("Block Outline", "Visuals", "Custom block selection outline"));
        modules.add(new FullbrightModule());
        modules.add(new VisualModule("Player Model", "Player", "Client-side player model options"));
        modules.add(new VisualModule("No HurtCam", "Player", "Removes camera shake"));
        modules.add(new VisualModule("Crosshair", "HUD", "Custom crosshair"));
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
        private final List<String> categories = Arrays.asList("Visuals", "Combat", "HUD", "Player");
        private int category = 0;
        private String search = "";
        private int panelX, panelY, panelW, panelH;
        private boolean dragging;
        private Theme theme = Theme.OCEAN;

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

            ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, theme.background());
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, theme.accent());

            // Sidebar
            int sideW = 145;
            ctx.fill(panelX, panelY, panelX + sideW, panelY + panelH, theme.sidebar());
            ctx.drawTextWithShadow(textRenderer, "RADINEE", panelX + 18, panelY + 18, theme.primaryText());
            ctx.drawTextWithShadow(textRenderer, "VISUAL CLIENT", panelX + 18, panelY + 33, theme.secondaryText());

            int cy = panelY + 70;
            for (int i = 0; i < categories.size(); i++) {
                boolean selected = i == category;
                boolean hover = mouseX >= panelX + 10 && mouseX < panelX + sideW - 10
                    && mouseY >= cy && mouseY < cy + 34;
                if (selected) ctx.fill(panelX + 10, cy, panelX + sideW - 10, cy + 34, theme.selected());
                else if (hover) ctx.fill(panelX + 10, cy, panelX + sideW - 10, cy + 34, theme.hover());
                ctx.drawTextWithShadow(textRenderer, categories.get(i), panelX + 22, cy + 11,
                    selected ? theme.accentText() : theme.secondaryText());
                cy += 38;
            }

            int themeY = panelY + panelH - 58;
            boolean themeHover = mouseX >= panelX + 10 && mouseX < panelX + sideW - 10
                && mouseY >= themeY && mouseY < themeY + 34;
            if (themeHover) ctx.fill(panelX + 10, themeY, panelX + sideW - 10, themeY + 34, theme.hover());
            ctx.drawTextWithShadow(textRenderer, "Theme: " + theme.displayName,
                panelX + 18, themeY + 11, theme.accentText());

            int themeY = panelY + panelH - 58;
            if (mouseX >= panelX + 10 && mouseX < panelX + sideW - 10
                && mouseY >= themeY && mouseY < themeY + 34) {
                Theme[] themes = Theme.values();
                theme = themes[(theme.ordinal() + 1) % themes.length];
                return true;
            }

            int contentX = panelX + sideW + 18;
            int contentW = panelW - sideW - 36;
            ctx.drawTextWithShadow(textRenderer, categories.get(category), contentX, panelY + 20, theme.primaryText());
            ctx.drawTextWithShadow(textRenderer, "Modules and settings", contentX, panelY + 35, theme.secondaryText());

            if (false) {
                renderThemes(ctx, mouseX, mouseY, contentX, contentW);
                ctx.drawTextWithShadow(textRenderer, "Theme changes apply instantly", contentX, panelY + panelH - 34, theme.secondaryText());
                ctx.drawTextWithShadow(textRenderer, "Right Shift  •  ESC closes", panelX + 18, panelY + panelH - 16, theme.secondaryText());
                super.render(ctx, mouseX, mouseY, delta);
                return;
            }

            // Search field
            int searchY = panelY + 52;
            ctx.fill(contentX, searchY, contentX + contentW, searchY + 28, theme.input());
            ctx.drawTextWithShadow(textRenderer,
                search.isEmpty() ? "Search modules..." : search,
                contentX + 10, searchY + 9, search.isEmpty() ? theme.secondaryText() : theme.primaryText());

            int y = searchY + 38;
            for (Module module : manager.modules) {
                if (!module.getCategory().equals(categories.get(category))) continue;
                if (!search.isEmpty() && !module.getName().toLowerCase().contains(search.toLowerCase())) continue;

                boolean hover = mouseX >= contentX && mouseX < contentX + contentW
                    && mouseY >= y && mouseY < y + 48;

                ctx.fill(contentX, y, contentX + contentW, y + 48,
                    hover ? theme.hover() : theme.card());

                ctx.drawTextWithShadow(textRenderer, module.getName(),
                    contentX + 12, y + 9,
                    module.isEnabled() ? theme.accentText() : theme.primaryText());

                String desc = module.getDescription();
                if (!desc.isEmpty())
                    ctx.drawTextWithShadow(textRenderer, desc, contentX + 12, y + 25, theme.secondaryText());

                int sw = 34, sh = 16;
                int sx = contentX + contentW - sw - 12;
                int sy = y + 16;
                ctx.fill(sx, sy, sx + sw, sy + sh, module.isEnabled() ? theme.accent() : theme.switchOff());
                ctx.fill(module.isEnabled() ? sx + 20 : sx + 2, sy + 2,
                    module.isEnabled() ? sx + 32 : sx + 14, sy + 14, theme.primaryText());

                y += 54;
                if (y > panelY + panelH - 25) break;
            }

            ctx.drawTextWithShadow(textRenderer,
                "Right Shift  •  click module  •  ESC closes",
                panelX + 18, panelY + panelH - 16, theme.secondaryText());

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
                    search = "";
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

        private void renderThemes(DrawContext ctx, int mouseX, int mouseY, int contentX, int contentW) {
            int startY = panelY + 62;
            int cardW = Math.max(150, (contentW - 16) / 2);
            Theme[] themes = Theme.values();
            for (int i = 0; i < themes.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int x = contentX + col * (cardW + 8);
                int y = startY + row * 82;
                boolean selected = theme == themes[i];
                boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + 68;
                ctx.fill(x, y, x + cardW, y + 68, selected ? themes[i].selected() : (hover ? themes[i].hover() : themes[i].card()));
                ctx.fill(x, y, x + 7, y + 68, themes[i].accent());
                ctx.drawTextWithShadow(textRenderer, themes[i].displayName, x + 18, y + 15, themes[i].primaryText());
                ctx.drawTextWithShadow(textRenderer, selected ? "Selected" : "Click to apply", x + 18, y + 35, themes[i].secondaryText());
                ctx.fill(x + cardW - 54, y + 16, x + cardW - 44, y + 26, themes[i].accent());
                ctx.fill(x + cardW - 40, y + 16, x + cardW - 30, y + 26, themes[i].primaryText());
                ctx.fill(x + cardW - 26, y + 16, x + cardW - 16, y + 26, themes[i].secondaryText());
            }
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

    private enum Theme {
        OCEAN("Ocean", 0xFF08141F, 0xFF0E2433, 0xFF102B3C, 0xFF16384B, 0xFF0C202F, 0xFF37BDEB, 0xFFD9F7FF, 0xFF87B7C8, 0xFF1B4559, 0xFF0A1118),
        SNOW("Snow", 0xFFEAF0F5, 0xFFDCE5EC, 0xFFF4F7FA, 0xFFFFFFFF, 0xFFE1EAF0, 0xFF4F86B5, 0xFF14202A, 0xFF61727E, 0xFFD5E1E8, 0xFFC5D0D8),
        SPRING("Spring", 0xFF111D16, 0xFF17271D, 0xFF1D3022, 0xFF243A28, 0xFF1A2B20, 0xFF69D36E, 0xFFE4FFE5, 0xFF9BC49D, 0xFF355A39, 0xFF101A13),
        SUMMER("Summer", 0xFF171A0B, 0xFF24280D, 0xFF303512, 0xFF3A4016, 0xFF292F0F, 0xFFE6C83D, 0xFFFFF7C7, 0xFFC7BE72, 0xFF5A541D, 0xFF181A0A),
        RAGE("Rage", 0xFF1A080B, 0xFF280C10, 0xFF351016, 0xFF43131B, 0xFF300D13, 0xFFFF4057, 0xFFFFE4E7, 0xFFC98A92, 0xFF641D28, 0xFF1C080B);

        private final String displayName;
        private final int background, sidebar, card, selected, hover, accent, primaryText, secondaryText, input, switchOff;
        Theme(String displayName, int background, int sidebar, int card, int selected, int hover, int accent, int primaryText, int secondaryText, int input, int switchOff) {
            this.displayName = displayName; this.background = background; this.sidebar = sidebar; this.card = card; this.selected = selected; this.hover = hover; this.accent = accent; this.primaryText = primaryText; this.secondaryText = secondaryText; this.input = input; this.switchOff = switchOff;
        }
        int background() { return background | 0xFF000000; }
        int sidebar() { return sidebar | 0xFF000000; }
        int card() { return card | 0xFF000000; }
        int selected() { return selected | 0xFF000000; }
        int hover() { return hover | 0xFF000000; }
        int accent() { return accent | 0xFF000000; }
        int accentText() { return primaryText | 0xFF000000; }
        int primaryText() { return primaryText | 0xFF000000; }
        int secondaryText() { return secondaryText | 0xFF000000; }
        int input() { return input | 0xFF000000; }
        int switchOff() { return switchOff | 0xFF000000; }
    }

}
