package com.carpentersblocks.registry;

import com.carpentersblocks.block.CarpentersBlock;
import com.carpentersblocks.block.SlopeBlock;
import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.item.SlopeItem;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.item.HammerItem;
import com.carpentersblocks.platform.Platform;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Every block, item and block entity type the mod adds. Ids match the original mod. */
public final class CBContent {
    /** Durability of the hammer; 1.7.10 default "Vanilla Tool Uses" = 400. */
    private static final int TOOL_USES = 400;

    public static final CreativeModeTab TAB = Platform.createTab("general", () -> new ItemStack(CBContent.BLOCK_ITEM.get()));

    public static final Supplier<CarpentersBlock> BLOCK = Platform.registerBlock("block",
        () -> new CarpentersBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(1.5F).sound(SoundType.WOOD)));

    public static final Supplier<BlockItem> BLOCK_ITEM = Platform.registerItem("block",
        () -> new BlockItem(BLOCK.get(), new Item.Properties().tab(TAB)));

    public static final Supplier<SlopeBlock> SLOPE = Platform.registerBlock("slope",
        () -> new SlopeBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(1.5F).sound(SoundType.WOOD)));

    /** The five slope items; the wedge is registered last so it is the slope block's own item. */
    public static final Map<SlopePlacement.Kind, Supplier<SlopeItem>> SLOPE_ITEMS = registerSlopeItems();

    public static final Supplier<HammerItem> HAMMER = Platform.registerItem("hammer",
        () -> new HammerItem(new Item.Properties().tab(TAB).durability(TOOL_USES)));


    /** Registered in {@link #init()} after every block, so it can list them all (Fabric registers immediately). */
    public static Supplier<BlockEntityType<CoverableBlockEntity>> COVERABLE_BLOCK_ENTITY;

    private CBContent() {
    }

    private static Map<SlopePlacement.Kind, Supplier<SlopeItem>> registerSlopeItems() {
        Map<SlopePlacement.Kind, Supplier<SlopeItem>> items = new EnumMap<>(SlopePlacement.Kind.class);
        List<SlopePlacement.Kind> order = new ArrayList<>(Arrays.asList(SlopePlacement.Kind.values()));
        order.remove(SlopePlacement.Kind.WEDGE);
        order.add(SlopePlacement.Kind.WEDGE);
        for (SlopePlacement.Kind kind : order) {
            items.put(kind, Platform.registerItem(SlopeItem.idOf(kind),
                () -> new SlopeItem(SLOPE.get(), kind, new Item.Properties().tab(TAB))));
        }
        return Collections.unmodifiableMap(items);
    }

    /** Blocks drawn by the covering model (render layer, colours, model wrapping). */
    public static List<net.minecraft.world.level.block.Block> coverableBlocks() {
        List<net.minecraft.world.level.block.Block> blocks = new ArrayList<>(Arrays.asList(BLOCK.get(), SLOPE.get()));
        for (Supplier<? extends net.minecraft.world.level.block.Block> block : BLOCKS) {
            blocks.add(block.get());
        }
        return blocks;
    }

    /**
     * Item models generated from a block state (item registry path -> state shown). Their JSON
     * item models only provide transforms.
     */
    public static Map<String, net.minecraft.world.level.block.state.BlockState> itemDisplayStates() {
        Map<String, net.minecraft.world.level.block.state.BlockState> map = new java.util.LinkedHashMap<>();
        for (SlopePlacement.Kind kind : SlopePlacement.Kind.values()) {
            map.put(SlopeItem.idOf(kind), SlopeBlock.displayState(kind));
        }
        DISPLAY_STATES.forEach((id, state) -> map.put(id, state.get()));
        return map;
    }

    /** Blocks registered through {@link #block}: drawn by the covering model with block/coverable.json. */
    private static final List<Supplier<? extends net.minecraft.world.level.block.Block>> BLOCKS = new ArrayList<>();
    private static final Map<String, Supplier<net.minecraft.world.level.block.state.BlockState>> DISPLAY_STATES = new java.util.LinkedHashMap<>();

    /**
     * Registers a coverable block, and its item unless {@code itemFactory} is null.
     * Its item shows {@code display} (or the default state) in inventories.
     */
    static <B extends net.minecraft.world.level.block.Block> Supplier<B> block(String id, Supplier<B> factory,
                                                                                 java.util.function.Function<B, Item> itemFactory,
                                                                                 java.util.function.Function<B, net.minecraft.world.level.block.state.BlockState> display) {
        Supplier<B> block = Platform.registerBlock(id, factory);
        BLOCKS.add(block);
        if (itemFactory != null) {
            Platform.registerItem(id, () -> itemFactory.apply(block.get()));
            DISPLAY_STATES.put(id, () -> display.apply(block.get()));
        }
        return block;
    }

    static Item.Properties itemProps() {
        return new Item.Properties().tab(TAB);
    }

    static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of(Material.WOOD).strength(1.5F).sound(SoundType.WOOD);
    }

    public static SlopeItem slopeItem(SlopePlacement.Kind kind) {
        return SLOPE_ITEMS.get(kind).get();
    }

    /** Registers everything, in an order that works for both loaders. */
    public static void init() {
        CBBlocks.init();
        COVERABLE_BLOCK_ENTITY = Platform.registerBlockEntity("coverable", () -> BlockEntityType.Builder
            .of(Platform::createCoverableBlockEntity, coverableBlocks().toArray(new net.minecraft.world.level.block.Block[0]))
            .build(null));
    }
}
