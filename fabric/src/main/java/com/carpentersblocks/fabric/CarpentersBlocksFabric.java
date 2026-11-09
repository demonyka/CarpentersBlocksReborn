package com.carpentersblocks.fabric;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.interaction.Interactions;
import com.carpentersblocks.util.MultiplayerTest;
import com.carpentersblocks.util.SmokeTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

public final class CarpentersBlocksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CarpentersBlocks.init();

        // Fires before the vanilla rule "sneaking with an item skips Block#use", on both sides.
        UseBlockCallback.EVENT.register(Interactions::use);

        // Server side: a left-click packet reached the server (the client did not predict it).
        AttackBlockCallback.EVENT.register((player, level, hand, pos, face) -> {
            if (!level.isClientSide && Interactions.attack(player, level, pos, face, false)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        FabricNetworking.registerServer();
        ServerLifecycleEvents.SERVER_STARTED.register(SmokeTest::onServerStarted);
        ServerLifecycleEvents.SERVER_STARTED.register(MultiplayerTest::onServerStarted);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MultiplayerTest.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> MultiplayerTest.onPlayerLeave(handler.player));
    }
}
