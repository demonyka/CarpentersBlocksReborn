package com.carpentersblocks.client.devtest;

import com.carpentersblocks.block.BarrierBlock;
import com.carpentersblocks.block.CarpentersBlock;
import com.carpentersblocks.block.DoorBlock;
import com.carpentersblocks.block.FenceType;
import com.carpentersblocks.block.GarageDoorBlock;
import com.carpentersblocks.block.GateBlock;
import com.carpentersblocks.block.SlabShapeProperty;
import com.carpentersblocks.block.SlopeBlock;
import com.carpentersblocks.block.SlopeTypeProperty;
import com.carpentersblocks.block.StairsBlock;
import com.carpentersblocks.block.TorchBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.shape.CollapsibleShape;
import com.carpentersblocks.core.shape.SlopeType;
import com.carpentersblocks.registry.CBBlocks;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import static com.carpentersblocks.client.devtest.ClientScenario.command;
import static com.carpentersblocks.client.devtest.ClientScenario.onServer;

/**
 * A small build made only of Carpenter's blocks, for gallery screenshots: a cottage with a
 * hipped brick roof and a glass skylight, a shed with a garage door, a fenced yard with a
 * gate, lanterns, a grass mound of collapsible blocks. Run with
 * {@code -Dcarpentersblocks.clientTest=showcase}; nothing is asserted.
 */
