package com.carpentersblocks.fabric.mixin;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.interaction.Interactions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla skips {@code Block#use} for players sneaking with an item, which is how dyes and
 * side covers are applied to Carpenter's blocks. This sends that case to our click handling.
 * It runs after Fabric's {@code UseBlockCallback} (injected at the head of the method), so
 * claim and protection mods have already had the chance to cancel the click.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Inject(method = "useItemOn", cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z"))
    private void carpentersblocks$sneakUse(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                           BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isSecondaryUseActive() && level.getBlockState(hit.getBlockPos()).getBlock() instanceof CoverableBlock) {
            InteractionResult result = Interactions.use(player, level, hand, hit);
            if (result.consumesAction()) {
                cir.setReturnValue(result);
            }
        }
    }
}
