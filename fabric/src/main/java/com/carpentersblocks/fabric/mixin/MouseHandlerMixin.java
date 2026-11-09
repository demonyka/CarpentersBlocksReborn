package com.carpentersblocks.fabric.mixin;

import com.carpentersblocks.client.SlopeScrollHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric API 1.16 has no scroll event; this is the Forge MouseScrollEvent equivalent. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void carpentersblocks$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow()) {
            return;
        }
        // Same amount vanilla uses to move the hotbar selection.
        double delta = (mc.options.discreteMouseScroll ? Math.signum(yOffset) : yOffset) * mc.options.mouseWheelSensitivity;
        if (SlopeScrollHandler.onScroll(delta)) {
            ci.cancel();
        }
    }
}
