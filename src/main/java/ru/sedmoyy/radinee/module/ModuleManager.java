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

        if ("Crosshair".equals(name)) {
            int cx = x < 0 ? ctx.getScaledWindowWidth() / 2 : x;
            int cy = y < 0 ? ctx.getScaledWindowHeight() / 2 : y;
            ctx.fill(cx - 1, cy - 7, cx + 1, cy + 7, color);
            ctx.fill(cx - 7, cy - 1, cx + 7, cy + 1, color);
        } else if ("FPS Counter".equals(name)) {
            ctx.fill(x, y, x + 86, y + 18, bg);
            ctx.drawTextWithShadow(client.textRenderer, "FPS: " + client.getCurrentFps(), x + 5, y + 5, color);
        } else if ("CPS Counter".equals(name)) {
            ctx.fill(x, y, x + 86, y + 18, bg);
            ctx.drawTextWithShadow(client.textRenderer, "CPS: " + getCps(), x + 5, y + 5, color);
        } else if ("Keystrokes".equals(name)) {
            ctx.fill(x, y, x + 94, y + 64, bg);
            int c = hudColor(0xFFFFFF);
            drawKey(ctx, client, "W", x + 31, y, client.options.forwardKey.isPressed(), c);
            drawKey(ctx, client, "A", x, y + 21, client.options.leftKey.isPressed(), c);
            drawKey(ctx, client, "S", x + 31, y + 21, client.options.backKey.isPressed(), c);
            drawKey(ctx, client, "D", x + 62, y + 21, client.options.rightKey.isPressed(), c);
            drawKey(ctx, client, "LMB", x, y + 42, client.options.attackKey.isPressed(), c);
            drawKey(ctx, client, "RMB", x + 48, y + 42, client.options.useKey.isPressed(), c);
        }
    }

    private void drawKey(DrawContext ctx, MinecraftClient client, String text, int x, int y, boolean pressed, int color) {
        int bg = hudColor(pressed ? 0x365A70 : 0x202830);
        int width = text.length() > 1 ? 44 : 28;
        ctx.fill(x, y, x + width, y + 18, bg);
        int textWidth = client.textRenderer.getWidth(text);
        ctx.drawTextWithShadow(client.textRenderer, text, x + (width - textWidth) / 2, y + 5, color);
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
            } else if (module instanceof TapeMouseModule tm) {
                drawSlider(ctx, x + 20, line, w - 40, "Ticks between clicks", tm.ticks);
            } else if (module instanceof ArmorHudModule ah) {
                drawSlider(ctx, x + 20, line + 50, w - 40, "HUD opacity", manager.hudOpacity);
            } else if (module instanceof TargetEspModule te) {
                drawSlider(ctx, x + 20, line + 50, w - 40, "HUD opacity", manager.hudOpacity);
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

            if (module instanceof TapeMouseModule && my >= line + 12 && my <= line + 40) {
                ((TapeMouseModule) module).ticks = Math.max(1, Math.min(20,
                    (int) (((mx - (x + 20)) / (double) (w - 40)) * 20) + 1));
                return true;
            }

            if ((module instanceof ArmorHudModule || module instanceof TargetEspModule) && my >= line + 62 && my <= line + 92) {
                manager.setHudOpacity((int) (((mx - (x + 20)) / (double) (w - 40)) * 100));
                return true;
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

    private static class CpsCounterModule extends Module {
        private int cps;
        private int lastClickTick = -100;
        private int tickCounter;

        CpsCounterModule() {
            super("CPS Counter", "HUD", "Clicks per second");
        }

        @Override
        public void onTick(MinecraftClient client) {
            tickCounter++;
            if (client.options.attackKey.isPressed() && tickCounter - lastClickTick > 2) {
                lastClickTick = tickCounter;
                cps = Math.min(20, cps + 1);
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
            ctx.fill(x + 8, y + 34, x + 8 + barWidth, y + 38, hudColor(0x3A3A3A));
            ctx.fill(x + 8, y + 34, x + 8 + filled, y + 38, hudColor(0x55FF55));
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

        ArmorHudModule() {
            super("Armor HUD", "HUD", "Armor durability with color-coded values");
        }

        void setManager(ModuleManager manager) {
            this.manager = manager;
        }

        void render(DrawContext ctx, MinecraftClient client) {
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
        int ticks = 2;
        private int timer;

        TapeMouseModule() {
            super("TapeMouse", "Combat", "Repeats left-click actions every configured number of ticks");
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

            // Simulate a left-click action regardless of whether the crosshair
            // is currently over an entity or a block.
            if (client.crosshairTarget instanceof EntityHitResult hit) {
                Entity entity = hit.getEntity();
                if (entity != client.player) {
                    client.interactionManager.attackEntity(client.player, entity);
                }
            } else if (client.crosshairTarget instanceof BlockHitResult hit) {
                client.interactionManager.attackBlock(hit.getBlockPos(), hit.getSide());
            }

            timer = Math.max(0, ticks - 1);
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
