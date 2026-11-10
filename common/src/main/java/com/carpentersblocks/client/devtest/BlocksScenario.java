package com.carpentersblocks.client.devtest;

import com.carpentersblocks.block.BarrierBlock;
import com.carpentersblocks.block.ButtonBlock;
import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.block.DoorBlock;
import com.carpentersblocks.block.GateBlock;
import com.carpentersblocks.block.HatchBlock;
import com.carpentersblocks.block.LadderBlock;
import com.carpentersblocks.block.LeverBlock;
import com.carpentersblocks.block.StairsBlock;
import com.carpentersblocks.block.TorchBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.shape.CollapsibleShape;
import com.carpentersblocks.core.shape.SlopeType;
import com.carpentersblocks.registry.CBBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.carpentersblocks.client.devtest.ClientScenario.command;
import static com.carpentersblocks.client.devtest.ClientScenario.give;
import static com.carpentersblocks.client.devtest.ClientScenario.onServer;
import static com.carpentersblocks.client.devtest.ClientScenario.shot;
import static com.carpentersblocks.client.devtest.ClientScenario.teleport;
import static com.carpentersblocks.client.devtest.ClientScenario.useOn;

/**
 * Gallery of every box-shaped block, west of the main scene: row z = 0 uncovered, row z = 3
 * covered with oak planks, a stone wall at z = 8 with wall-mounted blocks on its north face,
 * and a garage door hanging from a ceiling at z = 12.
 */
final class BlocksScenario {
    private static final int X0 = -40;
    private static final int Y = 200;

    private BlocksScenario() {
    }

    /** One exhibit per column: how to build its state (null = skip in that row). */
    private static List<Function<ServerLevel, BlockState>> floorRow() {
        List<Function<ServerLevel, BlockState>> row = new ArrayList<>();
        row.add(l -> CBBlocks.STAIRS.get().defaultBlockState().setValue(StairsBlock.SHAPE, SlopeType.WEDGE_POS_N.ordinal()));
        row.add(l -> CBBlocks.STAIRS.get().defaultBlockState().setValue(StairsBlock.SHAPE, SlopeType.WEDGE_EXT_POS_NE.ordinal()));
        row.add(l -> CBBlocks.COLLAPSIBLE.get().defaultBlockState());
        row.add(l -> CBBlocks.BARRIER.get().defaultBlockState());
        row.add(l -> CBBlocks.GATE.get().defaultBlockState());
        row.add(l -> CBBlocks.GATE.get().defaultBlockState().setValue(GateBlock.OPEN, true));
        row.add(l -> CBBlocks.DOOR.get().defaultBlockState());
        row.add(l -> CBBlocks.DOOR.get().defaultBlockState().setValue(DoorBlock.OPEN, true).setValue(DoorBlock.STYLE, DoorBlock.Style.FRENCH_GLASS));
        row.add(l -> CBBlocks.BUTTON.get().defaultBlockState());
        row.add(l -> CBBlocks.LEVER.get().defaultBlockState());
        row.add(l -> CBBlocks.LEVER.get().defaultBlockState().setValue(LeverBlock.POWERED, true).setValue(LeverBlock.ALONG_X, true));
        row.add(l -> CBBlocks.PRESSURE_PLATE.get().defaultBlockState());
        row.add(l -> CBBlocks.TORCH.get().defaultBlockState());
        row.add(l -> CBBlocks.TORCH.get().defaultBlockState().setValue(TorchBlock.TYPE, TorchBlock.Type.LANTERN));
        row.add(l -> CBBlocks.LADDER.get().defaultBlockState().setValue(LadderBlock.PLACEMENT, LadderBlock.Placement.FREE_X));
        return row;
    }

