package com.carpentersblocks.client.devtest;

import com.carpentersblocks.block.BarrierBlock;
import com.carpentersblocks.block.CarpentersBlock;
import com.carpentersblocks.block.DoorBlock;
import com.carpentersblocks.block.LeverBlock;
import com.carpentersblocks.block.SlopeBlock;
import com.carpentersblocks.block.TorchBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.registry.CBBlocks;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static com.carpentersblocks.client.devtest.ClientScenario.command;
import static com.carpentersblocks.client.devtest.ClientScenario.onServer;
import static com.carpentersblocks.client.devtest.ClientScenario.shot;
import static com.carpentersblocks.client.devtest.ClientScenario.teleport;

/**
 * Close-ups of what a player looks at first: item icons in the hotbar, doors closed and open
 * with each hinge beside vanilla doors, levers, torches beside vanilla torches, connected
 * barriers, the selection outline of a slope, and the particles of a broken block.
 * Run alone with {@code -Dcarpentersblocks.clientTest=look}.
 */
final class LookScenario {
    private static final int X0 = 160, Y = 200, Z = 6;
    private static final AttributeKey COVER = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);

    private LookScenario() {
    }

    static void build() {
        Minecraft mc = Minecraft.getInstance();
        ClientTestHarness.step("fly to look area", () -> teleport(X0 + 10, Y + 4, -6, 0, 30), 100);
        ClientTestHarness.step("build look scene", () -> onServer(server -> {
            ServerLevel level = server.overworld();
            command(server, "time set 6000");
            command(server, "fill " + (X0 - 6) + " " + (Y - 1) + " -8 " + (X0 + 40) + " " + (Y - 1) + " 12 minecraft:smooth_stone");
            command(server, "fill " + (X0 - 6) + " " + Y + " -8 " + (X0 + 40) + " " + (Y + 6) + " 12 minecraft:air");

            // Barriers: a straight run, a corner, and a gate between two barriers.
            for (int x = 0; x <= 2; x++) {
                place(level, x, 0, 0, CBBlocks.BARRIER.get().defaultBlockState());
            }
            place(level, 2, 0, -1, CBBlocks.BARRIER.get().defaultBlockState());
            place(level, 4, 0, 0, CBBlocks.BARRIER.get().defaultBlockState());
            place(level, 5, 0, 0, CBBlocks.GATE.get().defaultBlockState().setValue(com.carpentersblocks.block.GateBlock.ALONG_X, true));
            place(level, 6, 0, 0, CBBlocks.BARRIER.get().defaultBlockState());
            for (int x = 0; x <= 6; x++) { // let them connect
                BlockPos at = at(x, 0, 0);
                level.getBlockState(at).updateNeighbourShapes(level, at, 3);
                level.blockUpdated(at, level.getBlockState(at).getBlock());
            }

            // Doors facing the camera (placed by a player looking south): ours, then vanilla.
            int x = 9;
            for (boolean vanilla : new boolean[] { false, true }) {
                for (DoorHingeSide hinge : DoorHingeSide.values()) {
                    for (boolean open : new boolean[] { false, true }) {
                        BlockState lower = (vanilla ? Blocks.OAK_DOOR : CBBlocks.DOOR.get()).defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                            .setValue(BlockStateProperties.DOOR_HINGE, hinge).setValue(BlockStateProperties.OPEN, open);
                        level.setBlock(at(x, 0, 0), lower, 18);
                        level.setBlock(at(x, 1, 0), lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 18);
                        x += 2;
                    }
                }
            }

            // Levers on the floor and a wall, torches beside vanilla torches.
            command(server, "fill " + (X0 + 26) + " " + Y + " " + (Z + 1) + " " + (X0 + 36) + " " + (Y + 2) + " " + (Z + 1) + " minecraft:stone");
            BlockState lever = CBBlocks.LEVER.get().defaultBlockState();
            place(level, 26, 0, 0, lever);
            place(level, 27, 0, 0, lever.setValue(LeverBlock.POWERED, true));
            place(level, 28, 1, 0, lever.setValue(LeverBlock.FACING, Direction.NORTH));
            place(level, 29, 1, 0, lever.setValue(LeverBlock.FACING, Direction.NORTH).setValue(LeverBlock.POWERED, true));
            level.setBlockAndUpdate(at(30, 1, 0), Blocks.LEVER.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            BlockState torch = CBBlocks.TORCH.get().defaultBlockState();
            place(level, 32, 1, 0, torch.setValue(TorchBlock.FACING, Direction.NORTH));
            level.setBlockAndUpdate(at(33, 1, 0), Blocks.WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            place(level, 34, 0, -2, torch);
            level.setBlockAndUpdate(at(35, 0, -2), Blocks.TORCH.defaultBlockState());

            // For the outline and particle checks: a covered slope, a covered block, plain stone.
            covered(level, 0, 0, -6, SlopeBlock.displayState(SlopePlacement.Kind.WEDGE), Items.OAK_PLANKS);
            covered(level, 3, 0, -6, CBContent.BLOCK.get().defaultBlockState(), Items.STONE);
            place(level, 7, 0, -6, CBContent.BLOCK.get().defaultBlockState());
            level.setBlockAndUpdate(at(5, 0, -6), Blocks.STONE.defaultBlockState());

            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            Item[] hotbar = {
                CBContent.slopeItem(SlopePlacement.Kind.WEDGE), CBContent.slopeItem(SlopePlacement.Kind.OBLIQUE_INTERIOR),
                CBContent.slopeItem(SlopePlacement.Kind.OBLIQUE_EXTERIOR), CBContent.slopeItem(SlopePlacement.Kind.PRISM),
                CBContent.slopeItem(SlopePlacement.Kind.PRISM_WEDGE), CBBlocks.BARRIER.get().asItem(), CBBlocks.GATE.get().asItem(),
                CBBlocks.DOOR.get().asItem(), CBBlocks.TORCH.get().asItem(),
            };
            for (int i = 0; i < hotbar.length; i++) {
                player.inventory.setItem(i, new ItemStack(hotbar[i]));
            }
        }), 60);

        ClientTestHarness.step("show hud, hold the door", () -> {
            mc.options.hideGui = false;
            mc.player.inventory.selected = 7;
        }, 10);
        shot("look_hotbar_door_in_hand", X0 + 3, Y + 1.0, Z - 3.5, 0, 15);
        ClientTestHarness.step("hold the barrier", () -> mc.player.inventory.selected = 5, 10);
        shot("look_barrier_in_hand", X0 + 3, Y + 1.0, Z - 3.5, 0, 15);
        ClientTestHarness.step("hold the gate", () -> mc.player.inventory.selected = 6, 10);
        shot("look_gate_in_hand", X0 + 5.5, Y + 0.6, Z - 2.5, 0, 15);

        // Selection outline and tooltip: aim at each kind of block and check the crosshair finds it.
        aim("slope", at(0, 0, -6), 0.4);
        aim("covered_block", at(3, 0, -6), 0.5);
        aim("bare_block", at(7, 0, -6), 0.5);
        aim("barrier", at(1, 0, 0), 0.6);
        aim("gate", at(5, 0, 0), 0.6);
        aim("door", at(9, 0, 0), 0.8);
        aim("lever", at(26, 0, 0), 0.1);
        aim("torch", at(34, 0, -2), 0.3);

        // Particles: breaking a covered block must throw as many as breaking stone.
        int[] counts = new int[3];
        ClientTestHarness.step("hide hud", () -> mc.options.hideGui = true, 0);
        ClientTestHarness.step("stand by the blocks", () -> teleport(X0 + 4.5, Y, Z - 8.5, 0, 10), 40);
        ClientTestHarness.step("count particles", () -> counts[0] = particles(mc), 0);
        ClientTestHarness.step("break stone", () -> ClientScenario.attack(at(5, 0, -6), Direction.NORTH), 3);
        ClientTestHarness.step("count stone particles", () -> counts[1] = particles(mc) - counts[0], 60);
        ClientTestHarness.step("count again", () -> counts[0] = particles(mc), 0);
        ClientTestHarness.step("break covered block", () -> ClientScenario.attack(at(3, 0, -6), Direction.NORTH), 3);
        ClientTestHarness.step("compare particles", () -> {
            counts[2] = particles(mc) - counts[0];
            ClientTestHarness.check("a covered block breaks into about as many particles as stone (stone " + counts[1]
                + ", covered " + counts[2] + ")", counts[2] <= counts[1] * 1.5 + 8);
        }, 0);

        // The same by mining in survival: hit particles every tick, then the break.
        int[] mined = new int[2];
        ClientTestHarness.step("survival", () -> onServer(server -> {
            command(server, "gamemode survival @a");
            server.overworld().setBlockAndUpdate(at(5, 0, -6), Blocks.OAK_PLANKS.defaultBlockState());
            covered(server.overworld(), 3, 0, -6, CBContent.BLOCK.get().defaultBlockState(), Items.OAK_PLANKS);
        }), 20);
        mine("planks", at(5, 0, -6), mined, 0);
        mine("covered block", at(3, 0, -6), mined, 1);
        ClientTestHarness.step("compare mining particles", () -> {
            ClientTestHarness.check("mining a covered block throws about as many particles as mining planks (planks "
                + mined[0] + ", covered " + mined[1] + ")", mined[1] <= mined[0] * 1.5 + 8);
            onServer(server -> command(server, "gamemode creative @a"));
        }, 20);

        shot("look_barriers", X0 + 3, Y + 1.8, Z - 4, 0, 20);
        shot("look_doors_ours", X0 + 12, Y + 1.2, Z - 5, 0, 5);
        shot("look_doors_vanilla", X0 + 20, Y + 1.2, Z - 5, 0, 5);
        shot("look_doors_ours_back", X0 + 12, Y + 1.2, Z + 6, 180, 5);
        shot("look_doors_ours_top", X0 + 12.5, Y + 5, Z - 1.5, 0, 70);
        shot("look_doors_vanilla_top", X0 + 20.5, Y + 5, Z - 1.5, 0, 70);
        shot("look_levers_close", X0 + 27, Y, Z - 0.9, 0, 58);
        shot("look_levers_close_side", X0 + 25.2, Y, Z + 0.5, -90, 50);
        shot("look_levers", X0 + 28.5, Y + 1.3, Z - 2.2, 0, 15);
        shot("look_torch_wall_close_left", X0 + 33.4, Y + 0.1, Z - 0.6, 55, 5);
        shot("look_torch_wall_close_right", X0 + 31.6, Y + 0.1, Z - 0.6, -55, 5);
        shot("look_torch_wall_close_top", X0 + 32.5, Y + 0.9, Z - 0.3, 0, 60);
        shot("look_torches_wall", X0 + 33, Y + 1.2, Z - 1.6, 0, 0);
        shot("look_torches_wall_side", X0 + 31.2, Y + 1.5, Z + 0.4, -90, 5);
        shot("look_torches_floor", X0 + 35, Y + 0.9, Z - 4, 0, 15);
    }

    /** Stands two blocks north-east of the target, looks at a point {@code height} up its middle, and checks the hit. */
    private static void aim(String name, BlockPos target, double height) {
        Minecraft mc = Minecraft.getInstance();
        double standX = target.getX() + 1.9, standZ = target.getZ() - 1.9, eyeY = Y + 1.62;
        double dx = target.getX() + 0.5 - standX, dz = target.getZ() + 0.5 - standZ, dy = target.getY() + height - eyeY;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        shot("look_aim_" + name, standX, Y, standZ, yaw, pitch);
        ClientTestHarness.step("crosshair on " + name, () -> {
            HitResult hit = mc.hitResult;
            ClientTestHarness.check("the crosshair finds the " + name,
                hit instanceof BlockHitResult && ((BlockHitResult) hit).getBlockPos().equals(target));
        }, 0);
    }

    /** Mines a block the way holding the attack button does, and records the particle count when it breaks. */
    private static void mine(String name, BlockPos pos, int[] out, int index) {
        Minecraft mc = Minecraft.getInstance();
        ClientTestHarness.step("wait for old particles", () -> { }, 80);
        ClientTestHarness.step("start mining " + name, () -> mc.gameMode.startDestroyBlock(pos, Direction.NORTH), 0);
        ClientTestHarness.waitUntil("mine " + name, () -> {
            if (mc.level.getBlockState(pos).isAir()) {
                return true;
            }
            if (mc.gameMode.continueDestroyBlock(pos, Direction.NORTH)) {
                mc.particleEngine.crack(pos, Direction.NORTH);
            }
            return false;
        }, 400, 2);
        ClientTestHarness.step("count " + name, () -> out[index] = particles(mc), 0);
    }

    private static int particles(Minecraft mc) {
        try {
            return Integer.parseInt(mc.particleEngine.countParticles());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static BlockPos at(int x, int y, int z) {
        return new BlockPos(X0 + x, Y + y, Z + z);
    }

    private static void place(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlockAndUpdate(at(x, y, z), state);
    }

    private static void covered(ServerLevel level, int x, int y, int z, BlockState state, Item cover) {
        level.setBlockAndUpdate(at(x, y, z), state);
        BlockEntity be = level.getBlockEntity(at(x, y, z));
        if (be instanceof CoverableBlockEntity) {
            ((CoverableBlockEntity) be).putItem(COVER, new ItemStack(cover));
        }
    }
}
