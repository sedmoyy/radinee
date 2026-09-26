package ru.sedmoyy.radinee.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import ru.sedmoyy.radinee.RadineeClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final KeyBinding freeLookKey;

    public enum Theme {
        OCEAN("Ocean", 0xFF101F29, 0xFF0E2A38, 0xFF16384B, 0xFF37BDEB, 0xFF9CCBD9),
        SNOW("White", 0xFFF4F4F4, 0xFFFFFFFF, 0xFFE3E3E3, 0xFF202020, 0xFF666666),
        LIGHT_BLACK("Light Black", 0xFF171717, 0xFF202020, 0xFF303030, 0xFFE8E8E8, 0xFFAAAAAA),
        GLASS("Glass", 0xAA18202A, 0x992A3440, 0x884A5A6A, 0xFFE8F4FF, 0xFFB5C9D8),
        RAINY("Rainy", 0xFF16212A, 0xFF1D2D38, 0xFF2A3D4A, 0xFF78A9C2, 0xFFA8BBC5),
        BLACK_WHITE("Black White", 0xFF080808, 0xFF111111, 0xFF292929, 0xFFFFFFFF, 0xFFBDBDBD);

        final String name;
        final int panel, side, card, accent, muted;

        Theme(String name, int panel, int side, int card, int accent, int muted) {
            this.name = name;
            this.panel = panel;
            this.side = side;
            this.card = card;
            this.accent = accent;
            this.muted = muted;
        }

        Theme next() {
            Theme[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    private Theme theme = Theme.OCEAN;

    public Theme getTheme() {
        return theme;
    }

    private void cycleTheme() {
        theme = theme.next();
    }

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

        freeLookKey = RadineeClient.registerModuleKey("Free Look", InputUtil.UNKNOWN_KEY.getCode());
        get("Free Look").setKeyBinding(freeLookKey);
    }

    public List<Module> getModules() {
        return modules;
    }

    public void tick(MinecraftClient client) {
        for (Module module : modules) {
            KeyBinding key = module.getKeyBinding();

            // Free Look is a hold-to-use bind, not a toggle bind.
            if (key != null && !(module instanceof FreeLookModule)) {
                while (key.wasPressed()) {
                    module.toggle(client);
                }
            }

            if (module.isEnabled()) {
                module.onTick(client);
            }
        }
    }

    public void toggleMenu(MinecraftClient client) {
        if (client.currentScreen instanceof VisualsScreen) {
            client.setScreen(null);
        } else if (client.currentScreen == null) {
            client.setScreen(new VisualsScreen(this));
        }
    }

    public void renderHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Module armor = get("Armor HUD");
        if (armor instanceof ArmorHudModule ah && armor.isEnabled()) {
            ah.render(ctx, client);
        }

        Module target = get("Target ESP");
        if (target instanceof TargetEspModule te && target.isEnabled() && te.showTargetHud) {
            te.render(ctx, client);
        }
    }

    public boolean isFreeLookActive() {
        Module module = get("Free Look");
        return module != null
            && module.isEnabled()
            && module.getKeyBinding() != null
            && module.getKeyBinding().isPressed();
    }

    public Module get(String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) return module;
        }
        return null;
    }

    private static class VisualsScreen extends Screen {
        private final ModuleManager manager;
        private final List<String> categories = Arrays.asList("Visuals", "Combat", "HUD", "Player");
        private int category;
        private String search = "";
        private int panelX, panelY, panelW, panelH;

        protected VisualsScreen(ModuleManager manager) {
            super(Text.literal("Radinee Visuals"));
            this.manager = manager;
        }

        @Override
        protected void init() {
            panelW = Math.min(760, width - 30);
            panelH = Math.min(520, height - 30);
            panelX = (width - panelW) / 2;
            panelY = (height - panelH) / 2;
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            renderBackground(ctx, mouseX, mouseY, delta);
            Theme t = manager.theme;
            ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, t.panel);
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, t.accent);

            int sideW = 150;
            ctx.fill(panelX, panelY, panelX + sideW, panelY + panelH, t.side);
            ctx.drawTextWithShadow(textRenderer, "RADINEE", panelX + 18, panelY + 18, t.accent);
            ctx.drawTextWithShadow(textRenderer, "VISUAL CLIENT", panelX + 18, panelY + 33, t.muted);
            ctx.drawTextWithShadow(textRenderer, "THEME: " + t.name + "  [CLICK]", panelX + panelW - 190, panelY + 20, t.muted);

            int cy = panelY + 65;
            for (int i = 0; i < categories.size(); i++) {
                boolean selected = i == category;
                if (selected) {
                    ctx.fill(panelX + 10, cy, panelX + sideW - 10, cy + 34, 0xFF16384B);
                }
                ctx.drawTextWithShadow(textRenderer, categories.get(i), panelX + 22, cy + 11,
                    selected ? t.accent : t.muted);
                cy += 38;
            }

            int contentX = panelX + sideW + 18;
            int contentW = panelW - sideW - 36;
            ctx.drawTextWithShadow(textRenderer, categories.get(category), contentX, panelY + 20, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "Left click: toggle | Right click: settings",
                contentX, panelY + 35, 0xFF87B7C8);

            int searchY = panelY + 52;
            ctx.fill(contentX, searchY, contentX + contentW, searchY + 28, t.card);
            ctx.drawTextWithShadow(textRenderer,
                search.isEmpty() ? "Search modules..." : search,
                contentX + 10, searchY + 9,
                search.isEmpty() ? 0xFF87B7C8 : 0xFFFFFFFF);

            int y = searchY + 38;
            for (Module module : manager.modules) {
                if (!module.getCategory().equals(categories.get(category))) continue;
                if (!search.isEmpty() && !module.getName().toLowerCase().contains(search.toLowerCase())) continue;
                if (y + 48 > panelY + panelH - 12) break;

                boolean hover = mouseX >= contentX && mouseX < contentX + contentW
                    && mouseY >= y && mouseY < y + 48;
                ctx.fill(contentX, y, contentX + contentW, y + 48,
                    hover ? t.side : t.card);

                ctx.drawTextWithShadow(textRenderer, module.getName(), contentX + 12, y + 8,
                    module.isEnabled() ? 0xFF37BDEB : 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, module.getDescription(), contentX + 12, y + 25, 0xFF87B7C8);

                String bind = module.getKeyBinding() == null
                    ? "RMB settings"
                    : "Bind: " + module.getKeyBinding().getBoundKeyLocalizedText().getString();
                ctx.drawTextWithShadow(textRenderer, bind, contentX + contentW - 145, y + 8, 0xFF87B7C8);
                y += 54;
            }

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
        public boolean keyPressed(KeyInput input) {
            int keyCode = input.key();
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                client.setScreen(null);
                return true;
            }
            return super.keyPressed(input);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.button();
            int sideW = 150;
            int cy = panelY + 65;
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
            int y = panelY + 90;

            for (Module module : manager.modules) {
                if (!module.getCategory().equals(categories.get(category))) continue;
                if (!search.isEmpty() && !module.getName().toLowerCase().contains(search.toLowerCase())) continue;

                if (mouseX >= contentX && mouseX < contentX + contentW
                    && mouseY >= y && mouseY < y + 48) {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        if (mouseY >= panelY + 10 && mouseY <= panelY + 34
                            && mouseX >= panelX + panelW - 205) {
                            manager.cycleTheme();
                            return true;
                        }
                        module.toggle(client);
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        client.setScreen(new SettingsScreen(this, module));
                    }
                    return true;
                }
                y += 54;
            }
            return super.mouseClicked(click, doubled);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    private static class SettingsScreen extends Screen {
        private final Screen parent;
        private final Module module;

        SettingsScreen(Screen parent, Module module) {
            super(Text.literal(module.getName() + " Settings"));
            this.parent = parent;
            this.module = module;
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            renderBackground(ctx, mouseX, mouseY, delta);
            int w = Math.min(560, width - 40);
            int h = 300;
            int x = (width - w) / 2;
            int y = (height - h) / 2;

            Theme t = ((VisualsScreen) parent).manager.theme;
            ctx.fill(x, y, x + w, y + h, t.panel);
            ctx.drawTextWithShadow(textRenderer, module.getName(), x + 20, y + 20, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, module.getDescription(), x + 20, y + 40, t.muted);

            int line = y + 72;
            if (module instanceof FullbrightModule fb) {
                drawSlider(ctx, x + 20, line, w - 40, "Brightness", fb.brightness);
            } else if (module instanceof TargetEspModule te) {
                ctx.drawTextWithShadow(textRenderer, "Target HUD: " + (te.showTargetHud ? "ON" : "OFF"),
                    x + 20, line, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, "Shows target nickname and health bar",
                    x + 20, line + 20, 0xFF9DB5C4);
            } else if (module instanceof ArmorHudModule ah) {
                ctx.drawTextWithShadow(textRenderer, "Display: " + ah.mode,
                    x + 20, line, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, "Click to cycle: NUMBERS / BAR / PERCENT",
                    x + 20, line + 20, 0xFF9DB5C4);
            } else if (module instanceof NoHurtCamModule nh) {
                drawSlider(ctx, x + 20, line, w - 40, "Shake reduction", nh.reduction);
            } else if (module instanceof FreeLookModule fl) {
                String state = fl.rebinding ? "PRESS A KEY OR CLICK A MOUSE BUTTON" : fl.keyName();
                ctx.drawTextWithShadow(textRenderer, "Hold key: " + state, x + 20, line, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, "Right click this line to rebind",
                    x + 20, line + 20, 0xFF9DB5C4);
            }

            String bind = module.getKeyBinding() == null
                ? "NONE"
                : module.getKeyBinding().getBoundKeyLocalizedText().getString();
            ctx.drawTextWithShadow(textRenderer, "Keybind: " + bind,
                x + 20, y + h - 50, 0xFFD9F7FF);
            ctx.drawTextWithShadow(textRenderer, "ESC = back", x + w - 90, y + h - 25, 0xFF87B7C8);

            super.render(ctx, mouseX, mouseY, delta);
        }

        private void drawSlider(DrawContext ctx, int x, int y, int w, String label, int value) {
            ctx.drawTextWithShadow(textRenderer, label + ": " + value + "%", x, y, 0xFFFFFFFF);
            ctx.fill(x, y + 20, x + w, y + 26, t.card);
            int knob = x + (w * value / 100);
            ctx.fill(knob - 3, y + 15, knob + 3, y + 31, 0xFF37BDEB);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            double mx = click.x();
            double my = click.y();
            int button = click.button();
            int w = Math.min(560, width - 40);
            int h = 300;
            int x = (width - w) / 2;
            int y = (height - h) / 2;
            int line = y + 72;

            if (module instanceof FreeLookModule fl && fl.rebinding) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                    || button >= GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    fl.setBinding(InputUtil.Type.MOUSE.createFromCode(button));
                    return true;
                }
            }

            if (module instanceof FullbrightModule fb && my >= line + 12 && my <= line + 40) {
                fb.brightness = clamp((int) (((mx - (x + 20)) / (double) (w - 40)) * 100));
                return true;
            }

            if (module instanceof NoHurtCamModule nh && my >= line + 12 && my <= line + 40) {
                nh.reduction = clamp((int) (((mx - (x + 20)) / (double) (w - 40)) * 100));
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

            if (module instanceof FreeLookModule fl
                && my >= line && my <= line + 42
                && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                fl.startRebind();
                return true;
            }

            return super.mouseClicked(click, doubled);
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            int keyCode = input.key();
            if (module instanceof FreeLookModule fl && fl.rebinding) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    fl.cancelRebind();
                } else {
                    fl.setBinding(InputUtil.Type.KEYSYM.createFromCode(keyCode));
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                client.setScreen(parent);
                return true;
            }
            return super.keyPressed(input);
        }

        private int clamp(int n) {
            return Math.max(0, Math.min(100, n));
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    private static class VisualModule extends Module {
        VisualModule(String name, String category, String description) {
            super(name, category, description);
        }
    }

    private static class FullbrightModule extends Module {
        int brightness = 100;

        FullbrightModule() {
            super("Fullbright", "Visuals", "Adjustable brightness from 0% to 100%");
        }

        @Override
        public void onEnable(MinecraftClient client) {
            apply(client);
        }

        @Override
        public void onDisable(MinecraftClient client) {
            if (client.options != null) {
                client.options.getGamma().setValue(1.0);
            }
        }

        @Override
        public void onTick(MinecraftClient client) {
            apply(client);
        }

        private void apply(MinecraftClient client) {
            if (client.options != null) {
                client.options.getGamma().setValue(brightness / 100.0 * 16.0);
            }
        }
    }

    private static class TargetEspModule extends Module {
        boolean showTargetHud = true;

        TargetEspModule() {
            super("Target ESP", "Combat", "Target HUD with nickname and health bar");
        }

        void render(DrawContext ctx, MinecraftClient client) {
            if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;

            Entity entity = hit.getEntity();
            if (!(entity instanceof LivingEntity living)) return;
            if (entity == client.player) return;

            int x = 8;
            int y = 8;
            float health = Math.max(0.0f, living.getHealth());
            float maxHealth = Math.max(1.0f, living.getMaxHealth());
            int barWidth = 120;
            int filled = Math.round(barWidth * Math.min(1.0f, health / maxHealth));

            ctx.fill(x, y, x + 150, y + 42, 0xCC101820);
            ctx.drawTextWithShadow(client.textRenderer, living.getDisplayName(),
                x + 8, y + 6, 0xFFFFFFFF);
            ctx.drawTextWithShadow(client.textRenderer,
                String.format("%.1f HP", health), x + 8, y + 20, 0xFF9DB5C4);
            ctx.fill(x + 8, y + 34, x + 8 + barWidth, y + 38, 0xFF3A3A3A);
            ctx.fill(x + 8, y + 34, x + 8 + filled, y + 38, 0xFF55FF55);
        }
    }

    private static class ArmorHudModule extends Module {
        enum Mode {
            NUMBERS, BAR, PERCENT;

            Mode next() {
                return values()[(ordinal() + 1) % values().length];
            }
        }

        Mode mode = Mode.NUMBERS;

        ArmorHudModule() {
            super("Armor HUD", "HUD", "Armor durability with color-coded values");
        }

        void render(DrawContext ctx, MinecraftClient client) {
            int x = 8;
            int y = client.getWindow().getScaledHeight() - 42;

            for (int i = 0; i < 4; i++) {
                ItemStack stack = client.player.getInventory().getArmorStack(i);
                int slotX = x + i * 38;

                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, slotX, y);
                }

                if (!stack.isEmpty() && stack.isDamageable()) {
                    int max = stack.getMaxDamage();
                    int remaining = max - stack.getDamage();
                    float percent = max <= 0 ? 0.0f : remaining * 100.0f / max;
                    int color = colorFor(stack);

                    if (mode == Mode.NUMBERS) {
                        ctx.drawTextWithShadow(client.textRenderer, String.valueOf(remaining),
                            slotX, y + 18, color);
                    } else if (mode == Mode.PERCENT) {
                        ctx.drawTextWithShadow(client.textRenderer,
                            String.format("%.0f%%", percent),
                            slotX, y + 18, color);
                    } else {
                        int width = Math.max(0, Math.min(28, Math.round(28.0f * percent / 100.0f)));
                        ctx.fill(slotX, y + 18, slotX + 28, y + 22, 0xFF333333);
                        ctx.fill(slotX, y + 18, slotX + width, y + 22, color);
                    }
                }
            }
        }

        static int colorFor(ItemStack stack) {
            if (stack.isEmpty() || !stack.isDamageable()) return 0xFFFFFFFF;

            float p = (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
            if (p > 0.60f) return 0xFF55FF55;
            if (p > 0.30f) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
    }

    private static class NoHurtCamModule extends Module {
        int reduction = 100;

        NoHurtCamModule() {
            super("No HurtCam", "Player", "Reduces camera shake by a configurable percentage");
        }
    }

    private static class FreeLookModule extends Module {
        boolean rebinding;

        FreeLookModule() {
            super("Free Look", "Player", "Hold a key to rotate the camera independently of the player");
        }

        String keyName() {
            return getKeyBinding() == null
                ? "NONE"
                : getKeyBinding().getBoundKeyLocalizedText().getString();
        }

        void startRebind() {
            rebinding = true;
        }

        void cancelRebind() {
            rebinding = false;
        }

        void setBinding(InputUtil.Key key) {
            if (getKeyBinding() != null) {
                getKeyBinding().setBoundKey(key);
            }
            rebinding = false;
        }
    }
}
