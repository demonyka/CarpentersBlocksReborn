package com.carpentersblocks.forge;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.interaction.Interactions;
import com.carpentersblocks.platform.forge.PlatformImpl;
import com.carpentersblocks.util.DevTests;
import com.carpentersblocks.util.MultiplayerTest;
import com.carpentersblocks.util.SmokeTest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.carpentersblocks.block.CoverableBlock;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
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

        // Last and never for cancelled events: claim and protection mods decide first.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.RightClickBlock.class,
            CarpentersBlocksForge::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.LeftClickBlock.class,
            CarpentersBlocksForge::onLeftClickBlock);
        if (DevTests.server()) {
            MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) -> SmokeTest.onServerStarted(event.getServer()));
            MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) -> MultiplayerTest.onServerStarted(event.getServer()));
            MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> MultiplayerTest.onPlayerJoin((ServerPlayer) event.getPlayer()));
            MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> MultiplayerTest.onPlayerLeave((ServerPlayer) event.getPlayer()));
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CarpentersBlocksForgeClient.init(modBus));
    }

    /**
     * Vanilla skips {@code Block#use} for players sneaking with an item, which is how dyes and
     * side covers are applied. Asking Forge to use the block anyway sends the click to
     * {@code CoverableBlock#use} on both sides, unless another mod denied it.
     */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() != Event.Result.DENY && event.getWorld().hasChunkAt(event.getPos())
            && event.getWorld().getBlockState(event.getPos()).getBlock() instanceof CoverableBlock) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

    /**
     * Fires on the client (prediction, stops local breaking) and on the server after the
     * reach check (does the work). The client still sends the vanilla packet when cancelled.
     */
    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getUseItem() == Event.Result.DENY) {
            return;
        }
        boolean client = event.getWorld().isClientSide;
        if (Interactions.attack(event.getPlayer(), event.getWorld(), event.getPos(), event.getFace(), client)) {
            event.setCanceled(true);
        }
    }
}
