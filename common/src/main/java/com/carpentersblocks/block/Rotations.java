package com.carpentersblocks.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Rotation and mirroring of the common orientation properties (structure blocks, world edit
 * tools): facing, fence-like side connections and "along x" axes.
 */
final class Rotations {
    private static final BooleanProperty ALONG_X = GateBlock.ALONG_X;

    private Rotations() {
    }

    static BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return state;
        }
        BlockState out = state;
        for (DirectionProperty facing : new DirectionProperty[] { BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING }) {
            if (state.hasProperty(facing)) {
                out = out.setValue(facing, rotation.rotate(state.getValue(facing)));
            }
        }
        if (state.hasProperty(BlockStateProperties.NORTH) && state.hasProperty(BlockStateProperties.EAST)
            && state.hasProperty(BlockStateProperties.SOUTH) && state.hasProperty(BlockStateProperties.WEST)) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                out = out.setValue(sideProperty(rotation.rotate(side)), state.getValue(sideProperty(side)));
            }
        }
        if (state.hasProperty(ALONG_X) && rotation != Rotation.CLOCKWISE_180) {
            out = out.cycle(ALONG_X);
        }
        return out;
    }

    static BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        BlockState out = state;
        for (DirectionProperty facing : new DirectionProperty[] { BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING }) {
            if (state.hasProperty(facing)) {
                out = out.setValue(facing, mirror.mirror(state.getValue(facing)));
            }
        }
        if (state.hasProperty(BlockStateProperties.NORTH) && state.hasProperty(BlockStateProperties.EAST)
            && state.hasProperty(BlockStateProperties.SOUTH) && state.hasProperty(BlockStateProperties.WEST)) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                out = out.setValue(sideProperty(mirror.mirror(side)), state.getValue(sideProperty(side)));
            }
        }
        if (state.hasProperty(BlockStateProperties.DOOR_HINGE)) {
            out = out.setValue(BlockStateProperties.DOOR_HINGE,
                state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        }
        return out;
    }

    private static BooleanProperty sideProperty(Direction side) {
        switch (side) {
            case NORTH: return BlockStateProperties.NORTH;
            case EAST:  return BlockStateProperties.EAST;
            case SOUTH: return BlockStateProperties.SOUTH;
            default:    return BlockStateProperties.WEST;
        }
    }
}
