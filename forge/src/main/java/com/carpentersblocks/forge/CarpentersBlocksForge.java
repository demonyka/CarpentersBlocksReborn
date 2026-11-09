package com.carpentersblocks.forge;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.interaction.Interactions;
import com.carpentersblocks.platform.forge.PlatformImpl;
import com.carpentersblocks.util.MultiplayerTest;
import com.carpentersblocks.util.SmokeTest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CarpentersBlocks.MOD_ID)
public final class CarpentersBlocksForge {
    public CarpentersBlocksForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CarpentersBlocks.init();
        PlatformImpl.registerAll(modBus);
        ForgeNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(CarpentersBlocksForge::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(CarpentersBlocksForge::onLeftClickBlock);
        MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) -> SmokeTest.onServerStarted(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) -> MultiplayerTest.onServerStarted(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> MultiplayerTest.onPlayerJoin((ServerPlayer) event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> MultiplayerTest.onPlayerLeave((ServerPlayer) event.getPlayer()));

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CarpentersBlocksForgeClient.init(modBus));
    }

    /**
     * Fires on both sides before vanilla skips {@code Block#use} for sneaking players.
     * On the server it runs after vanilla checked reach and spawn protection.
     */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = Interactions.use(event.getPlayer(), event.getWorld(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    /**
     * Fires on the client (prediction, stops local breaking) and on the server after the
     * reach check (does the work). The client still sends the vanilla packet when cancelled.
     */
    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        boolean client = event.getWorld().isClientSide;
        if (Interactions.attack(event.getPlayer(), event.getWorld(), event.getPos(), event.getFace(), client)) {
            event.setCanceled(true);
        }
    }
}
