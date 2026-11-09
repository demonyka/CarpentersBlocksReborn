package com.carpentersblocks.fabric;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.client.devtest.ClientTestHarness;
import com.carpentersblocks.client.render.CoverBlockColor;
import com.carpentersblocks.client.render.ShapeItemModel;
import com.carpentersblocks.fabric.client.FabricCoverableModel;
import com.carpentersblocks.fabric.client.FabricCoverableUnbakedModel;
import com.carpentersblocks.util.UncoveredTextures;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.Block;
import com.carpentersblocks.interaction.Interactions;
import com.carpentersblocks.registry.CBContent;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashSet;
import java.util.Set;

public final class CarpentersBlocksFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Client side: predict the hammer click, stop vanilla breaking, ask the server to do it.
        AttackBlockCallback.EVENT.register((player, level, hand, pos, face) -> {
            if (level.isClientSide && Interactions.attack(player, level, pos, face, true)) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeBlockPos(pos);
                buf.writeEnum(face);
                ClientPlayNetworking.send(FabricNetworking.HAMMER_ATTACK, buf);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        // Cutout so glass and leaves work as covers.
        for (Block block : CBContent.coverableBlocks()) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutoutMipped());
            ColorProviderRegistry.BLOCK.register(new CoverBlockColor(), block);
        }
        ClientSpriteRegistryCallback.event(TextureAtlas.LOCATION_BLOCKS)
            .register((atlas, registry) -> UncoveredTextures.ALL.forEach(registry::register));

        // Fabric 1.16 has no "after bake" hook, so block-state variants of our blocks are mapped
        // to wrapped JSON models here (Forge does the same in ModelBakeEvent).
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(resources -> (id, context) -> {
            if (!id.getNamespace().equals(CarpentersBlocks.MOD_ID)) {
                return null;
            }
            String path = id.getPath();
            boolean item = id.getVariant().equals("inventory");
            if (path.equals("block") && !item) {
                // Variant "light=..,shape=X" -> JSON model carpentersblocks:block/block_X.
                for (String property : id.getVariant().split(",")) {
                    if (property.startsWith("shape=")) {
                        return new FabricCoverableUnbakedModel(CarpentersBlocks.id("block/block_" + property.substring(6)),
                            FabricCoverableModel::new);
                    }
                }
            } else if (!item && coverablePaths().contains(path)) {
                // Generated geometry; the JSON model only provides the particle texture.
                return new FabricCoverableUnbakedModel(CarpentersBlocks.id("block/coverable"), FabricCoverableModel::new);
            } else if (item) {
                BlockState display = CBContent.itemDisplayStates().get(path);
                if (display != null) {
                    return new FabricCoverableUnbakedModel(CarpentersBlocks.id("block/coverable"),
                        base -> new ShapeItemModel(base, display));
                }
            }
            return null;
        });

        if (ClientTestHarness.enabled()) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> ClientTestHarness.tick());
        }
    }

    /** Registry paths of coverable blocks other than the Carpenter's Block (which has JSON shapes). */
    private static Set<String> coverablePaths() {
        Set<String> paths = new HashSet<>();
        for (Block block : CBContent.coverableBlocks()) {
            String path = Registry.BLOCK.getKey(block).getPath();
            if (!path.equals("block")) {
                paths.add(path);
            }
        }
        return paths;
    }
}
