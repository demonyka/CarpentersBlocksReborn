package com.carpentersblocks.fabric;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.interaction.Interactions;
import com.carpentersblocks.interaction.SlopeItemSwitch;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.carpentersblocks.block.CoverableBlock;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric cancels a client-side left-click without telling the server, so the client sends
 * this small packet instead. The server treats it exactly like a left-click: the same
 * permission, distance and loaded-chunk checks in {@link Interactions#attack}.
 */
public final class FabricNetworking {
    public static final ResourceLocation HAMMER_ATTACK = CarpentersBlocks.id("hammer_attack");
    /** Sneak + mouse wheel with a slope in hand (see {@link SlopeItemSwitch}). */
    public static final ResourceLocation SLOPE_SWITCH = CarpentersBlocks.id("slope_switch");

    /** True while other mods are being asked whether a hammer click is allowed. Server thread only. */
    private static boolean probing;

    private FabricNetworking() {
    }

    static boolean isProbing() {
        return probing;
    }

    /**
     * The hammer click the client predicted. Other mods' left-click listeners (claims,
     * protection) are asked first, whatever order the mods were loaded in; ours stays out of
     * that round and does the work afterwards.
     */
    private static void hammerAttack(ServerPlayer player, BlockPos pos, Direction face) {
        ServerLevel level = player.getLevel();
        if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof CoverableBlock)) {
            return;
        }
        InteractionResult others;
        probing = true;
        try {
            others = AttackBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, pos, face);
        } finally {
            probing = false;
        }
        if (others == InteractionResult.PASS) {
            Interactions.attack(player, level, pos, face, false);
        } else {
            // Denied: put the client's predicted block back.
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
        }
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(HAMMER_ATTACK, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            Direction face = buf.readEnum(Direction.class);
            // Run on the server thread, never on the network thread.
            server.execute(() -> hammerAttack(player, pos, face));
        });
        ServerPlayNetworking.registerGlobalReceiver(SLOPE_SWITCH, (server, player, handler, buf, responseSender) -> {
            boolean forward = buf.readBoolean();
            server.execute(() -> SlopeItemSwitch.apply(player, forward));
        });
    }
}
