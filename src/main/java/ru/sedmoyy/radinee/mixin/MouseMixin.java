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

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void radinee$freeLook(double timeDelta, CallbackInfo ci) {
        if (!RadineeClient.isFreeLookActive()) {
            if (FreeLookState.isActive()) FreeLookState.end();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;
        FreeLookState.begin(client);
        FreeLookState.update(cursorDeltaX, cursorDeltaY);
        cursorDeltaX = 0.0;
        cursorDeltaY = 0.0;
        ci.cancel();
    }
}