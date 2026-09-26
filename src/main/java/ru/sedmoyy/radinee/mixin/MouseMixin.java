package ru.sedmoyy.radinee.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.sedmoyy.radinee.FreeLookState;
import ru.sedmoyy.radinee.RadineeClient;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void radinee$prepareFreeLook(double timeDelta, CallbackInfo ci) {
        if (RadineeClient.isFreeLookActive()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.currentScreen == null) {
                FreeLookState.begin(client);
            }
        } else if (FreeLookState.isActive()) {
            FreeLookState.end();
        }
    }

    @Inject(method = "updateMouse", at = @At("TAIL"))
    private void radinee$captureFreeLook(double timeDelta, CallbackInfo ci) {
        if (!RadineeClient.isFreeLookActive()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        FreeLookState.update(cursorDeltaX, cursorDeltaY);

        // Restore the player's normal look so Free Look never rotates the player.
        client.player.setYaw(client.player.getYaw());
        client.player.setPitch(client.player.getPitch());
    }
}