final class ShowcaseScenario {
    /** Scene origin; moved to the nearest plains biome so the grass is green. */
    private static int X0 = 240, Z0 = 0;
    private static final int Y = 200;
    private static final AttributeKey COVER = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);
    private static final AttributeKey DYE = AttributeKey.of(AttributeType.DYE, AttributeLocation.HOST);

    private ShowcaseScenario() {
    }

    static void build() {
        Minecraft mc = Minecraft.getInstance();
        ClientTestHarness.step("find plains", () -> onServer(server -> {
            ServerLevel level = server.overworld();
            BlockPos plains = level.findNearestBiome(server.registryAccess().registryOrThrow(net.minecraft.core.Registry.BIOME_REGISTRY)
                .getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS), new BlockPos(0, Y, 0), 6400, 8);
            if (plains != null) {
                X0 = plains.getX();
                Z0 = plains.getZ();
            }
        }), 0);
        ClientTestHarness.step("fly to showcase", () -> ClientScenario.teleport(X0 + 3, Y + 6, Z0 - 14, 0, 30), 160);
        ClientTestHarness.step("build showcase", () -> onServer(server -> {
            ServerLevel level = server.overworld();
            command(server, "time set 4500");
            command(server, "weather clear");
            command(server, "fill " + (X0 - 24) + " " + (Y - 3) + " " + (Z0 - 24) + " " + (X0 + 30) + " " + (Y - 2) + " " + (Z0 + 22) + " minecraft:dirt");
            command(server, "fill " + (X0 - 24) + " " + (Y - 1) + " " + (Z0 - 24) + " " + (X0 + 30) + " " + (Y - 1) + " " + (Z0 + 22) + " minecraft:grass_block");
            command(server, "fill " + (X0 - 24) + " " + Y + " " + (Z0 - 24) + " " + (X0 + 30) + " " + (Y + 12) + " " + (Z0 + 22) + " minecraft:air");
            command(server, "fill " + (X0 + 3) + " " + (Y - 1) + " " + (Z0 - 9) + " " + (X0 + 3) + " " + (Y - 1) + " " + (Z0 - 1) + " minecraft:gravel");

            // ---- cottage: 7 x 5, walls of covered Carpenter's blocks --------------------------
            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 4; z++) {
                    boolean wall = x == 0 || x == 6 || z == 0 || z == 4;
                    for (int y = 0; y <= 2 && wall; y++) {
                        boolean corner = (x == 0 || x == 6) && (z == 0 || z == 4);
                        Item cover = corner ? Items.STRIPPED_SPRUCE_LOG : y == 0 ? Items.STONE_BRICKS : Items.BIRCH_PLANKS;
                        boolean window = y == 1 && !corner && ((z == 0 || z == 4) && (x == 1 || x == 5) || (x == 0 || x == 6) && z == 2);
                        block(level, x, y, z, window ? Items.GLASS : cover);
                    }
                    block(level, x, -1, z, Items.SPRUCE_PLANKS); // floor
                }
            }
            // Front door (north wall, seen from outside) with a step.
            door(level, 3, 0, 0, DoorBlock.Style.GLASS_TOP, Items.DARK_OAK_PLANKS);
            block(level, 3, 2, 0, Items.STRIPPED_SPRUCE_LOG);
            // Hipped roof in rings, one block of overhang; a glass skylight on the front.
            for (int k = 0; k <= 2; k++) {
                int x0 = -1 + k, x1 = 7 - k, z0 = -1 + k, z1 = 5 - k, y = 3 + k;
                for (int x = x0; x <= x1; x++) {
                    boolean skylight = k == 1 && (x == 2 || x == 3 || x == 4);
                    slope(level, x, y, z0, x == x0 ? SlopeType.WEDGE_EXT_POS_NW : x == x1 ? SlopeType.WEDGE_EXT_POS_NE : SlopeType.WEDGE_POS_N,
                        skylight ? Items.GLASS : Items.BRICKS);
                    slope(level, x, y, z1, x == x0 ? SlopeType.WEDGE_EXT_POS_SW : x == x1 ? SlopeType.WEDGE_EXT_POS_SE : SlopeType.WEDGE_POS_S, Items.BRICKS);
                }
                for (int z = z0 + 1; z < z1; z++) {
                    slope(level, x0, y, z, SlopeType.WEDGE_POS_W, Items.BRICKS);
                    slope(level, x1, y, z, SlopeType.WEDGE_POS_E, Items.BRICKS);
                }
            }
            slope(level, 2, 6, 2, SlopeType.PRISM_1P_POS_E, Items.BRICKS);
            slope(level, 3, 6, 2, SlopeType.PRISM_2P_POS_WE, Items.BRICKS);
            slope(level, 4, 6, 2, SlopeType.PRISM_1P_POS_W, Items.BRICKS);
            // Lanterns by the door, a wall torch on each side of it.
            lantern(level, 2, 0, -1, Direction.UP);
            lantern(level, 4, 0, -1, Direction.UP);
            torch(level, 1, 2, -1, Direction.NORTH);
            torch(level, 5, 2, -1, Direction.NORTH);

            // ---- shed with a garage door, east of the cottage ------------------------------------
            for (int x = 9; x <= 13; x++) {
                for (int z = 0; z <= 4; z++) {
                    boolean wall = x == 9 || x == 13 || z == 4 || z == 0 && (x == 9 || x == 13);
                    for (int y = 0; y <= 2 && wall; y++) {
                        block(level, x, y, z, y == 0 ? Items.STONE_BRICKS : Items.SPRUCE_PLANKS);
                    }
                    place(level, x, 3, z, CBContent.BLOCK.get().defaultBlockState().setValue(CarpentersBlock.SHAPE, SlabShapeProperty.DOWN), Items.DARK_OAK_PLANKS);
                    block(level, x, -1, z, Items.STONE);
                }
            }
            for (int x = 10; x <= 12; x++) {
                BlockState host = CBBlocks.GARAGE_DOOR.get().defaultBlockState()
                    .setValue(GarageDoorBlock.FACING, Direction.SOUTH).setValue(GarageDoorBlock.STYLE, GarageDoorBlock.Style.GLASS_TOP);
                BlockPos top = at(x, 2, 0);
                level.setBlockAndUpdate(top, host);
                host.getBlock().setPlacedBy(level, top, host, null, ItemStack.EMPTY);
                for (int y = 0; y <= 2; y++) {
                    cover(level, at(x, y, 0), Items.OAK_PLANKS);
                }
            }

            // ---- yard: fence with a gate, a grass mound, stairs up to it ------------------------------
            BlockState fence = CBBlocks.BARRIER.get().defaultBlockState().setValue(BarrierBlock.TYPE, FenceType.PICKET);
            for (int x = -5; x <= 15; x++) {
                if (x == 3) {
                    place(level, x, 0, -9, CBBlocks.GATE.get().defaultBlockState().setValue(GateBlock.TYPE, FenceType.PICKET)
                        .setValue(GateBlock.ALONG_X, true), Items.DARK_OAK_PLANKS);
                } else {
                    place(level, x, 0, -9, fence, Items.DARK_OAK_PLANKS);
                }
            }
            for (int z = -8; z <= 4; z++) {
                place(level, -5, 0, z, fence, Items.DARK_OAK_PLANKS);
                place(level, 15, 0, z, fence, Items.DARK_OAK_PLANKS);
            }
            for (int x = -5; x <= 15; x++) {
                for (int z = -9; z <= 4; z++) {
                    BlockPos p = at(x, 0, z);
                    if (level.getBlockState(p).getBlock() instanceof BarrierBlock || level.getBlockState(p).getBlock() instanceof GateBlock) {
                        level.getBlockState(p).updateNeighbourShapes(level, p, 3);
                        level.blockUpdated(p, level.getBlockState(p).getBlock());
                    }
                }
            }
            lantern(level, 2, 0, -8, Direction.UP);
            lantern(level, 4, 0, -8, Direction.UP);
            // A mound: 4 x 2 collapsible blocks, grass covered, with a ridge down the middle.
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 2; j++) {
                    int[] rowA = j == 0 ? new int[] { 0, 6, 12, 6, 0 } : new int[] { 0, 6, 12, 6, 0 };
                    int h00 = (j == 0 ? 0 : rowA[i]), h10 = (j == 0 ? 0 : rowA[i + 1]);
                    int h01 = (j == 0 ? rowA[i] : 0), h11 = (j == 0 ? rowA[i + 1] : 0);
                    BlockPos p = at(-3 + i, 0, -5 + j);
                    place(level, -3 + i, 0, -5 + j, CBBlocks.COLLAPSIBLE.get().defaultBlockState(), Items.GRASS_BLOCK);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof CoverableBlockEntity) {
                        ((CoverableBlockEntity) be).setShapeData(CollapsibleShape.pack(Math.max(1, h00), Math.max(1, h10), Math.max(1, h01), Math.max(1, h11)) | 1 << 20);
                    }
                }
            }
            // Dyed blocks and slabs: a little flower bed border and coloured stairs.
            Item[] dyes = { Items.RED_DYE, Items.YELLOW_DYE, Items.LIME_DYE, Items.LIGHT_BLUE_DYE, Items.MAGENTA_DYE };
            for (int i = 0; i < dyes.length; i++) {
                BlockPos p = at(7 + i, 0, -6);
                level.setBlockAndUpdate(p, CBContent.BLOCK.get().defaultBlockState().setValue(CarpentersBlock.SHAPE, SlabShapeProperty.DOWN));
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof CoverableBlockEntity) {
                    ((CoverableBlockEntity) be).putItem(COVER, new ItemStack(Items.WHITE_CONCRETE));
                    ((CoverableBlockEntity) be).putItem(DYE, new ItemStack(dyes[i]));
                }
            }
            place(level, 7, 0, -4, CBBlocks.STAIRS.get().defaultBlockState().setValue(StairsBlock.SHAPE, SlopeType.WEDGE_POS_N.ordinal()), Items.QUARTZ_BLOCK);
            place(level, 8, 0, -4, CBBlocks.STAIRS.get().defaultBlockState().setValue(StairsBlock.SHAPE, SlopeType.WEDGE_POS_N.ordinal()), Items.QUARTZ_BLOCK);
            slope(level, 10, 0, -4, SlopeType.WEDGE_POS_W, Items.GRASS_BLOCK);
            block(level, 11, 0, -4, Items.GRASS_BLOCK);
            slope(level, 12, 0, -4, SlopeType.WEDGE_POS_E, Items.GRASS_BLOCK);
        }), 80);

        ClientTestHarness.step("hide hud", () -> {
            mc.options.hideGui = true;
            mc.getToasts().clear();
        }, 0);
        look("showcase_front", -7, 5, -17, 3, 2, 1);
        look("showcase_front_right", 16, 5, -16, 6, 2, 0);
        look("showcase_house", -2, 3.5, -8, 3, 2.5, 1);
        look("showcase_door", 3.5, 0.3, -6, 3.5, 1.6, 0);
        look("showcase_roof", 3.5, 9, -7, 3.5, 4, 1.5);
        look("showcase_garage", 11.5, 1, -7, 11.5, 1.5, 0);
        look("showcase_yard", 14, 3, -13, 3, 0.5, -5);
        look("showcase_mound", -1, 1.5, -10, -1, 0.3, -4.5);
        look("showcase_top", 5, 16, -9, 5, 0, -1);
        ClientTestHarness.step("dusk", () -> onServer(server -> command(server, "time set 13200")), 40);
        look("showcase_dusk", -7, 5, -17, 3, 2, 1);
        look("showcase_dusk_door", 0.5, 0.6, -7, 3.5, 1.4, 0);
        ClientTestHarness.step("night", () -> onServer(server -> command(server, "time set 18000")), 40);
        look("showcase_night", 14, 4, -15, 4, 1.5, 0);
    }

    /** A screenshot from a point (relative to the scene origin) looking at another point. */
    private static void look(String name, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX, dy = toY - (fromY + 1.62), dz = toZ - fromZ;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        // The origin is only known once the scenario runs, so the position is worked out in the step.
        ClientTestHarness.step("move for " + name, () -> ClientScenario.teleport(X0 + fromX, Y + fromY, Z0 + fromZ, yaw, pitch), 60);
        ClientTestHarness.step("screenshot " + name, () -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "cbtest-" + name + ".png", mc.getWindow().getWidth(),
                mc.getWindow().getHeight(), mc.getMainRenderTarget(), message -> { });
            ClientTestHarness.record("SHOT  screenshots/cbtest-" + name + ".png");
        }, 10);
    }

    private static BlockPos at(int x, int y, int z) {
        return new BlockPos(X0 + x, Y + y, Z0 + z);
    }

    private static void cover(ServerLevel level, BlockPos pos, Item cover) {
        BlockEntity be = level.getBlockEntity(pos);
        if (cover != null && be instanceof CoverableBlockEntity) {
            ((CoverableBlockEntity) be).putItem(COVER, new ItemStack(cover));
        }
    }

    private static void place(ServerLevel level, int x, int y, int z, BlockState state, Item cover) {
        level.setBlockAndUpdate(at(x, y, z), state);
        cover(level, at(x, y, z), cover);
    }

    private static void block(ServerLevel level, int x, int y, int z, Item cover) {
        place(level, x, y, z, CBContent.BLOCK.get().defaultBlockState(), cover);
    }

    private static void slope(ServerLevel level, int x, int y, int z, SlopeType type, Item cover) {
        place(level, x, y, z, CBContent.SLOPE.get().defaultBlockState().setValue(SlopeBlock.TYPE, SlopeTypeProperty.of(type)), cover);
    }

    private static void door(ServerLevel level, int x, int y, int z, DoorBlock.Style style, Item cover) {
        BlockState lower = CBBlocks.DOOR.get().defaultBlockState().setValue(DoorBlock.STYLE, style)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
        level.setBlock(at(x, y, z), lower, 18);
        level.setBlock(at(x, y + 1, z), lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 18);
        cover(level, at(x, y, z), cover);
        cover(level, at(x, y + 1, z), cover);
    }

    private static void lantern(ServerLevel level, int x, int y, int z, Direction facing) {
        place(level, x, y, z, CBBlocks.TORCH.get().defaultBlockState().setValue(TorchBlock.TYPE, TorchBlock.Type.LANTERN)
            .setValue(TorchBlock.FACING, facing), Items.DARK_OAK_PLANKS);
    }

    private static void torch(ServerLevel level, int x, int y, int z, Direction facing) {
        place(level, x, y, z, CBBlocks.TORCH.get().defaultBlockState().setValue(TorchBlock.FACING, facing), null);
    }
}
