package com.carpentersblocks.fabric.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Loads a normal JSON model and wraps the baked result (block covering model or item shape model). */
public final class FabricCoverableUnbakedModel implements UnbakedModel {
    private final ResourceLocation inner;
    private final UnaryOperator<BakedModel> wrapper;

    public FabricCoverableUnbakedModel(ResourceLocation inner, UnaryOperator<BakedModel> wrapper) {
        this.inner = inner;
        this.wrapper = wrapper;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.singletonList(inner);
    }

    @Override
    public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> models, Set<Pair<String, String>> errors) {
        return models.apply(inner).getMaterials(models, errors);
    }

    @Override
    public BakedModel bake(ModelBakery bakery, Function<Material, TextureAtlasSprite> sprites, ModelState state, ResourceLocation id) {
        BakedModel base = bakery.bake(inner, state);
        return base == null ? null : wrapper.apply(base);
    }
}
