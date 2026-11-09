package com.carpentersblocks.platform.fabric;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.fabric.FabricCoverableBlockEntity;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** Fabric side of {@link com.carpentersblocks.platform.Platform}. Registers immediately. */
public final class PlatformImpl {
    private PlatformImpl() {
    }

    public static <T extends Block> Supplier<T> registerBlock(String id, Supplier<T> factory) {
        T value = Registry.register(Registry.BLOCK, CarpentersBlocks.id(id), factory.get());
        return () -> value;
    }

    public static <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory) {
        T value = Registry.register(Registry.ITEM, CarpentersBlocks.id(id), factory.get());
        return () -> value;
    }

    public static <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String id, Supplier<T> factory) {
        T value = Registry.register(Registry.BLOCK_ENTITY_TYPE, CarpentersBlocks.id(id), factory.get());
        return () -> value;
    }

    public static CreativeModeTab createTab(String id, Supplier<ItemStack> icon) {
        // Despite the package name this builder is safe on dedicated servers.
        return FabricItemGroupBuilder.build(CarpentersBlocks.id(id), icon);
    }

    public static CoverableBlockEntity createCoverableBlockEntity() {
        return new FabricCoverableBlockEntity();
    }
}