    /** Wall-mounted exhibits on the north face of the wall at z = 8 (they face north). */
    private static List<Function<ServerLevel, BlockState>> wallRow() {
        List<Function<ServerLevel, BlockState>> row = new ArrayList<>();
        Direction n = Direction.NORTH;
        row.add(l -> CBBlocks.HATCH.get().defaultBlockState().setValue(HatchBlock.FACING, n).setValue(HatchBlock.OPEN, true));
        row.add(l -> CBBlocks.HATCH.get().defaultBlockState().setValue(HatchBlock.FACING, n).setValue(HatchBlock.HIGH, true)
            .setValue(HatchBlock.STYLE, HatchBlock.Style.WINDOW));
        row.add(l -> CBBlocks.HATCH.get().defaultBlockState().setValue(HatchBlock.FACING, n).setValue(HatchBlock.OPEN, true)
            .setValue(HatchBlock.STYLE, HatchBlock.Style.FRENCH_WINDOW));
        row.add(l -> CBBlocks.BUTTON.get().defaultBlockState().setValue(ButtonBlock.FACING, n));
        row.add(l -> CBBlocks.LEVER.get().defaultBlockState().setValue(LeverBlock.FACING, n));
        row.add(l -> CBBlocks.LEVER.get().defaultBlockState().setValue(LeverBlock.FACING, n).setValue(LeverBlock.POWERED, true));
        row.add(l -> CBBlocks.TORCH.get().defaultBlockState().setValue(TorchBlock.FACING, n));
        row.add(l -> CBBlocks.TORCH.get().defaultBlockState().setValue(TorchBlock.FACING, n).setValue(TorchBlock.TYPE, TorchBlock.Type.LANTERN));
        row.add(l -> CBBlocks.LADDER.get().defaultBlockState().setValue(LadderBlock.PLACEMENT, LadderBlock.Placement.NORTH));
        row.add(l -> CBBlocks.LADDER.get().defaultBlockState().setValue(LadderBlock.PLACEMENT, LadderBlock.Placement.NORTH)
            .setValue(LadderBlock.STYLE, LadderBlock.Style.RAIL));
        row.add(l -> CBBlocks.LADDER.get().defaultBlockState().setValue(LadderBlock.PLACEMENT, LadderBlock.Placement.NORTH)
            .setValue(LadderBlock.STYLE, LadderBlock.Style.POLE));
        return row;
    }

