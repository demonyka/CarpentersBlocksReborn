package com.carpentersblocks.fabric;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.interaction.Interactions;
import com.carpentersblocks.interaction.SlopeItemSwitch;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
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

    private FabricNetworking() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(HAMMER_ATTACK, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            Direction face = buf.readEnum(Direction.class);
            // Run on the server thread, never on the network thread.
            server.execute(() -> Interactions.attack(player, player.getLevel(), pos, face, false));
        });
        ServerPlayNetworking.registerGlobalReceiver(SLOPE_SWITCH, (server, player, handler, buf, responseSender) -> {
            boolean forward = buf.readBoolean();
            server.execute(() -> SlopeItemSwitch.apply(player, forward));
        });
    }
}
