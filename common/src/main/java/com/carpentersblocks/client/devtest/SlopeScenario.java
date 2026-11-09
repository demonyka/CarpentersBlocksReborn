package com.carpentersblocks.client.devtest;

import com.carpentersblocks.block.SlopeBlock;
import com.carpentersblocks.block.SlopeTypeProperty;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.core.shape.SlopeType;
import com.carpentersblocks.item.SlopeItem;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import static com.carpentersblocks.client.devtest.ClientScenario.attack;
import static com.carpentersblocks.client.devtest.ClientScenario.command;
import static com.carpentersblocks.client.devtest.ClientScenario.give;
import static com.carpentersblocks.client.devtest.ClientScenario.onServer;
import static com.carpentersblocks.client.devtest.ClientScenario.shot;
import static com.carpentersblocks.client.devtest.ClientScenario.teleport;
import static com.carpentersblocks.client.devtest.ClientScenario.useOn;

/**
 * Slope part of the automated client run, on its own platform east of the main scene.
 * Rows (z): 0 uncovered, 3 grass, 6 stone bricks; z = 10 a small brick roof;
 * item frames with the five slope items on a wall at z = 15; placement tests at z = 18.
 */
final class SlopeScenario {
    private static final int X0 = 40;
    private static final int Y = 200;
    private static final AttributeKey COVER = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);

    /** One of each family, oriented to face the camera (north) where possible. */
    private static final SlopeType[] SHOWCASE = {
        SlopeType.WEDGE_POS_N, SlopeType.WEDGE_NEG_N, SlopeType.WEDGE_NE, SlopeType.WEDGE_INT_POS_NW,
        SlopeType.WEDGE_EXT_POS_NE, SlopeType.OBL_INT_POS_NW, SlopeType.OBL_EXT_POS_NE, SlopeType.PRISM_POS,
        SlopeType.PRISM_2P_POS_WE, SlopeType.PRISM_4P_POS, SlopeType.PRISM_WEDGE_POS_N,
    };

    private SlopeScenario() {
    }

    static void build() {
        Minecraft mc = Minecraft.getInstance();
        ClientTestHarness.step("fly to slope area", () -> teleport(X0 + 0.5, Y + 3, -6, 0, 30), 100);
        ClientTestHarness.waitUntil("slope area loaded",
            () -> mc.level.hasChunkAt(new BlockPos(X0 + 12, Y, 20)) && mc.level.hasChunkAt(new BlockPos(X0 - 12, Y, -8)), 1200, 20);

        ClientTestHarness.step("build slope scene", () -> onServer(server -> {
            command(server, "time set 6000");
            command(server, "fill " + (X0 - 12) + " " + (Y - 1) + " -8 " + (X0 + 12) + " " + (Y - 1) + " 20 minecraft:smooth_stone");
            ServerLevel level = server.overworld();
            for (int i = 0; i < SHOWCASE.length; i++) {
                int x = X0 - 10 + i * 2;
                slope(level, x, 0, SHOWCASE[i], null);
                slope(level, x, 3, SHOWCASE[i], Items.GRASS_BLOCK);
                slope(level, x, 6, SHOWCASE[i], Items.STONE_BRICKS);
            }
            // A small hipped roof of bricks: textures must continue across blocks.
            for (int x = -2; x <= 2; x++) {
                slope(level, X0 + x, 9, x == -2 ? SlopeType.WEDGE_EXT_POS_NW : x == 2 ? SlopeType.WEDGE_EXT_POS_NE : SlopeType.WEDGE_POS_N, Items.BRICKS);
                slope(level, X0 + x, 11, x == -2 ? SlopeType.WEDGE_EXT_POS_SW : x == 2 ? SlopeType.WEDGE_EXT_POS_SE : SlopeType.WEDGE_POS_S, Items.BRICKS);
            }
            slope(level, X0 - 2, 10, SlopeType.WEDGE_POS_W, Items.BRICKS);
            slope(level, X0 + 2, 10, SlopeType.WEDGE_POS_E, Items.BRICKS);
            for (int x = -1; x <= 1; x++) {
                level.setBlockAndUpdate(new BlockPos(X0 + x, Y, 10), net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState());
            }
            // Item frames with the five slope items, on a stone wall facing the camera (north).
            command(server, "fill " + (X0 - 5) + " " + Y + " 15 " + (X0 + 5) + " " + (Y + 1) + " 15 minecraft:stone");
            int fx = X0 - 4;
            for (SlopePlacement.Kind kind : SlopePlacement.Kind.values()) {
                command(server, "summon minecraft:item_frame " + fx + " " + (Y + 1) + " 14 {Facing:2b,Fixed:1b,Item:{id:\"carpentersblocks:"
                    + SlopeItem.idOf(kind) + "\",Count:1b}}");
                fx += 2;
            }
        }), 20);

        // ---- placement and hammer through the real client input path ----------------------
        BlockPos placeOn = new BlockPos(X0, Y - 1, 18);
        BlockPos placed = placeOn.above();
        ClientTestHarness.step("stand south of the test spot, looking north", () -> teleport(X0 + 0.5, Y, 20.5, 180, 40), 40);
        give(CBContent.slopeItem(SlopePlacement.Kind.WEDGE));
        ClientTestHarness.step("place wedge on the floor", () -> useOn(placeOn, Direction.UP), 10);
        ClientTestHarness.step("check placed wedge", () -> onServer(server -> ClientTestHarness.check(
            "wedge placed on top while looking north rises away from the player (WEDGE_POS_S)",
            type(server.overworld(), placed) == SlopeType.WEDGE_POS_S)), 0);

        give(CBContent.HAMMER.get());
        ClientTestHarness.step("hammer right-click", () -> useOn(placed, Direction.UP), 10);
        ClientTestHarness.step("check next shape", () -> onServer(server -> ClientTestHarness.check(
            "hammer right-click steps to the next shape (WEDGE_POS_S -> WEDGE_NEG_S)",
            type(server.overworld(), placed) == SlopeType.WEDGE_NEG_S)), 0);
        ClientTestHarness.step("hammer left-click", () -> attack(placed, Direction.UP), 10);
        ClientTestHarness.step("check rotation", () -> onServer(server -> ClientTestHarness.check(
            "hammer left-click rotates and does not break the slope (WEDGE_NEG_S -> WEDGE_NEG_W)",
            type(server.overworld(), placed) == SlopeType.WEDGE_NEG_W)), 0);

        give(Items.STONE);
        ClientTestHarness.step("cover the slope", () -> useOn(placed, Direction.NORTH), 10);
        ClientTestHarness.step("check slope cover", () -> onServer(server -> {
            BlockEntity be = server.overworld().getBlockEntity(placed);
            ClientTestHarness.check("right-click with stone covers a slope",
                be instanceof CoverableBlockEntity && ((CoverableBlockEntity) be).has(COVER));
        }), 0);

        // ---- automatic joining (1.7.10 SlopeTransform) ---------------------------------------
        BlockPos cornerAt = new BlockPos(X0 + 4, Y, 18);
        BlockPos neighbourAt = new BlockPos(X0 + 7, Y, 17);
        BlockPos joinerAt = new BlockPos(X0 + 7, Y, 18);
        BlockPos sneakAt = new BlockPos(X0 + 10, Y, 18);
        BlockPos prismA = new BlockPos(X0 - 4, Y, 18);
        BlockPos prismB = new BlockPos(X0 - 5, Y, 18);
        ClientTestHarness.step("prepare neighbours", () -> onServer(server -> {
            ServerLevel level = server.overworld();
            slope(level, X0 + 4, 17, SlopeType.WEDGE_POS_E, null);
            slope(level, X0 + 7, 17, SlopeType.WEDGE_POS_N, null);
            slope(level, X0 + 10, 17, SlopeType.WEDGE_POS_E, null);
            slope(level, X0 - 5, 18, SlopeType.PRISM_POS, null);
        }), 10);

        give(CBContent.slopeItem(SlopePlacement.Kind.WEDGE));
        ClientTestHarness.step("look south", () -> teleport(X0 + 4.5, Y, 20.5, 0, 50), 30);
        ClientTestHarness.step("place wedge next to a wedge", () -> useOn(cornerAt.below(), Direction.UP), 10);
        ClientTestHarness.step("check corner", () -> onServer(server -> ClientTestHarness.check(
            "wedge next to a wedge becomes an interior corner (WEDGE_INT_POS_NE)",
            type(server.overworld(), cornerAt) == SlopeType.WEDGE_INT_POS_NE)), 0);

        ClientTestHarness.step("look east", () -> teleport(X0 + 5.5, Y, 18.5, -90, 50), 30);
        ClientTestHarness.step("place wedge beside a wedge", () -> useOn(joinerAt.below(), Direction.UP), 10);
        ClientTestHarness.step("check neighbour changed", () -> onServer(server -> {
            ClientTestHarness.check("placed wedge faces west (WEDGE_POS_W)", type(server.overworld(), joinerAt) == SlopeType.WEDGE_POS_W);
            ClientTestHarness.check("neighbouring wedge becomes an exterior corner (WEDGE_EXT_POS_NW)",
                type(server.overworld(), neighbourAt) == SlopeType.WEDGE_EXT_POS_NW);
        }), 0);

        ClientTestHarness.step("look south again", () -> teleport(X0 + 10.5, Y, 20.5, 0, 50), 30);
        ClientTestHarness.step("sneak", () -> mc.options.keyShift.setDown(true), 5);
        ClientTestHarness.step("sneak-place wedge", () -> useOn(sneakAt.below(), Direction.UP), 10);
        ClientTestHarness.step("unsneak", () -> mc.options.keyShift.setDown(false), 5);
        ClientTestHarness.step("check sneak placement", () -> onServer(server -> ClientTestHarness.check(
            "sneaking places a plain wedge without joining (WEDGE_POS_N)",
            type(server.overworld(), sneakAt) == SlopeType.WEDGE_POS_N)), 0);

        give(CBContent.slopeItem(SlopePlacement.Kind.PRISM));
        ClientTestHarness.step("stand by the prisms", () -> teleport(X0 - 3.5, Y, 20.5, 0, 50), 30);
        ClientTestHarness.step("place prism next to a prism", () -> useOn(prismA.below(), Direction.UP), 10);
        ClientTestHarness.step("check prism ridges", () -> onServer(server -> {
            ClientTestHarness.check("new prism grows a ridge towards its neighbour (PRISM_1P_POS_W)",
                type(server.overworld(), prismA) == SlopeType.PRISM_1P_POS_W);
            ClientTestHarness.check("neighbouring prism grows a ridge back (PRISM_1P_POS_E)",
                type(server.overworld(), prismB) == SlopeType.PRISM_1P_POS_E);
        }), 0);

        // ---- sneak + mouse wheel switches the slope kind ------------------------------------
        give(CBContent.slopeItem(SlopePlacement.Kind.WEDGE));
        ClientTestHarness.step("sneak", () -> mc.options.keyShift.setDown(true), 5);
        ClientTestHarness.step("scroll", () -> ClientTestHarness.check("scroll with a slope while sneaking is consumed",
            com.carpentersblocks.client.SlopeScrollHandler.onScroll(1.0)), 10);
        ClientTestHarness.step("unsneak", () -> mc.options.keyShift.setDown(false), 5);
        ClientTestHarness.step("check switched item", () -> onServer(server -> {
            ItemStack held = server.getPlayerList().getPlayers().get(0).getMainHandItem();
            ClientTestHarness.check("wheel switches wedge to oblique interior, keeping the count (" + held + ")",
                held.getItem() == CBContent.slopeItem(SlopePlacement.Kind.OBLIQUE_INTERIOR) && held.getCount() == 16);
        }), 0);

        // ---- screenshots ------------------------------------------------------------------
        shot("slopes_overview", X0 + 0.5, Y + 6, -7, 0, 35);
        shot("slopes_left", X0 - 6, Y + 2.5, -3.5, 0, 30);
        shot("slopes_right", X0 + 6, Y + 2.5, -3.5, 0, 30);
        shot("slopes_from_south", X0 + 0.5, Y + 4, 14, 180, 40);
        shot("slopes_roof", X0 + 0.5, Y + 3, 6.5, 0, 35);
        shot("slopes_items", X0 + 0.5, Y + 1.5, 11.5, 0, 0);
        shot("slopes_joined", X0 + 3.5, Y + 3, 21.5, 180, 45);
    }

    private static void slope(ServerLevel level, int x, int z, SlopeType type, Item cover) {
        BlockPos pos = new BlockPos(x, Y, z);
        level.setBlockAndUpdate(pos, CBContent.SLOPE.get().defaultBlockState().setValue(SlopeBlock.TYPE, SlopeTypeProperty.of(type)));
        BlockEntity be = level.getBlockEntity(pos);
        if (cover != null && be instanceof CoverableBlockEntity) {
            ((CoverableBlockEntity) be).putItem(COVER, new ItemStack(cover));
        }
    }

    private static SlopeType type(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof SlopeBlock ? SlopeBlock.type(level.getBlockState(pos)) : null;
    }
}