    static void build() {
        Minecraft mc = Minecraft.getInstance();
        ClientTestHarness.step("fly to gallery", () -> teleport(X0 + 0.5, Y + 4, -8, 0, 30), 100);
        ClientTestHarness.waitUntil("gallery loaded",
            () -> mc.level.hasChunkAt(new BlockPos(X0 + 20, Y, 16)) && mc.level.hasChunkAt(new BlockPos(X0 - 20, Y, -8)), 1200, 20);

        ClientTestHarness.step("build gallery", () -> onServer(server -> {
            command(server, "time set 6000");
            command(server, "fill " + (X0 - 20) + " " + (Y - 1) + " -8 " + (X0 + 20) + " " + (Y - 1) + " 16 minecraft:smooth_stone");
            command(server, "fill " + (X0 - 20) + " " + Y + " -8 " + (X0 + 20) + " " + (Y + 6) + " 16 minecraft:air");
            ServerLevel level = server.overworld();
            List<Function<ServerLevel, BlockState>> floor = floorRow();
            for (int i = 0; i < floor.size(); i++) {
                int x = X0 - 15 + i * 2;
                place(level, new BlockPos(x, Y, 0), floor.get(i).apply(level), null);
                place(level, new BlockPos(x, Y, 3), floor.get(i).apply(level), Items.OAK_PLANKS);
            }
            // A three-piece barrier run to show joining (the single one above shows a post).
            for (int x = X0 + 18; x <= X0 + 20; x++) {
                place(level, new BlockPos(x, Y, 0), CBBlocks.BARRIER.get().defaultBlockState(), null);
            }
            command(server, "fill " + (X0 - 15) + " " + Y + " 8 " + (X0 + 10) + " " + (Y + 2) + " 8 minecraft:stone");
            List<Function<ServerLevel, BlockState>> wall = wallRow();
            for (int i = 0; i < wall.size(); i++) {
                place(level, new BlockPos(X0 - 14 + i * 2, Y + 1, 7), wall.get(i).apply(level), i % 2 == 0 ? null : Items.OAK_PLANKS);
            }
            // Garage door hanging from a three-block ceiling.
            command(server, "fill " + (X0 - 1) + " " + (Y + 3) + " 12 " + (X0 + 1) + " " + (Y + 3) + " 12 minecraft:stone");
            for (int x = X0 - 1; x <= X0 + 1; x++) {
                BlockPos top = new BlockPos(x, Y + 2, 12);
                BlockState host = CBBlocks.GARAGE_DOOR.get().defaultBlockState();
                level.setBlockAndUpdate(top, host);
                host.getBlock().setPlacedBy(level, top, host, null, ItemStack.EMPTY);
            }
            // Collapsible blocks forming a little ramp.
            for (int i = 0; i < 3; i++) {
                BlockPos at = new BlockPos(X0 + 4 + i, Y, 12);
                place(level, at, CBBlocks.COLLAPSIBLE.get().defaultBlockState(), Items.GRASS_BLOCK);
                setShape(level, at, CollapsibleShape.pack(16 - i * 5, 11 - i * 5, 16 - i * 5, 11 - i * 5));
            }
        }), 40);

        // ---- clicks through the real input path ------------------------------------------------
        BlockPos doorAt = new BlockPos(X0 - 15 + 6 * 2, Y, 0);
        BlockPos leverAt = new BlockPos(X0 - 15 + 9 * 2, Y, 0);
        BlockPos buttonAt = new BlockPos(X0 - 15 + 8 * 2, Y, 0);
        ClientTestHarness.step("stand by the doors", () -> teleport(X0 - 2.5, Y, -2.5, 0, 30), 40);
        give(Items.STICK);
        ClientTestHarness.step("open the door", () -> useOn(doorAt, Direction.NORTH), 10);
        ClientTestHarness.step("check door", () -> onServer(server -> {
            BlockState lower = server.overworld().getBlockState(doorAt), upper = server.overworld().getBlockState(doorAt.above());
            ClientTestHarness.check("right-click opens both halves of the door",
                lower.getBlock() instanceof DoorBlock && lower.getValue(DoorBlock.OPEN) && upper.getValue(DoorBlock.OPEN));
        }), 0);
        ClientTestHarness.step("pull the lever", () -> useOn(leverAt, Direction.UP), 10);
        ClientTestHarness.step("press the button", () -> useOn(buttonAt, Direction.UP), 5);
        ClientTestHarness.step("check redstone", () -> onServer(server -> {
            ServerLevel level = server.overworld();
            ClientTestHarness.check("lever switches on and powers the floor below",
                level.getBlockState(leverAt).getValue(LeverBlock.POWERED) && level.getDirectSignal(leverAt, Direction.UP) == 15);
            ClientTestHarness.check("button is pressed", level.getBlockState(buttonAt).getValue(ButtonBlock.POWERED));
        }), 30);
        ClientTestHarness.step("check button release", () -> onServer(server -> ClientTestHarness.check("button releases after 20 ticks",
            !server.overworld().getBlockState(buttonAt).getValue(ButtonBlock.POWERED))), 0);

        // ---- screenshots --------------------------------------------------------------------
        shot("gallery_overview", X0 + 0.5, Y + 7, -9, 0, 35);
        shot("gallery_left", X0 - 10, Y + 2.5, -4, 0, 25);
        shot("gallery_middle", X0 - 1, Y + 2.5, -4, 0, 25);
        shot("gallery_right", X0 + 8, Y + 2.5, -4, 0, 25);
        shot("gallery_wall_left", X0 - 9, Y + 2, 4.5, 0, 15);
        shot("gallery_wall_right", X0 + 1, Y + 2, 4.5, 0, 15);
        shot("gallery_garage", X0 + 2, Y + 2, 8.5, 0, 20);
        // Close-ups of the wall row: lever off, lever on, torch, lantern (front and side).
        for (int i = 4; i <= 7; i++) {
            double x = X0 - 14 + i * 2 + 0.5;
            shot("wall_" + i + "_front", x, Y + 0.6, 5.2, 0, 10);
            shot("wall_" + i + "_side", x + 1.8, Y + 0.6, 7.5, 90, 10);
        }
        ClientTestHarness.step("night", () -> onServer(server -> command(server, "time set 18000")), 0);
        shot("gallery_night", X0 - 4, Y + 4, -6, 0, 30);
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state, net.minecraft.world.item.Item cover) {
        // A lower door half alone would remove itself when its shape is refreshed: place it as is.
        boolean door = state.getBlock() instanceof DoorBlock;
        level.setBlockAndUpdate(pos, door ? state : Block.updateFromNeighbourShapes(state, level, pos));
        BlockState placed = level.getBlockState(pos);
        if (door) {
            placed.getBlock().setPlacedBy(level, pos, placed, null, ItemStack.EMPTY);
        }
        if (cover != null && placed.getBlock() instanceof CoverableBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CoverableBlockEntity) {
                ((CoverableBlockEntity) be).putItem(AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST), new ItemStack(cover));
            }
            if (placed.getBlock() instanceof DoorBlock) {
                BlockEntity upper = level.getBlockEntity(pos.above());
                if (upper instanceof CoverableBlockEntity) {
                    ((CoverableBlockEntity) upper).putItem(AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST), new ItemStack(cover));
                }
            }
        }
        // Barriers and ladders join their neighbours: refresh the ones around us.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos at = pos.relative(dir);
            BlockState other = level.getBlockState(at);
            if (other.getBlock() instanceof BarrierBlock || other.getBlock() instanceof LadderBlock) {
                level.setBlockAndUpdate(at, Block.updateFromNeighbourShapes(other, level, at));
            }
        }
    }

    private static void setShape(ServerLevel level, BlockPos pos, int packed) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CoverableBlockEntity) {
            ((CoverableBlockEntity) be).setShapeData(packed | 1 << 20);
        }
    }
}
