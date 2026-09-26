package ru.sedmoyy.radinee;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.sedmoyy.radinee.module.ModuleManager;

public class RadineeClient implements ClientModInitializer {
    public static ModuleManager MODULES;

    @Override
    public void onInitializeClient() {
        MODULES = new ModuleManager();
        KeyBinding menuKey = registerModuleKey("ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) MODULES.toggleMenu(client);
            MODULES.tick(client);
        });
    }

    public static KeyBinding registerModuleKey(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.radinee." + name.toLowerCase().replace(" ", "_"),
            InputUtil.Type.KEYSYM, key, "category.radinee"
        ));
    }

    public static boolean isFreeLookActive() {
        return MODULES != null && MODULES.isFreeLookActive();
    }
}