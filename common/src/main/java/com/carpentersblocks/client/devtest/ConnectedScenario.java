package com.carpentersblocks.client.devtest;

import com.carpentersblocks.block.CarpentersBlock;
import com.carpentersblocks.block.SlabShapeProperty;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import static com.carpentersblocks.client.devtest.ClientScenario.command;
import static com.carpentersblocks.client.devtest.ClientScenario.onServer;
import static com.carpentersblocks.client.devtest.ClientScenario.shot;
import static com.carpentersblocks.client.devtest.ClientScenario.teleport;

/**
 * Covers next to each other and next to real blocks, east of the main scene: what connected
 * textures (OptiFine, the CTM mod), translucent covers and hidden-face culling have to get
 * right. Nothing here is asserted; the screenshots are compared by eye.
 * <ul>
 * <li>x 0..2: Carpenter's blocks covered with glass, x 3..5: real glass, one wall;</li>
 * <li>x 8..10: white stained glass covers (translucent), x 11: a real stained glass column;</li>
 * <li>x 14..17: bookshelf and sandstone covers in rows;</li>
 * <li>x 20..23: bare frames and glass covers beside slabs (their faces must stay visible);</li>
 * <li>x 26..: blocks named in {@code -Dcarpentersblocks.ctmBlocks=mod:a,mod:b}, each as a
 *     3x3 wall of covers with a real column beside it.</li>
 * </ul>
 */
final class ConnectedScenario {
    private static final int X0 = 80, Y = 200, Z = 6;
    private static final AttributeKey COVER = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);

    private ConnectedScenario() {
    }

    static void build() {
        ClientTestHarness.step("fly to connected area", () -> teleport(X0 + 10, Y + 4, -6, 0, 30), 100);
        ClientTestHarness.step("build connected scene", () -> onServer(server -> {
            ServerLevel level = server.overworld();
            command(server, "time set 6000");
            command(server, "fill " + (X0 - 6) + " " + (Y - 1) + " -8 " + (X0 + 60) + " " + (Y - 1) + " 12 minecraft:smooth_stone");
            command(server, "fill " + (X0 - 6) + " " + Y + " -8 " + (X0 + 60) + " " + (Y + 6) + " 12 minecraft:air");
            // A brick backdrop: what must be visible through glass.
            command(server, "fill " + (X0 - 2) + " " + Y + " " + (Z + 3) + " " + (X0 + 60) + " " + (Y + 3) + " " + (Z + 3) + " minecraft:bricks");

            wall(level, 0, Items.GLASS);
            fill(level, 3, 5, Blocks.GLASS);
            wall(level, 8, Items.WHITE_STAINED_GLASS);
            fill(level, 11, 11, Blocks.WHITE_STAINED_GLASS);
            for (int y = 0; y < 2; y++) {
                for (int x = 14; x < 18; x++) {
                    covered(level, x, y, SlabShapeProperty.FULL, y == 0 ? Items.BOOKSHELF : Items.SANDSTONE);
                }
            }
            // Culling: a bare frame and a glass-covered block, each between a slab and a stair.
            covered(level, 21, 0, SlabShapeProperty.FULL, null);
            level.setBlockAndUpdate(at(20, 0), Blocks.STONE_SLAB.defaultBlockState());
            level.setBlockAndUpdate(at(22, 0), Blocks.OAK_STAIRS.defaultBlockState());
            covered(level, 21, 2, SlabShapeProperty.FULL, Items.GLASS);
            level.setBlockAndUpdate(at(20, 2), Blocks.STONE_SLAB.defaultBlockState());
            level.setBlockAndUpdate(at(22, 2), Blocks.STONE.defaultBlockState());
            covered(level, 23, 0, SlabShapeProperty.DOWN, Items.GLASS);
            covered(level, 24, 0, SlabShapeProperty.FULL, Items.GLASS);

            int x = 26;
            for (String id : System.getProperty("carpentersblocks.ctmBlocks", "").split(",")) {
                ResourceLocation key = ResourceLocation.tryParse(id.trim());
                Block block = key == null || id.trim().isEmpty() ? Blocks.AIR : Registry.BLOCK.get(key);
                if (block == Blocks.AIR) {
                    if (!id.trim().isEmpty()) {
                        ClientTestHarness.record("NOTE  no such block for the connected scene: " + id);
                    }
                    continue;
                }
                wall(level, x, block.asItem());
                fill(level, x + 3, x + 3, block);
                x += 5;
            }
        }), 60);

        shot("connected_glass", X0 + 3, Y + 1.6, Z - 5.5, 0, 5);
        shot("connected_glass_angle", X0 - 3, Y + 2.5, Z - 3.5, -50, 15);
        shot("connected_stained", X0 + 10, Y + 1.6, Z - 5.5, 0, 5);
        shot("connected_rows", X0 + 16, Y + 1.2, Z - 4.5, 0, 5);
        shot("connected_culling", X0 + 22.5, Y + 2, Z - 4.5, 0, 15);
        shot("connected_culling_back", X0 + 22.5, Y + 2, Z + 2.5, 180, 15);
        shot("connected_mod_blocks", X0 + 33, Y + 1.6, Z - 8, 0, 5);
        shot("connected_mod_blocks_near", X0 + 29, Y + 1.6, Z - 4.5, 0, 5);
    }

    private static BlockPos at(int x, int y) {
        return new BlockPos(X0 + x, Y + y, Z);
    }

    /** A 3x3 wall of covered Carpenter's blocks starting at column {@code x}. */
    private static void wall(ServerLevel level, int x, Item cover) {
        for (int dx = 0; dx < 3; dx++) {
            for (int y = 0; y < 3; y++) {
                covered(level, x + dx, y, SlabShapeProperty.FULL, cover);
            }
        }
    }

    private static void fill(ServerLevel level, int x0, int x1, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = 0; y < 3; y++) {
                level.setBlockAndUpdate(at(x, y), block.defaultBlockState());
            }
        }
    }

    private static void covered(ServerLevel level, int x, int y, SlabShapeProperty shape, Item cover) {
        BlockPos pos = at(x, y);
        level.setBlockAndUpdate(pos, CBContent.BLOCK.get().defaultBlockState().setValue(CarpentersBlock.SHAPE, shape));
        BlockEntity be = level.getBlockEntity(pos);
        if (cover != null && be instanceof CoverableBlockEntity) {
            ((CoverableBlockEntity) be).putItem(COVER, new ItemStack(cover));
        }
    }
}
