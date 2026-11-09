package com.carpentersblocks.fabric.mixin;

import com.carpentersblocks.block.HatchBlock;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric equivalent of Forge's {@code isLadder}: an open hidden hatch can be climbed. The
 * climbable tag cannot express it because it depends on the hatch's state.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void carpentersblocks$climbHatch(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!cir.getReturnValueZ() && !self.isSpectator() && HatchBlock.isClimbable(self.getFeetBlockState())) {
            cir.setReturnValue(true);
        }
    }
}
