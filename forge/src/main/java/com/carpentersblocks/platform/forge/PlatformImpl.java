package com.carpentersblocks.platform.forge;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.forge.ForgeCoverableBlockEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/** Forge side of {@link com.carpentersblocks.platform.Platform}. Uses deferred registers. */
public final class PlatformImpl {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CarpentersBlocks.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CarpentersBlocks.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, CarpentersBlocks.MOD_ID);

    private PlatformImpl() {
    }

    /** Called from the mod constructor after {@link CarpentersBlocks#init()}. */
    public static void registerAll(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static <T extends Block> Supplier<T> registerBlock(String id, Supplier<T> factory) {
        return BLOCKS.register(id, factory);
    }

    public static <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory) {
        return ITEMS.register(id, factory);
    }

    public static <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String id, Supplier<T> factory) {
        return BLOCK_ENTITIES.register(id, factory);
    }

    public static CreativeModeTab createTab(String id, Supplier<ItemStack> icon) {
        return new CreativeModeTab(CarpentersBlocks.MOD_ID + "." + id) {
            @Override
            public ItemStack makeIcon() {
                return icon.get();
            }
        };
    }

    public static CoverableBlockEntity createCoverableBlockEntity() {
        return new ForgeCoverableBlockEntity();
    }
}
