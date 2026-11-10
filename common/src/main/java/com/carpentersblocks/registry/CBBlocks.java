package com.carpentersblocks.registry;

import com.carpentersblocks.block.BarrierBlock;
import com.carpentersblocks.block.ButtonBlock;
import com.carpentersblocks.block.CollapsibleBlock;
import com.carpentersblocks.block.DoorBlock;
import com.carpentersblocks.block.GarageDoorBlock;
import com.carpentersblocks.block.GateBlock;
import com.carpentersblocks.block.HatchBlock;
import com.carpentersblocks.block.LadderBlock;
import com.carpentersblocks.block.LeverBlock;
import com.carpentersblocks.block.PressurePlateBlock;
import com.carpentersblocks.block.StairsBlock;
import com.carpentersblocks.block.TorchBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.carpentersblocks.registry.CBContent.block;
import static com.carpentersblocks.registry.CBContent.itemProps;
import static com.carpentersblocks.registry.CBContent.wood;

/**
 * The box-shaped Carpenter's blocks (stairs, doors, redstone parts...). Registered through
 * {@link CBContent#block}, so rendering, colours and item models are wired up automatically.
 */
public final class CBBlocks {
    private static final Function<Block, BlockState> DEFAULT = Block::defaultBlockState;

    public static final Supplier<StairsBlock> STAIRS = block("stairs",
        () -> new StairsBlock(wood()), b -> new BlockItem(b, itemProps()), DEFAULT::apply);
    public static final Supplier<CollapsibleBlock> COLLAPSIBLE = block("collapsible_block",
        () -> new CollapsibleBlock(wood()), b -> new BlockItem(b, itemProps()), DEFAULT::apply);
    public static final Supplier<BarrierBlock> BARRIER = block("barrier",
        () -> new BarrierBlock(wood()), b -> new BlockItem(b, itemProps()),
        b -> b.defaultBlockState().setValue(BarrierBlock.EAST, true).setValue(BarrierBlock.WEST, true).setValue(BarrierBlock.POST, true));
    public static final Supplier<GateBlock> GATE = block("gate",
        () -> new GateBlock(wood()), b -> new BlockItem(b, itemProps()), DEFAULT::apply);
    public static final Supplier<HatchBlock> HATCH = block("hatch",
        () -> new HatchBlock(wood()), b -> new BlockItem(b, itemProps()),
        b -> b.defaultBlockState().setValue(HatchBlock.OPEN, true));
    public static final Supplier<DoorBlock> DOOR = block("door",
        () -> new DoorBlock(wood()), b -> new BlockItem(b, itemProps()), null); // flat icon, like vanilla doors
    public static final Supplier<GarageDoorBlock> GARAGE_DOOR = block("garage_door",
        () -> new GarageDoorBlock(wood()), b -> new BlockItem(b, itemProps()), DEFAULT::apply);
    public static final Supplier<ButtonBlock> BUTTON = block("button",
        () -> new ButtonBlock(circuit()), b -> new BlockItem(b, itemProps()),
        b -> b.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH));
    public static final Supplier<LeverBlock> LEVER = block("lever",
        () -> new LeverBlock(circuit()), b -> new BlockItem(b, itemProps()), DEFAULT::apply);
    public static final Supplier<PressurePlateBlock> PRESSURE_PLATE = block("pressure_plate",
        () -> new PressurePlateBlock(circuit()), b -> new BlockItem(b, itemProps()), DEFAULT::apply);
    public static final Supplier<TorchBlock> TORCH = block("torch",
        () -> new TorchBlock(circuit()), b -> new BlockItem(b, itemProps()), DEFAULT::apply);
    public static final Supplier<LadderBlock> LADDER = block("ladder",
        () -> new LadderBlock(wood().sound(SoundType.LADDER)), b -> new BlockItem(b, itemProps()), DEFAULT::apply);

    private CBBlocks() {
    }

    /** Small redstone parts: break instantly-ish, like vanilla buttons and torches. */
    private static BlockBehaviour.Properties circuit() {
        return BlockBehaviour.Properties.of(Material.DECORATION).strength(0.5F).sound(SoundType.WOOD);
    }

    /** Forces class loading so the fields above are registered. */
    static void init() {
    }
}
