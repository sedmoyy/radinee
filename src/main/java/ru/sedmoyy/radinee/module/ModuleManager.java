package ru.sedmoyy.radinee.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.sedmoyy.radinee.RadineeClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final KeyBinding freeLookKey;

    public ModuleManager() {
        modules.add(new VisualModule("NameTags", "Visuals", "Enhanced entity name tags"));
        modules.add(new TargetEspModule());
        modules.add(new ArmorHudModule());
        modules.add(new VisualModule("China Hat", "Visuals", "Cosmetic player hat"));
        modules.add(new VisualModule("Hit Color", "Visuals", "Custom damage tint"));
        modules.add(new VisualModule("Block Outline", "Visuals", "Custom block selection outline"));
        modules.add(new FullbrightModule());
        modules.add(new VisualModule("Player Model", "Player", "Client-side player model options"));
        modules.add(new NoHurtCamModule());
        modules.add(new VisualModule("Crosshair", "HUD", "Custom crosshair"));
        modules.add(new VisualModule("FPS Counter", "HUD", "FPS indicator"));
        modules.add(new VisualModule("CPS Counter", "HUD", "Clicks per second"));
        modules.add(new VisualModule("Keystrokes", "HUD", "Movement and mouse keys"));
        modules.add(new FreeLookModule());

        freeLookKey = RadineeClient.registerModuleKey("Free Look", GLFW.GLFW_KEY_V);
        modules.get(modules.size() - 1).setKeyBinding(freeLookKey);
    }

    public List<Module> getModules() { return modules; }

    public void tick(MinecraftClient client) {
        for (Module module : modules) {
            KeyBinding key = module.getKeyBinding();
            if (key != null) while (key.wasPressed()) module.toggle(client);
            if (module.isEnabled()) module.onTick(client);
        }
    }

    public void toggleMenu(MinecraftClient client) {
        if (client.currentScreen instanceof VisualsScreen) client.setScreen(null);
        else if (client.currentScreen == null) client.setScreen(new VisualsScreen(this));
    }

    public boolean isFreeLookActive() {\n        Module m = get("Free Look");\n        return m != null && m.getKeyBinding() != null && m.getKeyBinding().isPressed();\n    }\n\n    public Module get(String name) {
        for (Module m : modules) if (m.getName().equals(name)) return m;
        return null;
    }

    private static class VisualsScreen extends Screen {
        private final ModuleManager manager;
        private final List<String> categories = Arrays.asList("Visuals", "Combat", "HUD", "Player");
        private int category;
        private String search = "";
        private int panelX, panelY, panelW, panelH;
        private Theme theme = Theme.OCEAN;

        protected VisualsScreen(ModuleManager manager) {
            super(Text.literal("Radinee Visuals"));
            this.manager = manager;
        }

        @Override protected void init() {
            panelW = Math.min(760, width - 30);
            panelH = Math.min(520, height - 30);
            panelX = (width - panelW) / 2;
            panelY = (height - panelH) / 2;
        }

        @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            renderBackground(ctx, mouseX, mouseY, delta);
            ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, theme.background());
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, theme.accent());

            int sideW = 150;
            ctx.fill(panelX, panelY, panelX + sideW, panelY + panelH, theme.sidebar());
            ctx.drawTextWithShadow(textRenderer, "RADINEE", panelX + 18, panelY + 18, theme.primaryText());
            ctx.drawTextWithShadow(textRenderer, "VISUAL CLIENT", panelX + 18, panelY + 33, theme.secondaryText());

            int cy = panelY + 65;
            for (int i = 0; i < categories.size(); i++) {
                boolean selected = i == category;
                if (selected) ctx.fill(panelX + 10, cy, panelX + sideW - 10, cy + 34, theme.selected());
                ctx.drawTextWithShadow(textRenderer, categories.get(i), panelX + 22, cy + 11,
                    selected ? theme.accentText() : theme.secondaryText());
                cy += 38;
            }

            int themeY = panelY + panelH - 45;
            ctx.drawTextWithShadow(textRenderer, "Theme: " + theme.displayName, panelX + 18, themeY, theme.accentText());

            int contentX = panelX + sideW + 18;
            int contentW = panelW - sideW - 36;
            ctx.drawTextWithShadow(textRenderer, categories.get(category), contentX, panelY + 20, theme.primaryText());
            ctx.drawTextWithShadow(textRenderer, "Left click: toggle  |  Right click: settings", contentX, panelY + 35, theme.secondaryText());

            int searchY = panelY + 52;
            ctx.fill(contentX, searchY, contentX + contentW, searchY + 28, theme.input());
            ctx.drawTextWithShadow(textRenderer, search.isEmpty() ? "Search modules..." : search,
                contentX + 10, searchY + 9, search.isEmpty() ? theme.secondaryText() : theme.primaryText());

            int y = searchY + 38;
            for (Module module : manager.modules) {
                if (!module.getCategory().equals(categories.get(category))) continue;
                if (!search.isEmpty() && !module.getName().toLowerCase().contains(search.toLowerCase())) continue;
                if (y + 48 > panelY + panelH - 12) break;

                boolean hover = mouseX >= contentX && mouseX < contentX + contentW && mouseY >= y && mouseY < y + 48;
                ctx.fill(contentX, y, contentX + contentW, y + 48, hover ? theme.hover() : theme.card());
                ctx.drawTextWithShadow(textRenderer, module.getName(), contentX + 12, y + 8,
                    module.isEnabled() ? theme.accentText() : theme.primaryText());
                ctx.drawTextWithShadow(textRenderer, module.getDescription(), contentX + 12, y + 25, theme.secondaryText());

                String bind = module.getKeyBinding() == null ? "RMB settings" : "Bind: " + module.getKeyBinding().getBoundKeyLocalizedText().getString();
                ctx.drawTextWithShadow(textRenderer, bind, contentX + contentW - 145, y + 8, theme.secondaryText());
                y += 54;
            }
            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override public boolean charTyped(char chr, int modifiers) {
            if (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '-' || chr == '_') {
                search += chr;
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                client.setScreen(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int sideW = 150;
            int cy = panelY + 65;
            for (int i = 0; i < categories.size(); i++) {
                if (mouseX >= panelX + 10 && mouseX < panelX + sideW - 10 && mouseY >= cy && mouseY < cy + 34) {
                    category = i; search = ""; return true;
                }
                cy += 38;
            }

            int contentX = panelX + sideW + 18;
            int contentW = panelW - sideW - 36;
            int y = panelY + 90;
            for (Module module : manager.modules) {
                if (!module.getCategory().equals(categories.get(category))) continue;
                if (!search.isEmpty() && !module.getName().toLowerCase().contains(search.toLowerCase())) continue;
                if (mouseX >= contentX && mouseX < contentX + contentW && mouseY >= y && mouseY < y + 48) {
                    if (button == 0) module.toggle(client);
                    else if (button == 1) client.setScreen(new SettingsScreen(this, module));
                    return true;
                }
                y += 54;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override public boolean shouldPause() { return false; }
    }

    private static class SettingsScreen extends Screen {
        private final Screen parent;
        private final Module module;

        SettingsScreen(Screen parent, Module module) {
            super(Text.literal(module.getName() + " Settings"));
            this.parent = parent;
            this.module = module;
        }

        @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            renderBackground(ctx, mouseX, mouseY, delta);
            int w = Math.min(560, width - 40), h = 300;
            int x = (width - w) / 2, y = (height - h) / 2;
            ctx.fill(x, y, x + w, y + h, 0xEE101820);
            ctx.drawTextWithShadow(textRenderer, module.getName(), x + 20, y + 20, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, module.getDescription(), x + 20, y + 40, 0xFF9DB5C4);

            int line = y + 72;
            if (module instanceof FullbrightModule fb) {
                drawSlider(ctx, x + 20, line, w - 40, "Brightness", fb.brightness);
                line += 52;
            } else if (module instanceof TargetEspModule te) {
                ctx.drawTextWithShadow(textRenderer, "Target HUD: " + (te.showTargetHud ? "ON" : "OFF"), x + 20, line, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, "Shows target nickname and health bar", x + 20, line + 20, 0xFF9DB5C4);
                line += 52;
            } else if (module instanceof ArmorHudModule ah) {
                ctx.drawTextWithShadow(textRenderer, "Display: " + ah.mode, x + 20, line, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, "Click to cycle: NUMBERS / BAR / PERCENT", x + 20, line + 20, 0xFF9DB5C4);
                line += 52;
            } else if (module instanceof NoHurtCamModule nh) {
                drawSlider(ctx, x + 20, line, w - 40, "Shake reduction", nh.reduction);
                line += 52;
            } else if (module instanceof FreeLookModule fl) {
                ctx.drawTextWithShadow(textRenderer, "Hold key: " + fl.keyName(), x + 20, line, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, "Right click this line to rebind", x + 20, line + 20, 0xFF9DB5C4);
                line += 52;
            }

            ctx.drawTextWithShadow(textRenderer, "Keybind: " + (module.getKeyBinding() == null ? "NONE" : module.getKeyBinding().getBoundKeyLocalizedText().getString()),
                x + 20, y + h - 50, 0xFFD9F7FF);
            ctx.drawTextWithShadow(textRenderer, "ESC = back", x + w - 90, y + h - 25, 0xFF87B7C8);
            super.render(ctx, mouseX, mouseY, delta);
        }

        private void drawSlider(DrawContext ctx, int x, int y, int w, String label, int value) {
            ctx.drawTextWithShadow(textRenderer, label + ": " + value + "%", x, y, 0xFFFFFFFF);
            ctx.fill(x, y + 20, x + w, y + 26, 0xFF31434F);
            int knob = x + (w * value / 100);
            ctx.fill(knob - 3, y + 15, knob + 3, y + 31, 0xFF37BDEB);
        }

        @Override public boolean mouseClicked(double mx, double my, int button) {
            int w = Math.min(560, width - 40), h = 300, x = (width - w) / 2, y = (height - h) / 2;
            int line = y + 72;
            if (module instanceof FullbrightModule fb && my >= line + 12 && my <= line + 40) {
                fb.brightness = clamp((int)(((mx - (x + 20)) / (double)(w - 40)) * 100));
                return true;
            }
            if (module instanceof NoHurtCamModule nh && my >= line + 12 && my <= line + 40) {
                nh.reduction = clamp((int)(((mx - (x + 20)) / (double)(w - 40)) * 100));
                return true;
            }
            if (module instanceof ArmorHudModule ah && my >= line && my <= line + 42) {
                ah.mode = ah.mode.next();
                return true;
            }
            if (module instanceof TargetEspModule te && my >= line && my <= line + 42) {
                te.showTargetHud = !te.showTargetHud;
                return true;
            }
            if (module instanceof FreeLookModule fl && my >= line && my <= line + 42 && button != 0) {
                fl.startRebind();
                return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (module instanceof FreeLookModule fl && fl.rebinding) {
                fl.finishRebind(keyCode, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { client.setScreen(parent); return true; }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private int clamp(int n) { return Math.max(0, Math.min(100, n)); }
        @Override public boolean shouldPause() { return false; }
    }

    private static class VisualModule extends Module {
        VisualModule(String name, String category, String description) { super(name, category, description); }
    }

    private static class FullbrightModule extends Module {
        int brightness = 100;
        FullbrightModule() { super("Fullbright", "Visuals", "Adjustable brightness from 0% to 100%"); }
        @Override public void onEnable(MinecraftClient client) { apply(client); }
        @Override public void onDisable(MinecraftClient client) { if (client.options != null) client.options.getGamma().setValue(1.0); }
        @Override public void onTick(MinecraftClient client) { apply(client); }
        private void apply(MinecraftClient client) { if (client.options != null) client.options.getGamma().setValue(brightness / 100.0 * 16.0); }
    }

    private static class TargetEspModule extends Module {
        boolean showTargetHud = true;
        TargetEspModule() { super("Target ESP", "Combat", "Target HUD with nickname and health bar"); }
        @Override public void onTick(MinecraftClient client) {
            // Rendering is intentionally kept client-side; target data is read from the normal crosshair target.
        }
    }

    private static class ArmorHudModule extends Module {
        enum Mode { NUMBERS, BAR, PERCENT; Mode next() { return values()[(ordinal()+1)%values().length]; } }
        Mode mode = Mode.NUMBERS;
        ArmorHudModule() { super("Armor HUD", "HUD", "Armor durability with color-coded values"); }
        @Override public void onTick(MinecraftClient client) {}
        static int colorFor(ItemStack stack) {
            if (stack.isEmpty() || !stack.isDamageable()) return 0xFFFFFFFF;
            float p = (stack.getMaxDamage() - stack.getDamage()) / (float)stack.getMaxDamage();
            if (p > 0.60f) return 0xFF55FF55;
            if (p > 0.30f) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
    }

    private static class NoHurtCamModule extends Module {
        int reduction = 100;
        NoHurtCamModule() { super("No HurtCam", "Player", "Reduces camera shake by a configurable percentage"); }
    }

    private static class FreeLookModule extends Module {
        boolean rebinding;
        FreeLookModule() { super("Free Look", "Player", "Hold a key to rotate the camera independently of the player"); }
        String keyName() { return getKeyBinding() == null ? "NONE" : getKeyBinding().getBoundKeyLocalizedText().getString(); }
        void startRebind() { rebinding = true; }
        void finishRebind(int keyCode, int mouseButton) { rebinding = false; }
    }

    private enum Theme {
        OCEAN("Ocean", 0xFF08141F, 0xFF0E2433, 0xFF102B3C, 0xFF16384B, 0xFF0C202F, 0xFF37BDEB, 0xFFD9F7FF, 0xFF87B7C8, 0xFF1B4559, 0xFF0A1118),
        SNOW("Snow", 0xFFEAF0F5, 0xFFDCE5EC, 0xFFF4F7FA, 0xFFFFFFFF, 0xFFE1EAF0, 0xFF4F86B5, 0xFF14202A, 0xFF61727E, 0xFFD5E1E8, 0xFFC5D0D8),
        SPRING("Spring", 0xFF111D16, 0xFF17271D, 0xFF1D3022, 0xFF243A28, 0xFF243A28, 0xFF69D36E, 0xFFE4FFE5, 0xFF9BC49D, 0xFF355A39, 0xFF101A13),
        SUMMER("Summer", 0xFF171A0B, 0xFF24280D, 0xFF303512, 0xFF3A4016, 0xFF292F0F, 0xFFE6C83D, 0xFFFFF7C7, 0xFFC7BE72, 0xFF5A541D, 0xFF181A0A),
        RAGE("Rage", 0xFF1A080B, 0xFF280C10, 0xFF351016, 0xFF43131B, 0xFF300D13, 0xFFFF4057, 0xFFFFE4E7, 0xFFC98A92, 0xFF641D28, 0xFF1C080B);
        final String displayName; final int background, sidebar, card, selected, hover, accent, primaryText, secondaryText, input, switchOff;
        Theme(String n,int a,int b,int c,int d,int e,int f,int g,int h,int i,int j){displayName=n;background=a;sidebar=b;card=c;selected=d;hover=e;accent=f;primaryText=g;secondaryText=h;input=i;switchOff=j;}
        int background(){return background|0xFF000000;} int sidebar(){return sidebar|0xFF000000;} int card(){return card|0xFF000000;} int selected(){return selected|0xFF000000;} int hover(){return hover|0xFF000000;} int accent(){return accent|0xFF000000;} int accentText(){return primaryText();} int primaryText(){return primaryText|0xFF000000;} int secondaryText(){return secondaryText|0xFF000000;} int input(){return input|0xFF000000;}
    }
}