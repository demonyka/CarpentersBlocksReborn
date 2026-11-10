package com.carpentersblocks.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side checks every block modification must pass. Vanilla already applies most of
 * these to right-clicks; we repeat them because left-click hooks and our own packets can run
 * before vanilla's checks.
 */
public final class Permissions {
    /** Same limit vanilla uses for block interaction packets (8 blocks). */
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private Permissions() {
    }

    /** Changing the block: covers, hammer. Not in adventure mode. */
    public static boolean canModify(Player player, Level level, BlockPos pos) {
        return player.mayBuild() && canUse(player, level, pos);   // mayBuild: false in adventure mode
    }

    /** Placing an extra block on the player's behalf (the rest of a garage door column). */
    public static boolean canPlace(Player player, Level level, BlockPos pos) {
        return player.mayBuild() && !player.isSpectator() && level.hasChunkAt(pos) && level.mayInteract(player, pos);
    }

    /** Using the block like a vanilla door or button: also allowed in adventure mode. */
    public static boolean canUse(Player player, Level level, BlockPos pos) {
        return !player.isSpectator()
            && level.hasChunkAt(pos)                   // never load chunks on behalf of a client
            && level.mayInteract(player, pos)          // spawn protection and world border
            && player.distanceToSqr(Vec3.atCenterOf(pos)) <= MAX_DISTANCE_SQR;
    }
}
