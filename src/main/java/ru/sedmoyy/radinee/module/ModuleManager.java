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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import ru.sedmoyy.radinee.RadineeClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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
    private static int hudOpacity = 100;
    private int targetHudX = -1, targetHudY = -1;
    private int armorHudX = 8, armorHudY = -1;
    private final Map<String, int[]> hudPositions = new HashMap<>();

    private void initHudPositions() {
        hudPositions.put("Crosshair", new int[]{-1, -1});
        hudPositions.put("FPS Counter", new int[]{8, 8});
        hudPositions.put("CPS Counter", new int[]{8, 22});
        hudPositions.put("Keystrokes", new int[]{8, 42});
    }

    public int[] getHudPosition(String name) {
        return hudPositions.computeIfAbsent(name, k -> new int[]{8, 8});
    }

    public void setHudPosition(String name, int x, int y, int screenWidth, int screenHeight) {
        int w = 100;
        int h = name.equals("Keystrokes") ? 64 : 18;
        if (name.equals("Crosshair")) { w = 16; h = 16; }
        int[] pos = getHudPosition(name);
        pos[0] = Math.max(0, Math.min(x, Math.max(0, screenWidth - w)));
        pos[1] = Math.max(0, Math.min(y, Math.max(0, screenHeight - h)));
    }

    public int getTargetHudX() { return targetHudX; }
    public int getTargetHudY() { return targetHudY; }
    public int getArmorHudX() { return armorHudX; }
    public int getArmorHudY() { return armorHudY; }

    public void setTargetHudPosition(int x, int y, int screenWidth, int screenHeight) {
        targetHudX = Math.max(0, Math.min(x, Math.max(0, screenWidth - 150)));
        targetHudY = Math.max(0, Math.min(y, Math.max(0, screenHeight - 42)));
    }

    public void setArmorHudPosition(int x, int y, int screenWidth, int screenHeight) {
        armorHudX = Math.max(0, Math.min(x, Math.max(0, screenWidth - 152)));
        armorHudY = Math.max(0, Math.min(y, Math.max(0, screenHeight - 42)));
    }

    public int getHudOpacity() { return hudOpacity; }
    public void setHudOpacity(int value) { hudOpacity = Math.max(0, Math.min(100, value)); }
    private static int hudColor(int rgb) { return ((hudOpacity * 255 / 100) << 24) | (rgb & 0x00FFFFFF); }

    public Theme getTheme() {
        return theme;
    }

    private void cycleTheme() {
        theme = theme.next();
    }

    public ModuleManager() {
        modules.add(new VisualModule("NameTags", "Visuals", "Enhanced entity name tags"));
        modules.add(new TargetEspModule());
        ((TargetEspModule) get("Target ESP")).setManager(this);
        modules.add(new ArmorHudModule());
        ((ArmorHudModule) get("Armor HUD")).setManager(this);
        modules.add(new VisualModule("China Hat", "Visuals", "Cosmetic player hat"));
        modules.add(new VisualModule("Hit Color", "Visuals", "Custom damage tint"));
        modules.add(new VisualModule("Block Outline", "Visuals", "Custom block selection outline"));
        modules.add(new FullbrightModule());
        modules.add(new VisualModule("Player Model", "Player", "Client-side player model options"));
        modules.add(new NoHurtCamModule());
        modules.add(new VisualModule("Crosshair", "HUD", "Custom crosshair"));
        modules.add(new VisualModule("FPS Counter", "HUD", "FPS indicator"));
        modules.add(new CpsCounterModule());
        modules.add(new VisualModule("Keystrokes", "HUD", "Movement and mouse keys"));
        modules.add(new FreeLookModule());
        modules.add(new TapeMouseModule());

        freeLookKey = RadineeClient.registerModuleKey("Free Look", InputUtil.UNKNOWN_KEY.getCode());
        get("Free Look").setKeyBinding(freeLookKey);
        initHudPositions();
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
        if (client.currentScreen instanceof VisualsScreen || client.currentScreen instanceof HudEditorScreen) {
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

        renderExtraHud(ctx, client, "Crosshair");
        renderExtraHud(ctx, client, "FPS Counter");
        renderExtraHud(ctx, client, "CPS Counter");
        renderExtraHud(ctx, client, "Keystrokes");
    }

    private void renderExtraHud(DrawContext ctx, MinecraftClient client, String name) {
        Module module = get(name);
        if (module == null || !module.isEnabled()) return;

        int[] pos = getHudPosition(name);
        int x = pos[0];
        int y = pos[1];
        int color = hudColor(0xFFFFFF);
        int bg = hudColor(0x101820);

        } else if ("Crosshair".equals(name)) {
            VisualModule vm = module instanceof VisualModule v ? v : null;
            if (vm != null && !vm.optionA) return;
            int size = vm == null ? 7 : Math.max(2, Math.min(20, vm.valueA / 10));
            int gap = vm == null ? 1 : Math.max(1, Math.min(8, vm.valueB / 15));
            int cx = x < 0 ? ctx.getScaledWindowWidth() / 2 : x;
            int cy = y < 0 ? ctx.getScaledWindowHeight() / 2 : y;
            ctx.fill(cx - 1, cy - size - gap, cx + 1, cy - gap, color);
            ctx.fill(cx - 1, cy + gap, cx + 1, cy + size + gap, color);
            ctx.fill(cx - size - gap, cy - 1, cx - gap, cy + 1, color);
            ctx.fill(cx + gap, cy - 1, cx + size + gap, cy + 1, color);
        } else if ("FPS Counter".equals(name)) {
            VisualModule vm = module instanceof VisualModule v ? v : null;
            if (vm != null && !vm.optionA) return;
            if (vm == null || vm.optionB) ctx.fill(x, y, x + 86, y + 18, bg);
            ctx.drawTextWithShadow(client.textRenderer, "FPS: " + client.getCurrentFps(), x + 5, y + 5, color);
        } else if ("CPS Counter".equals(name)) {
            ctx.fill(x, y, x + 86, y + 18, bg);
            ctx.drawTextWithShadow(client.textRenderer, "CPS: " + getCps(), x + 5, y + 5, color);
        } else if ("Keystrokes".equals(name)) {
            VisualModule vm = module instanceof VisualModule v ? v : null;
            if (vm == null) return;
            boolean showMouse = vm.optionA;
            boolean showWasd = vm.optionB;
            int scale = Math.max(50, Math.min(150, vm.valueA));
            int keyW = Math.max(18, 28 * scale / 100);
            int mouseW = Math.max(28, 44 * scale / 100);
            int keyH = Math.max(12, 18 * scale / 100);
            int gap = Math.max(2, 3 * scale / 100);
            int totalW = keyW * 3 + gap * 2;
            int totalH = keyH * (showMouse ? 3 : 2) + gap * 2;
            ctx.fill(x, y, x + Math.max(94, totalW), y + totalH, bg);
            int c = hudColor(0xFFFFFF);
            if (showWasd) {
                drawKeyScaled(ctx, client, "W", x + keyW + gap, y, keyW, keyH, client.options.forwardKey.isPressed(), c);
                drawKeyScaled(ctx, client, "A", x, y + keyH + gap, keyW, keyH, client.options.leftKey.isPressed(), c);
                drawKeyScaled(ctx, client, "S", x + keyW + gap, y + keyH + gap, keyW, keyH, client.options.backKey.isPressed(), c);
                drawKeyScaled(ctx, client, "D", x + (keyW + gap) * 2, y + keyH + gap, keyW, keyH, client.options.rightKey.isPressed(), c);
            }
            if (showMouse) {
                int mouseY = showWasd ? y + (keyH + gap) * 2 : y;
                drawKeyScaled(ctx, client, "LMB", x, mouseY, mouseW, keyH, client.options.attackKey.isPressed(), c);
                drawKeyScaled(ctx, client, "RMB", x + mouseW + gap, mouseY, mouseW, keyH, client.options.useKey.isPressed(), c);
            }
        }
    }

    private void drawKeyScaled(DrawContext ctx, MinecraftClient client, String text, int x, int y, int width, int height, boolean pressed, int color) {
        int bg = hudColor(pressed ? 0x365A70 : 0x202830);
        ctx.fill(x, y, x + width, y + height, bg);
        int textWidth = client.textRenderer.getWidth(text);
        int textY = y + Math.max(1, (height - 9) / 2);
        ctx.drawTextWithShadow(client.textRenderer, text, x + (width - textWidth) / 2, textY, color);
    }

    private int getCps() {
        Module module = get("CPS Counter");
        if (module instanceof CpsCounterModule cps) return cps.getCps();
        return 0;
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
            ctx.drawTextWithShadow(textRenderer, "EDIT HUD", panelX + panelW - 92, panelY + 38, t.accent);

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

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= panelX + panelW - 105 && mouseX < panelX + panelW - 10
                && mouseY >= panelY + 28 && mouseY < panelY + 52) {
                client.setScreen(new HudEditorScreen(this, manager));
                return true;
            }

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

    private static class HudEditorScreen extends Screen {
        private final Screen parent;
        private final ModuleManager manager;
        private String dragging;
        private int offsetX, offsetY;

        HudEditorScreen(Screen parent, ModuleManager manager) {
            super(Text.literal("Radinee HUD Editor"));
            this.parent = parent;
            this.manager = manager;
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            renderBackground(ctx, mouseX, mouseY, delta);
            int sw = ctx.getScaledWindowWidth();
            int sh = client.getWindow().getScaledHeight();

            ctx.drawTextWithShadow(textRenderer, "HUD EDITOR", 8, 8, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "Drag enabled HUD modules with LMB", 8, 22, 0xFF9DB5C4);

            Module target = manager.get("Target ESP");
            if (target instanceof TargetEspModule te && target.isEnabled() && te.showTargetHud) {
                int x = manager.getTargetHudX() < 0 ? (sw - 150) / 2 : manager.getTargetHudX();
                int y = manager.getTargetHudY() < 0 ? (sh - 42) / 2 : manager.getTargetHudY();
                ctx.fill(x, y, x + 150, y + 42, hudColor(0x101820));
                ctx.drawTextWithShadow(textRenderer, "Target ESP", x + 8, y + 6, hudColor(0xFFFFFF));
                ctx.drawTextWithShadow(textRenderer, "DRAG", x + 8, y + 20, hudColor(0x9DB5C4));
                ctx.fill(x + 8, y + 34, x + 128, y + 38, hudColor(0x55FF55));
            }

            String[] extraNames = {"Crosshair", "FPS Counter", "CPS Counter", "Keystrokes"};
            for (String name : extraNames) {
                Module extraModule = manager.get(name);
                if (extraModule != null && extraModule.isEnabled()) {
                    int[] p = manager.getHudPosition(name);
                    int ex = p[0], ey = p[1];
                    if ("Crosshair".equals(name) && ex < 0) {
                        ex = (sw - 16) / 2;
                        ey = (sh - 16) / 2;
                    }
                    int ew = "Keystrokes".equals(name) ? 94 : ("Crosshair".equals(name) ? 16 : 86);
                    int eh = "Keystrokes".equals(name) ? 64 : 18;
                    ctx.fill(ex, ey, ex + ew, ey + eh, hudColor(0x25303A));
                    ctx.drawTextWithShadow(textRenderer, name, ex + 4, ey + 4, hudColor(0xFFFFFF));
                }
            }

            Module armor = manager.get("Armor HUD");
            if (armor instanceof ArmorHudModule && armor.isEnabled()) {
                int x = manager.getArmorHudX();
                int y = manager.getArmorHudY() < 0 ? sh - 42 : manager.getArmorHudY();
                ctx.fill(x, y, x + 152, y + 42, hudColor(0x101820));
                ctx.drawTextWithShadow(textRenderer, "Armor HUD", x + 8, y + 6, hudColor(0xFFFFFF));
                ctx.drawTextWithShadow(textRenderer, "DRAG", x + 8, y + 20, hudColor(0x9DB5C4));
            }

            ctx.drawTextWithShadow(textRenderer, "ESC = back", 8, sh - 16, 0xFF9DB5C4);
            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled);

            double mx = click.x();
            double my = click.y();
            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();

            Module target = manager.get("Target ESP");
            int targetX = manager.getTargetHudX() < 0 ? (sw - 150) / 2 : manager.getTargetHudX();
            int targetY = manager.getTargetHudY() < 0 ? (sh - 42) / 2 : manager.getTargetHudY();
            if (target != null && target.isEnabled() && mx >= targetX && mx <= targetX + 150
                && my >= targetY && my <= targetY + 42) {
                dragging = "Target ESP";
                offsetX = (int) mx - targetX;
                offsetY = (int) my - targetY;
                return true;
            }

            String[] extraNames = {"Crosshair", "FPS Counter", "CPS Counter", "Keystrokes"};
            for (String name : extraNames) {
                Module extraModule = manager.get(name);
                if (extraModule == null || !extraModule.isEnabled()) continue;
                int[] p = manager.getHudPosition(name);
                int ex = p[0], ey = p[1];
                if ("Crosshair".equals(name) && ex < 0) {
                    ex = (sw - 16) / 2;
                    ey = (sh - 16) / 2;
                }
                int ew = "Keystrokes".equals(name) ? 94 : ("Crosshair".equals(name) ? 16 : 86);
                int eh = "Keystrokes".equals(name) ? 64 : 18;
                if (mx >= ex && mx <= ex + ew && my >= ey && my <= ey + eh) {
                    dragging = name;
                    offsetX = (int) mx - ex;
                    offsetY = (int) my - ey;
                    return true;
                }
            }

            Module armor = manager.get("Armor HUD");
            int armorX = manager.getArmorHudX();
            int armorY = manager.getArmorHudY() < 0 ? sh - 42 : manager.getArmorHudY();
            if (armor != null && armor.isEnabled() && mx >= armorX && mx <= armorX + 152
                && my >= armorY && my <= armorY + 42) {
                dragging = "Armor HUD";
                offsetX = (int) mx - armorX;
                offsetY = (int) my - armorY;
                return true;
            }
            return true;
        }

        @Override
        public boolean mouseDragged(Click click, double offsetX, double offsetY) {
            if (dragging == null || click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

            int x = (int) click.x() - this.offsetX;
            int y = (int) click.y() - this.offsetY;
            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();

            if ("Target ESP".equals(dragging)) {
                manager.setTargetHudPosition(x, y, sw, sh);
            } else if ("Armor HUD".equals(dragging)) {
                manager.setArmorHudPosition(x, y, sw, sh);
            } else {
                manager.setHudPosition(dragging, x, y, sw, sh);
            }
            return true;
        }

        @Override
        public boolean mouseReleased(Click click) {
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                dragging = null;
                return true;
            }
            return super.mouseReleased(click);
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                client.setScreen(parent);
                return true;
            }
            return super.keyPressed(input);
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
            int w = Math.min(600, width - 40);
            int h = 390;
            int x = (width - w) / 2;
            int y = (height - h) / 2;
            Theme t = ((VisualsScreen) parent).manager.theme;

            ctx.fill(x, y, x + w, y + h, t.panel);
            ctx.fill(x, y, x + w, y + 2, t.accent);
            ctx.drawTextWithShadow(textRenderer, module.getName(), x + 20, y + 18, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "Module settings", x + 20, y + 35, t.muted);

            int line = y + 62;
            renderModuleSettings(ctx, x + 20, line, w - 40, t);

            String bind = module.getKeyBinding() == null
                ? "NONE"
                : module.getKeyBinding().getBoundKeyLocalizedText().getString();
            ctx.drawTextWithShadow(textRenderer, "Keybind: " + bind, x + 20, y + h - 42, 0xFFD9F7FF);
            ctx.drawTextWithShadow(textRenderer, "ESC = back", x + w - 90, y + h - 22, 0xFF87B7C8);
            super.render(ctx, mouseX, mouseY, delta);
        }

        private void renderModuleSettings(DrawContext ctx, int x, int line, int w, Theme t) {
            if (module instanceof VisualModule vm) {
                switch (module.getName()) {
                    case "NameTags" -> {
                        toggleRow(ctx, x, line, "Show health", vm.optionA);
                        toggleRow(ctx, x, line + 42, "Show distance", vm.optionB);
                        slider(ctx, x, line + 88, w, "Scale", vm.valueA);
                    }
                    case "China Hat" -> {
                        toggleRow(ctx, x, line, "Follow head rotation", vm.optionA);
                        toggleRow(ctx, x, line + 42, "Show in first person", vm.optionB);
                        slider(ctx, x, line + 88, w, "Hat size", vm.valueA);
                        slider(ctx, x, line + 134, w, "Hat opacity", vm.valueB);
                    }
                    case "Hit Color" -> {
                        toggleRow(ctx, x, line, "Enable damage tint", vm.optionA);
                        slider(ctx, x, line + 46, w, "Intensity", vm.valueA);
                        slider(ctx, x, line + 92, w, "Duration", vm.valueB);
                    }
                    case "Block Outline" -> {
                        toggleRow(ctx, x, line, "Show outline", vm.optionA);
                        slider(ctx, x, line + 46, w, "Thickness", vm.valueA);
                        slider(ctx, x, line + 92, w, "Opacity", vm.valueB);
                    }
                    case "Player Model" -> {
                        toggleRow(ctx, x, line, "Show model changes", vm.optionA);
                        toggleRow(ctx, x, line + 42, "Rotate with camera", vm.optionB);
                        slider(ctx, x, line + 88, w, "Model scale", vm.valueA);
                    }
                    case "Crosshair" -> {
                        toggleRow(ctx, x, line, "Custom crosshair", vm.optionA);
                        slider(ctx, x, line + 46, w, "Size", vm.valueA);
                        slider(ctx, x, line + 92, w, "Gap", vm.valueB);
                    }
                    case "FPS Counter" -> {
                        toggleRow(ctx, x, line, "Show FPS label", vm.optionA);
                        toggleRow(ctx, x, line + 42, "Show background", vm.optionB);
                        slider(ctx, x, line + 88, w, "Update rate", vm.valueA);
                    }
                    case "Keystrokes" -> {
                        toggleRow(ctx, x, line, "Show mouse buttons", vm.optionA);
                        toggleRow(ctx, x, line + 42, "Show WASD", vm.optionB);
                        slider(ctx, x, line + 88, w, "Scale", vm.valueA);
                    }
                }
                return;
            }

            if (module instanceof TargetEspModule te) {
                toggleRow(ctx, x, line, "Target HUD", te.showTargetHud);
                toggleRow(ctx, x, line + 42, "Health bar", te.showHealthBar);
                slider(ctx, x, line + 88, w, "HUD opacity", manager().hudOpacity);
            } else if (module instanceof ArmorHudModule ah) {
                toggleRow(ctx, x, line, "Armor HUD", ah.showArmor);
                ctx.drawTextWithShadow(textRenderer, "Display: " + ah.mode + "  [CLICK]", x, line + 44, 0xFFFFFFFF);
                slider(ctx, x, line + 82, w, "HUD opacity", manager().hudOpacity);
            } else if (module instanceof FullbrightModule fb) {
                slider(ctx, x, line, w, "Brightness", fb.brightness);
            } else if (module instanceof NoHurtCamModule nh) {
                slider(ctx, x, line, w, "Shake reduction", nh.reduction);
            } else if (module instanceof TapeMouseModule tm) {
                ctx.drawTextWithShadow(textRenderer, "Click mode: " + tm.mode + "  [CLICK]", x, line, 0xFFFFFFFF);
                slider(ctx, x, line + 42, w, "Ticks between clicks", tm.ticks * 5);
            } else if (module instanceof CpsCounterModule cps) {
                toggleRow(ctx, x, line, "Count while key is held", cps.countHeld);
                slider(ctx, x, line + 46, w, "Max CPS", cps.maxCps * 5);
            } else if (module instanceof FreeLookModule fl) {
                slider(ctx, x, line, w, "Camera sensitivity", fl.sensitivity * 10);
                toggleRow(ctx, x, line + 46, "Invert Y", fl.invertY);
                ctx.drawTextWithShadow(textRenderer, "Hold key: " + fl.keyName(), x, line + 92, 0xFFD9F7FF);
                ctx.drawTextWithShadow(textRenderer, "Right click the line below to rebind", x, line + 112, 0xFF87B7C8);
            }
        }

        private ModuleManager manager() {
            if (parent instanceof VisualsScreen vs) return vs.manager;
            return RadineeClient.MODULES;
        }

        private void toggleRow(DrawContext ctx, int x, int y, String label, boolean value) {
            ctx.drawTextWithShadow(textRenderer, label + ": " + (value ? "ON" : "OFF") + "  [CLICK]", x, y, 0xFFFFFFFF);
        }

        private void slider(DrawContext ctx, int x, int y, int w, String label, int value) {
            int v = Math.max(0, Math.min(100, value));
            ctx.drawTextWithShadow(textRenderer, label + ": " + v + "%", x, y, 0xFFFFFFFF);
            ctx.fill(x, y + 20, x + w, y + 26, 0xFF303840);
            int knob = x + (w * v / 100);
            ctx.fill(knob - 3, y + 15, knob + 3, y + 31, 0xFF37BDEB);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            double mx = click.x();
            double my = click.y();
            int button = click.button();
            int w = Math.min(600, width - 40);
            int h = 390;
            int x = (width - w) / 2;
            int y = (height - h) / 2;
            int line = y + 62;
            int contentW = w - 40;

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (module instanceof VisualModule vm) {
                    switch (module.getName()) {
                        case "NameTags" -> {
                            if (my >= line && my <= line + 30) vm.optionA = !vm.optionA;
                            else if (my >= line + 42 && my <= line + 72) vm.optionB = !vm.optionB;
                            else if (my >= line + 82 && my <= line + 120) vm.valueA = sliderValue(mx, x + 20, contentW);
                        }
                        case "China Hat" -> {
                            if (my >= line && my <= line + 30) vm.optionA = !vm.optionA;
                            else if (my >= line + 42 && my <= line + 72) vm.optionB = !vm.optionB;
                            else if (my >= line + 82 && my <= line + 120) vm.valueA = sliderValue(mx, x + 20, contentW);
                            else if (my >= line + 128 && my <= line + 166) vm.valueB = sliderValue(mx, x + 20, contentW);
                        }
                        case "Hit Color", "Block Outline" -> {
                            if (my >= line && my <= line + 30) vm.optionA = !vm.optionA;
                            else if (my >= line + 40 && my <= line + 78) vm.valueA = sliderValue(mx, x + 20, contentW);
                            else if (my >= line + 86 && my <= line + 124) vm.valueB = sliderValue(mx, x + 20, contentW);
                        }
                        case "Player Model" -> {
                            if (my >= line && my <= line + 30) vm.optionA = !vm.optionA;
                            else if (my >= line + 42 && my <= line + 72) vm.optionB = !vm.optionB;
                            else if (my >= line + 82 && my <= line + 120) vm.valueA = sliderValue(mx, x + 20, contentW);
                        }
                        case "Crosshair" -> {
                            if (my >= line && my <= line + 30) vm.optionA = !vm.optionA;
                            else if (my >= line + 40 && my <= line + 78) vm.valueA = sliderValue(mx, x + 20, contentW);
                            else if (my >= line + 86 && my <= line + 124) vm.valueB = sliderValue(mx, x + 20, contentW);
                        }
                        case "FPS Counter", "Keystrokes" -> {
                            if (my >= line && my <= line + 30) vm.optionA = !vm.optionA;
                            else if (my >= line + 42 && my <= line + 72) vm.optionB = !vm.optionB;
                            else if (my >= line + 82 && my <= line + 120) vm.valueA = sliderValue(mx, x + 20, contentW);
                        }
                    }
                } else if (module instanceof TargetEspModule te) {
                    if (my >= line && my <= line + 30) te.showTargetHud = !te.showTargetHud;
                    else if (my >= line + 42 && my <= line + 72) te.showHealthBar = !te.showHealthBar;
                    else if (my >= line + 82 && my <= line + 120) manager().setHudOpacity(sliderValue(mx, x + 20, contentW));
                } else if (module instanceof ArmorHudModule ah) {
                    if (my >= line && my <= line + 30) ah.showArmor = !ah.showArmor;
                    else if (my >= line + 35 && my <= line + 72) ah.mode = ah.mode.next();
                    else if (my >= line + 76 && my <= line + 118) manager().setHudOpacity(sliderValue(mx, x + 20, contentW));
                } else if (module instanceof FullbrightModule fb && my >= line && my <= line + 42) {
                    fb.brightness = sliderValue(mx, x + 20, contentW);
                    fb.apply(client);
                } else if (module instanceof NoHurtCamModule nh && my >= line && my <= line + 42) {
                    nh.reduction = sliderValue(mx, x + 20, contentW);
                } else if (module instanceof TapeMouseModule tm) {
                    if (my >= line && my <= line + 38) tm.mode = tm.mode.next();
                    else if (my >= line + 42 && my <= line + 82) tm.ticks = Math.max(1, Math.min(20, sliderValue(mx, x + 20, contentW) / 5));
                } else if (module instanceof CpsCounterModule cps) {
                    if (my >= line && my <= line + 30) cps.countHeld = !cps.countHeld;
                    else if (my >= line + 42 && my <= line + 82) cps.maxCps = Math.max(1, Math.min(20, sliderValue(mx, x + 20, contentW) / 5));
                } else if (module instanceof FreeLookModule fl) {
                    if (my >= line && my <= line + 42) fl.sensitivity = Math.max(1, sliderValue(mx, x + 20, contentW) / 10.0f);
                    else if (my >= line + 46 && my <= line + 76) fl.invertY = !fl.invertY;
                    else if (my >= line + 90 && my <= line + 130) fl.startRebind();
                }
            }

            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && module instanceof FreeLookModule fl
                && my >= line + 90 && my <= line + 140) {
                fl.startRebind();
                return true;
            }

            return super.mouseClicked(click, doubled);
        }

        private int sliderValue(double mx, int x, int w) {
            return Math.max(0, Math.min(100, (int) (((mx - x) / (double) w) * 100)));
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            int keyCode = input.key();
            if (module instanceof FreeLookModule fl && fl.rebinding) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) fl.cancelRebind();
                else fl.setBinding(InputUtil.Type.KEYSYM.createFromCode(keyCode));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                client.setScreen(parent);
                return true;
            }
            return super.keyPressed(input);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }


    private static class VisualModule extends Module {
        boolean optionA = true;
        boolean optionB = true;
        int valueA = 100;
        int valueB = 100;

        VisualModule(String name, String category, String description) {
            super(name, category, description);
        }
    }

    private static class CpsCounterModule extends Module {
        private int cps;
        private int lastClickTick = -100;
        private int tickCounter;
        boolean countHeld = true;
        int maxCps = 20;

        CpsCounterModule() {
            super("CPS Counter", "HUD", "Clicks per second");
        }

        @Override
        public void onTick(MinecraftClient client) {
            tickCounter++;
            boolean clicking = client.options.attackKey.isPressed();
            if (countHeld && clicking && tickCounter - lastClickTick > 2) {
                lastClickTick = tickCounter;
                cps = Math.min(maxCps, cps + 1);
            } else if (cps > 0 && tickCounter % 20 == 0) {
                cps--;
            }
        }

        int getCps() {
            return cps;
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
        boolean showHealthBar = true;
        private ModuleManager manager;

        void setManager(ModuleManager manager) {
            this.manager = manager;
        }

        TargetEspModule() {
            super("Target ESP", "Combat", "Target HUD with nickname and health bar");
        }

        void render(DrawContext ctx, MinecraftClient client) {
            if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;

            Entity entity = hit.getEntity();
            if (!(entity instanceof LivingEntity living)) return;
            if (entity == client.player) return;

            int hudWidth = 150;
            int hudHeight = 42;
            int x = manager.getTargetHudX() < 0 ? (ctx.getScaledWindowWidth() - hudWidth) / 2 : manager.getTargetHudX();
            int y = manager.getTargetHudY() < 0 ? (client.getWindow().getScaledHeight() - hudHeight) / 2 : manager.getTargetHudY();
            float health = Math.max(0.0f, living.getHealth());
            float maxHealth = Math.max(1.0f, living.getMaxHealth());
            int barWidth = 120;
            int filled = Math.round(barWidth * Math.min(1.0f, health / maxHealth));

            ctx.fill(x, y, x + hudWidth, y + hudHeight, hudColor(0x101820));
            ctx.drawTextWithShadow(client.textRenderer, living.getDisplayName(),
                x + 8, y + 6, hudColor(0xFFFFFF));
            ctx.drawTextWithShadow(client.textRenderer,
                String.format("%.1f HP", health), x + 8, y + 20, hudColor(0x9DB5C4));
            if (showHealthBar) {
                ctx.fill(x + 8, y + 34, x + 8 + barWidth, y + 38, hudColor(0x3A3A3A));
                ctx.fill(x + 8, y + 34, x + 8 + filled, y + 38, hudColor(0x55FF55));
            }
        }
    }

    private static class ArmorHudModule extends Module {
        private ModuleManager manager;
        enum Mode {
            NUMBERS, BAR, PERCENT;

            Mode next() {
                return values()[(ordinal() + 1) % values().length];
            }
        }

        Mode mode = Mode.NUMBERS;
        boolean showArmor = true;

        ArmorHudModule() {
            super("Armor HUD", "HUD", "Armor durability with color-coded values");
        }

        void setManager(ModuleManager manager) {
            this.manager = manager;
        }

        void render(DrawContext ctx, MinecraftClient client) {
            if (!showArmor) return;
            int x = manager.getArmorHudX();
            int y = manager.getArmorHudY() < 0 ? client.getWindow().getScaledHeight() - 42 : manager.getArmorHudY();

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
                            slotX, y + 18, withAlpha(color));
                    } else if (mode == Mode.PERCENT) {
                        ctx.drawTextWithShadow(client.textRenderer,
                            String.format("%.0f%%", percent),
                            slotX, y + 18, color);
                    } else {
                        int width = Math.max(0, Math.min(28, Math.round(28.0f * percent / 100.0f)));
                        ctx.fill(slotX, y + 18, slotX + 28, y + 22, hudColor(0x333333));
                        ctx.fill(slotX, y + 18, slotX + width, y + 22, withAlpha(color));
                    }
                }
            }
        }

        static int withAlpha(int color) {
            return ((hudOpacity * 255 / 100) << 24) | (color & 0x00FFFFFF);
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

    private static class TapeMouseModule extends Module {
        enum ClickMode {
            LEFT, RIGHT;

            ClickMode next() {
                return values()[(ordinal() + 1) % values().length];
            }
        }

        int ticks = 2;
        ClickMode mode = ClickMode.LEFT;
        private int timer;

        TapeMouseModule() {
            super("TapeMouse", "Combat", "Repeats left/right click actions every configured number of ticks");
        }

        @Override
        public void onEnable(MinecraftClient client) {
            timer = 0;
        }

        @Override
        public void onDisable(MinecraftClient client) {
            timer = 0;
        }

        @Override
        public void onTick(MinecraftClient client) {
            if (client.player == null || client.world == null
                || client.currentScreen != null || client.interactionManager == null) {
                timer = 0;
                return;
            }

            if (timer > 0) {
                timer--;
                return;
            }

            if (mode == ClickMode.LEFT) {
                if (client.crosshairTarget instanceof EntityHitResult hit) {
                    Entity entity = hit.getEntity();
                    if (entity != client.player) {
                        client.interactionManager.attackEntity(client.player, entity);
                    }
                } else if (client.crosshairTarget instanceof BlockHitResult hit) {
                    client.interactionManager.attackBlock(hit.getBlockPos(), hit.getSide());
                }
            } else {
                if (client.crosshairTarget instanceof EntityHitResult hit) {
                    Entity entity = hit.getEntity();
                    if (entity != client.player) {
                        client.interactionManager.interactEntity(client.player, entity, Hand.MAIN_HAND);
                    }
                } else if (client.crosshairTarget instanceof BlockHitResult hit) {
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
                } else {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                }
            }

            timer = Math.max(0, ticks - 1);
        }
    }

    private static class FreeLookModule extends Module {
        boolean rebinding;
        float sensitivity = 1.5f;
        boolean invertY;

        FreeLookModule() {
            super("Free Look", "Player", "Hold a key to rotate the camera independently of the player");
        }

        @Override
        public void onTick(MinecraftClient client) {
            ru.sedmoyy.radinee.FreeLookState.configure(sensitivity, invertY);
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
