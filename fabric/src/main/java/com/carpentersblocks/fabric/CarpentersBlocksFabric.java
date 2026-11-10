package com.carpentersblocks.fabric;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.interaction.Interactions;
import com.carpentersblocks.util.DevTests;
import com.carpentersblocks.util.MultiplayerTest;
import com.carpentersblocks.util.SmokeTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;

public final class CarpentersBlocksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CarpentersBlocks.init();

        // Right-clicks arrive through CoverableBlock#use and the game mode mixins, after every
        // UseBlockCallback listener (claims, protection) has run.

        // Server side: a left-click packet reached the server (the client did not predict it).
        AttackBlockCallback.EVENT.register((player, level, hand, pos, face) -> {
            if (!level.isClientSide && !FabricNetworking.isProbing()
                && Interactions.attack(player, level, pos, face, false)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        FabricNetworking.registerServer();
        if (DevTests.server()) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> SmokeTest.onServerStarted(server));
            ServerLifecycleEvents.SERVER_STARTED.register(server -> MultiplayerTest.onServerStarted(server));
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MultiplayerTest.onPlayerJoin(handler.player));
            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> MultiplayerTest.onPlayerLeave(handler.player));
        }
    }
}
