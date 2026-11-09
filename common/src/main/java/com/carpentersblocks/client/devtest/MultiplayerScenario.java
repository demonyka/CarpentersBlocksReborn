package com.carpentersblocks.client.devtest;

import com.carpentersblocks.block.CarpentersBlock;
import com.carpentersblocks.block.DoorBlock;
import com.carpentersblocks.block.LeverBlock;
import com.carpentersblocks.block.SlabShapeProperty;
import com.carpentersblocks.block.SlopeBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Client half of the multiplayer test ({@code tools/mp-test.sh}). Both testers join a real
 * dedicated server over the network. Tester A changes blocks with real clicks and checks it
 * sees the server's answer; tester B joins afterwards and checks the saved, synced result
 * (chunk data with block entities, block states).
 */
final class MultiplayerScenario {
    /** Must match {@code MultiplayerTest.ORIGIN} on the server. */
    private static final BlockPos ORIGIN = new BlockPos(0, 4, 0);
    private static final BlockPos COVERED = ORIGIN, HAMMERED = ORIGIN.east(2), DOOR = ORIGIN.east(4),
        LEVER = ORIGIN.east(6), SLOPE = ORIGIN.east(8);

    private MultiplayerScenario() {
    }

    static void build(String role) {
        Minecraft mc = Minecraft.getInstance();
        ClientTestHarness.step("connect", () -> {
            mc.options.pauseOnLostFocus = false;
            mc.setScreen(new ConnectScreen(new TitleScreen(), mc, "localhost", 25565));
        }, 0);
        ClientTestHarness.waitUntil("joined the server", () -> mc.player != null && mc.level != null && mc.screen == null, 3600, 100);
        ClientTestHarness.waitUntil("scene loaded", () -> mc.level.getBlockState(COVERED).getBlock() == CBContent.BLOCK.get(), 1200, 20);

        if (role.equals("a")) {
            give(mc, Items.STONE);
            ClientTestHarness.step("cover a block", () -> useOn(mc, COVERED, Direction.UP), 10);
            give(mc, CBContent.HAMMER.get());
            ClientTestHarness.step("hammer a block", () -> useOn(mc, HAMMERED, Direction.UP), 10);
            give(mc, Items.STICK);
            ClientTestHarness.step("open the door", () -> useOn(mc, DOOR, Direction.NORTH), 10);
            ClientTestHarness.step("pull the lever", () -> useOn(mc, LEVER, Direction.UP), 10);
            give(mc, CBContent.slopeItem(SlopePlacement.Kind.WEDGE));
            ClientTestHarness.step("place a slope", () -> useOn(mc, SLOPE.below(), Direction.UP), 40);
        } else {
            // Give the server time to stream the chunks with their block entities.
            ClientTestHarness.waitUntil("changes from tester A visible", () -> allVisible(mc.level), 600, 20);
        }

        String who = "tester " + role.toUpperCase() + " sees ";
        ClientTestHarness.step("check", () -> {
            ClientLevel level = mc.level;
            ClientTestHarness.check(who + "the stone cover", covered(level));
            ClientTestHarness.check(who + "the hammered slab", hammered(level));
            ClientTestHarness.check(who + "the open door (both halves)", doorOpen(level));
            ClientTestHarness.check(who + "the lever on", leverOn(level));
            ClientTestHarness.check(who + "the placed slope", level.getBlockState(SLOPE).getBlock() instanceof SlopeBlock);
        }, 0);
        ClientTestHarness.step("hide hud", () -> mc.options.hideGui = true, 20);
        ClientTestHarness.step("screenshot", () -> Screenshot.grab(mc.gameDirectory, "cbtest-mp-" + role + ".png",
            mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.getMainRenderTarget(), message -> { }), 10);
    }

    private static boolean allVisible(ClientLevel level) {
        return covered(level) && hammered(level) && doorOpen(level) && leverOn(level)
            && level.getBlockState(SLOPE).getBlock() instanceof SlopeBlock;
    }

    private static boolean covered(ClientLevel level) {
        BlockEntity be = level.getBlockEntity(COVERED);
        return be instanceof CoverableBlockEntity && ((CoverableBlockEntity) be).getRenderData().cover(AttributeLocation.HOST) != null;
    }

    private static boolean hammered(ClientLevel level) {
        BlockState state = level.getBlockState(HAMMERED);
        return state.getBlock() instanceof CarpentersBlock && state.getValue(CarpentersBlock.SHAPE) != SlabShapeProperty.FULL;
    }

    private static boolean doorOpen(ClientLevel level) {
        BlockState lower = level.getBlockState(DOOR), upper = level.getBlockState(DOOR.above());
        return is(lower, s -> s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.OPEN))
            && is(upper, s -> s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.OPEN));
    }

    private static boolean leverOn(ClientLevel level) {
        return is(level.getBlockState(LEVER), s -> s.getBlock() instanceof LeverBlock && s.getValue(LeverBlock.POWERED));
    }

    private static boolean is(BlockState state, Predicate<BlockState> test) {
        return test.test(state);
    }

    /** Creative players may set their own hotbar; the server accepts it like the creative menu. */
    private static void give(Minecraft mc, Item item) {
        ClientTestHarness.step("take " + item, () -> {
            ItemStack stack = new ItemStack(item, 16);
            mc.player.inventory.selected = 0;
            mc.player.inventory.setItem(0, stack);
            mc.gameMode.handleCreativeModeItemAdd(stack, 36);
        }, 10);
    }

    private static void useOn(Minecraft mc, BlockPos pos, Direction face) {
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        mc.gameMode.useItemOn(mc.player, mc.level, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }
}
