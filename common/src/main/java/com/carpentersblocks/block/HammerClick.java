package com.carpentersblocks.block;

import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A hammer click on a coverable block (not counting sneak + left-click, which removes attributes). */
public final class HammerClick {
    public final BlockState state;
    public final Level level;
    public final BlockPos pos;
    public final Player player;
    public final Direction face;
    /** Hit point relative to the block (0..1 per axis). */
    public final Vec3 hit;
    public final boolean leftClick;
    public final boolean sneaking;
    /** Client-side prediction: decide whether the click is handled, but change nothing. */
    public final boolean simulate;

    public HammerClick(BlockState state, Level level, BlockPos pos, Player player, Direction face, Vec3 hit,
                       boolean leftClick, boolean simulate) {
        this.state = state;
        this.level = level;
        this.pos = pos;
        this.player = player;
        this.face = face;
        this.hit = hit;
        this.leftClick = leftClick;
        this.sneaking = player.isShiftKeyDown();
        this.simulate = simulate;
    }

    public com.carpentersblocks.core.Side side() {
        return Sides.toCore(face);
    }

    /** Replaces the clicked block's state (server side only). */
    public void set(BlockState newState) {
        if (!simulate) {
            level.setBlock(pos, newState, com.carpentersblocks.util.BlockFlags.ALL);
        }
    }

    /** Status line above the hotbar, like 1.7.10 chat messages for mode changes. */
    public void message(String key) {
        if (!simulate) {
            player.displayClientMessage(new TranslatableComponent(key), true);
        }
    }
}
