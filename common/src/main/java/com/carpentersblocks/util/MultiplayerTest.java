package com.carpentersblocks.util;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.registry.CBBlocks;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server half of the multiplayer test ({@code tools/mp-test.sh}), enabled with
 * {@code -Dcarpentersblocks.mpTest=true} on a real dedicated server. Builds a small scene,
 * puts joining test players next to it in creative mode, and stops the server when the
 * second tester leaves. The clients do the clicking and checking.
 */
public final class MultiplayerTest {
    /** Scene origin on a flat world (ground at y = 3, blocks at y = 4). */
    public static final BlockPos ORIGIN = new BlockPos(0, 4, 0);
    public static final String LAST_PLAYER = "CBTesterB";

    private MultiplayerTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("carpentersblocks.mpTest");
    }

    public static void onServerStarted(MinecraftServer server) {
        if (!enabled()) {
            return;
        }
        ServerLevel level = server.overworld();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setDayTime(6000);
        for (int x = -2; x <= 12; x++) {
            for (int z = -6; z <= 4; z++) {
                level.setBlockAndUpdate(ORIGIN.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 4; y++) {
                    level.setBlockAndUpdate(ORIGIN.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Targets for the first tester: a block to cover, a block to hammer, a door, a lever.
        level.setBlockAndUpdate(ORIGIN, CBContent.BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(ORIGIN.east(2), CBContent.BLOCK.get().defaultBlockState());
        BlockState door = CBBlocks.DOOR.get().defaultBlockState();
        level.setBlockAndUpdate(ORIGIN.east(4), door);
        door.getBlock().setPlacedBy(level, ORIGIN.east(4), door, null, ItemStack.EMPTY);
        level.setBlockAndUpdate(ORIGIN.east(6), CBBlocks.LEVER.get().defaultBlockState());
        CarpentersBlocks.LOGGER.info("[MultiplayerTest] scene ready at {}", ORIGIN);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!enabled()) {
            return;
        }
        player.setGameMode(GameType.CREATIVE);
        player.teleportTo(player.getLevel(), ORIGIN.getX() + 4.5, ORIGIN.getY(), ORIGIN.getZ() - 3.5, 0, 25);
        CarpentersBlocks.LOGGER.info("[MultiplayerTest] {} joined", player.getGameProfile().getName());
    }

    public static void onPlayerLeave(ServerPlayer player) {
        if (!enabled()) {
            return;
        }
        String name = player.getGameProfile().getName();
        CarpentersBlocks.LOGGER.info("[MultiplayerTest] {} left", name);
        if (LAST_PLAYER.equals(name)) {
            MinecraftServer server = player.getServer();
            server.execute(() -> server.halt(false));  // the leave event may come from a network thread
        }
    }
}
