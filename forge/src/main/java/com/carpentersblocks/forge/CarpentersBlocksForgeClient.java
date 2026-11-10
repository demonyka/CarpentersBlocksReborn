package com.carpentersblocks.forge;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.client.devtest.ClientTestHarness;
import com.carpentersblocks.client.render.CoverBlockColor;
import com.carpentersblocks.client.render.ShapeItemModel;
import com.carpentersblocks.forge.client.ForgeCoverableModel;
import com.carpentersblocks.client.SlopeScrollHandler;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

/** Client-only setup. Loaded through DistExecutor, never on a dedicated server. */
public final class CarpentersBlocksForgeClient {
    private CarpentersBlocksForgeClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(CarpentersBlocksForgeClient::onClientSetup);
        modBus.addListener(CarpentersBlocksForgeClient::onModelBake);
        modBus.addListener(CarpentersBlocksForgeClient::onBlockColors);
        modBus.addListener(CarpentersBlocksForgeClient::onTextureStitch);
        MinecraftForge.EVENT_BUS.addListener((InputEvent.MouseScrollEvent event) -> {
            if (SlopeScrollHandler.onScroll(event.getScrollDelta())) {
                event.setCanceled(true);
            }
        });

        if (com.carpentersblocks.util.DevTests.client()) {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
                if (event.phase == TickEvent.Phase.END) {
                    ClientTestHarness.tick();
                }
            });
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // Every layer: each cover is drawn in its own (stained glass translucent, leaves cut out...).
        for (Block block : CBContent.coverableBlocks()) {
            ItemBlockRenderTypes.setRenderLayer(block, layer -> layer != RenderType.tripwire());
        }
    }

    private static void onModelBake(ModelBakeEvent event) {
        Map<ResourceLocation, BakedModel> models = event.getModelRegistry();
        for (Block block : CBContent.coverableBlocks()) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation id = BlockModelShaper.stateToModelLocation(state);
                BakedModel base = models.get(id);
                if (base != null) {
                    models.put(id, new ForgeCoverableModel(base));
                }
            }
        }
        CBContent.itemDisplayStates().forEach((path, display) -> {
            ModelResourceLocation id = new ModelResourceLocation(CarpentersBlocks.id(path), "inventory");
            BakedModel base = models.get(id);
            if (base != null) {
                models.put(id, new ShapeItemModel(base, display));
            }
        });
    }

    private static void onTextureStitch(TextureStitchEvent.Pre event) {
        if (event.getMap().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            UncoveredTextures.ALL.forEach(event::addSprite);
        }
    }

    private static void onBlockColors(ColorHandlerEvent.Block event) {
        event.getBlockColors().register(new CoverBlockColor(), CBContent.coverableBlocks().toArray(new Block[0]));
    }
}
