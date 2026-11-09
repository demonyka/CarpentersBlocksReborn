package com.carpentersblocks.block;

import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Carpenter's Ladder (1.7.10): on a wall or free-standing, three styles (classic, iron rail,
 * pole). Neighbouring ladders of the same placement join (shared rails disappear).
 * Climbable through the {@code minecraft:climbable} tag.
 */
public class LadderBlock extends BoxedBlock {
    /** Wall ladders face away from their wall; free ones stand in a plane along x or z. */
    public enum Placement implements StringRepresentable {
        NORTH, SOUTH, WEST, EAST, FREE_X, FREE_Z;

        boolean free() {
            return this == FREE_X || this == FREE_Z;
        }

        Direction facing() {
            switch (this) {
                case NORTH: return Direction.NORTH;
                case SOUTH: return Direction.SOUTH;
                case WEST:  return Direction.WEST;
                case EAST:  return Direction.EAST;
                default:    return null;
            }
        }

        static Placement of(Direction facing) {
            return valueOf(facing.name());
        }

        /** Quarter turns from the design frame (wall to the north, rungs along x). */
        int turns() {
            switch (this) {
                case SOUTH:  return 0;
                case WEST:   return 1;
                case NORTH:  return 2;
                case EAST:   return 3;
                case FREE_Z: return 1;
                default:     return 0;
            }
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Style implements StringRepresentable {
        CLASSIC, RAIL, POLE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Placement> PLACEMENT = EnumProperty.create("placement", Placement.class);
    public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);
    /** A ladder of the same placement on the low-x (design frame) side / high-x side. */
    public static final BooleanProperty JOIN_LEFT = BooleanProperty.create("join_left");
    public static final BooleanProperty JOIN_RIGHT = BooleanProperty.create("join_right");
    private static final int IRON = 1;

    public LadderBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(PLACEMENT, Placement.SOUTH).setValue(STYLE, Style.CLASSIC)
            .setValue(JOIN_LEFT, false).setValue(JOIN_RIGHT, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLACEMENT, STYLE, JOIN_LEFT, JOIN_RIGHT);
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        Placement placement = state.getValue(PLACEMENT);
        boolean free = placement.free();
        boolean left = state.getValue(JOIN_LEFT), right = state.getValue(JOIN_RIGHT);
        // Design frame: wall at z = 0 (north) for wall ladders; centred for free ones.
        double z0 = free ? 6 : 0;   // pixels
        List<Part> parts = new ArrayList<>();
        switch (state.getValue(STYLE)) {
            case CLASSIC: {
                double railZ1 = free ? 10 : 3, rungZ0 = free ? 7 : 2, rungZ1 = free ? 9 : 3;
                if (!left) {
                    parts.add(Part.of(Box.px(0, 0, z0, 2, 16, railZ1)));
                }
                if (!right) {
                    parts.add(Part.of(Box.px(14, 0, z0, 16, 16, railZ1)));
                }
                for (int y = 2; y < 16; y += 4) {
                    parts.add(Part.of(Box.px(left ? 0 : 2, y, rungZ0, right ? 16 : 14, y + 1, rungZ1)));
                }
                break;
            }
            case RAIL: {
                double c = free ? 6 : 0;
                parts.add(new Part(Box.px(2, 0, c + 1, 4, 16, c + 2), IRON));
                parts.add(new Part(Box.px(3, 0, c + 2, 4, 16, c + 3), IRON));
                parts.add(new Part(Box.px(12, 0, c + 1, 14, 16, c + 2), IRON));
                parts.add(new Part(Box.px(12, 0, c + 2, 13, 16, c + 3), IRON));
                for (int y = 1; y < 16; y += 4) {
                    parts.add(Part.of(Box.px(0, y, c, 16, y + 2, c + 1)));
                }
                break;
            }
            default: { // POLE
                if (free) {
                    parts.add(Part.of(Box.px(6, 0, 6, 10, 16, 10)));
                    parts.add(Part.of(Box.px(7, 2, 10, 9, 3, 15)));
                    parts.add(Part.of(Box.px(7, 3, 14, 9, 4, 15)));
                    parts.add(Part.of(Box.px(7, 12, 1, 9, 13, 6)));
                    parts.add(Part.of(Box.px(7, 13, 1, 9, 14, 2)));
                } else {
                    for (int y = 2; y < 16; y += 4) {
                        parts.add(Part.of(Box.px(2, y, 0, 3, y + 1, 2)));
                        parts.add(Part.of(Box.px(13, y, 0, 14, y + 1, 2)));
                        parts.add(Part.of(Box.px(2, y, 2, 14, y + 1, 3)));
                    }
                }
                break;
            }
        }
        List<Part> turned = new ArrayList<>(parts.size());
        for (Part part : parts) {
            turned.add(new Part(part.box.rotateY(placement.turns()), part.part));
        }
        return turned;
    }

