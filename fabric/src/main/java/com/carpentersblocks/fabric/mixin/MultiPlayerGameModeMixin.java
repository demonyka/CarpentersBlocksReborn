package com.carpentersblocks.fabric.mixin;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.interaction.Interactions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client half of {@link ServerPlayerGameModeMixin}: predicts the click and sends the vanilla packet. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow @Final private ClientPacketListener connection;

    @Inject(method = "useItemOn", cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z"))
    private void carpentersblocks$sneakUse(LocalPlayer player, ClientLevel level, InteractionHand hand, BlockHitResult hit,
                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isSecondaryUseActive() && level.getBlockState(hit.getBlockPos()).getBlock() instanceof CoverableBlock) {
            InteractionResult result = Interactions.use(player, level, hand, hit);
            if (result.consumesAction()) {
                this.connection.send(new ServerboundUseItemOnPacket(hand, hit));
                cir.setReturnValue(result);
            }
        }
    }
}
