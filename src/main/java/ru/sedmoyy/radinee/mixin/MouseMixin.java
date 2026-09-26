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

    private float radinee$playerYaw;
    private float radinee$playerPitch;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void radinee$prepareFreeLook(double timeDelta, CallbackInfo ci) {
        if (!RadineeClient.isFreeLookActive()) {
            if (FreeLookState.isActive()) FreeLookState.end();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        radinee$playerYaw = client.player.getYaw();
        radinee$playerPitch = client.player.getPitch();
        FreeLookState.begin(client);
    }

    @Inject(method = "updateMouse", at = @At("TAIL"))
    private void radinee$captureFreeLook(double timeDelta, CallbackInfo ci) {
        if (!RadineeClient.isFreeLookActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        FreeLookState.update(cursorDeltaX, cursorDeltaY);

        // Vanilla mouse handling has already updated the player.
        // Restore the player's rotation; CameraMixin uses FreeLookState for the view.
        client.player.setYaw(radinee$playerYaw);
        client.player.setPitch(radinee$playerPitch);
    }
}