    @Override
    protected List<Box> outlineBoxes(BlockState state) {
        Placement placement = state.getValue(PLACEMENT);
        Box box = placement.free() ? Box.px(0, 0, 6, 16, 16, 10) : Box.px(0, 0, 0, 16, 16, 3);
        return Collections.singletonList(box.rotateY(placement.turns()));
    }

    @Override
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        return polygon.part == IRON ? UncoveredTextures.IRON : null;
    }

    // ---- placement and support ------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Placement placement = face.getAxis().isHorizontal()
            ? Placement.of(face)
            // Clicked a floor or ceiling: stand free, facing the player.
            : context.getHorizontalDirection().getAxis() == Direction.Axis.X ? Placement.FREE_Z : Placement.FREE_X;
        BlockState state = defaultBlockState().setValue(PLACEMENT, placement);
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            // Continue the ladder above or below (below wins, as in 1.7.10), if it can stand here.
            for (BlockPos other : new BlockPos[] { context.getClickedPos().above(), context.getClickedPos().below() }) {
                BlockState ladder = context.getLevel().getBlockState(other);
                if (ladder.getBlock() == this) {
                    BlockState copy = state.setValue(PLACEMENT, ladder.getValue(PLACEMENT)).setValue(STYLE, ladder.getValue(STYLE));
                    if (canSurvive(copy, context.getLevel(), context.getClickedPos())) {
                        state = copy;
                    }
                }
            }
        }
        if (!canSurvive(state, context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return joins(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(PLACEMENT).facing();
        if (facing == null) {
            return true; // free-standing ladders never fall
        }
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return joins(state, level, pos);
    }

    /** Joins with ladders of the same placement to the left and right (in the design frame). */
    private BlockState joins(BlockState state, LevelReader level, BlockPos pos) {
        Placement placement = state.getValue(PLACEMENT);
        // Design-frame -x / +x turned like the model.
        Direction minus = turn(Direction.WEST, placement.turns()), plus = turn(Direction.EAST, placement.turns());
        return state.setValue(JOIN_LEFT, same(level, pos.relative(minus), placement))
            .setValue(JOIN_RIGHT, same(level, pos.relative(plus), placement));
    }

    private boolean same(LevelReader level, BlockPos at, Placement placement) {
        BlockState other = level.getBlockState(at);
        return other.getBlock() == this && other.getValue(PLACEMENT) == placement;
    }

    private static Direction turn(Direction dir, int quarterTurns) {
        for (int i = 0; i < quarterTurns; i++) {
            dir = dir.getClockWise();
        }
        return dir;
    }

    /** Hammer: left-click next style, right-click previous. */
    @Override
    public boolean onHammer(HammerClick click) {
        Style[] styles = Style.values();
        int step = click.leftClick ? 1 : styles.length - 1;
        click.set(click.state.setValue(STYLE, styles[(click.state.getValue(STYLE).ordinal() + step) % styles.length]));
        return true;
    }

    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return location.isHost();
    }
}
