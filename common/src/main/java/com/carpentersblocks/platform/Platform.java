package com.carpentersblocks.platform;

import com.carpentersblocks.blockentity.CoverableBlockEntity;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * The few things Fabric and Forge do differently. Implementations live in
 * {@code com.carpentersblocks.platform.fabric.PlatformImpl} and {@code ...forge.PlatformImpl}.
 */
public final class Platform {
    private Platform() {
    }

    @ExpectPlatform
    public static <T extends Block> Supplier<T> registerBlock(String id, Supplier<T> factory) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String id, Supplier<T> factory) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static CreativeModeTab createTab(String id, Supplier<ItemStack> icon) {
        throw new AssertionError();
    }

    /** Creates the loader-specific subclass, which knows how to sync itself to clients. */
    @ExpectPlatform
    public static CoverableBlockEntity createCoverableBlockEntity() {
        throw new AssertionError();
    }
}
