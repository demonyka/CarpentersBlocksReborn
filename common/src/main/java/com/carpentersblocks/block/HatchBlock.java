package com.carpentersblocks.block;

import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Orientation;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Vec3d;
import com.carpentersblocks.registry.CBBlocks;
import com.carpentersblocks.registry.CBContent;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Carpenter's Hatch (1.7.10): a trapdoor hinged on the side of its support, in five styles.
 * Wooden hatches open by hand; rigid ("iron") ones only with redstone. Moves to another
 * support when its own disappears.
 */
public class HatchBlock extends BoxedBlock {
    public enum Style implements StringRepresentable {
        HIDDEN, WINDOW, SCREEN, FRENCH_WINDOW, PANEL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);
    /** Direction away from the support (the hinge side is opposite). */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HIGH = BooleanProperty.create("high");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty RIGID = BooleanProperty.create("rigid");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int PANE = 1, HANDLE = 2;

    public HatchBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(STYLE, Style.HIDDEN).setValue(FACING, Direction.NORTH)
            .setValue(HIGH, false).setValue(OPEN, false).setValue(RIGID, false).setValue(POWERED, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STYLE, FACING, HIGH, OPEN, RIGID, POWERED);
    }

    // ---- geometry -----------------------------------------------------------------------

    /**
     * Model frame: an open hatch standing against a support on the south (+z), facing north.
     * Closed hatches are the same panel laid flat, hinge towards the support.
     */
    private static UnaryOperator<Vec3d> placement(BlockState state) {
        int turns = (Orientation.turnsFromNorth(com.carpentersblocks.util.Sides.toCore(state.getValue(FACING)))) % 4;
        if (state.getValue(OPEN)) {
            return p -> Orientation.rotateY(p, turns);
        }
        boolean high = state.getValue(HIGH);
        // Lay the panel down around the hinge edge at the support: thickness (z) becomes height.
        return p -> Orientation.rotateY(high ? new Vec3d(p.x, p.z, 1 - p.y) : new Vec3d(p.x, 1 - p.z, p.y), turns);
    }

    private static double thickness(BlockState state) {
        return state.getValue(STYLE) == Style.HIDDEN ? 2 : 3;
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        List<Part> parts = new ArrayList<>();
        Style style = state.getValue(STYLE);
        double z0 = 16 - thickness(state);
        if (style == Style.HIDDEN) {
            parts.add(Part.of(Box.px(0, 0, z0, 16, 16, 16)));
        } else {
            parts.add(Part.of(Box.px(0, 0, z0, 3, 16, 16)));      // stiles
            parts.add(Part.of(Box.px(13, 0, z0, 16, 16, 16)));
            parts.add(Part.of(Box.px(3, 13, z0, 13, 16, 16)));    // rails
            parts.add(Part.of(Box.px(3, 0, z0, 13, 3, 16)));
            switch (style) {
                case WINDOW:
                case SCREEN:
                    parts.add(new Part(Box.px(3, 3, 14.5, 13, 13, 14.5), PANE));
                    break;
                case FRENCH_WINDOW:
                    parts.add(new Part(Box.px(3, 3, 14.5, 13, 13, 14.5), PANE));
                    parts.add(Part.of(Box.px(3, 7, 14, 13, 9, 15)));
                    parts.add(Part.of(Box.px(7, 3, 14, 9, 7, 15)));
                    parts.add(Part.of(Box.px(7, 9, 14, 9, 13, 15)));
                    break;
                default: // PANEL (1.7.10 had a 0.8215 typo here)
                    parts.add(Part.of(Box.px(3, 3, 14, 13, 13, 15)));
                    parts.add(Part.of(Box.px(5, 5, 13, 11, 11, 16)));
                    break;
            }
        }
        // Iron pull handle on the edge away from the hinge: inside always, outside unless hidden.
        double hy = state.getValue(HIGH) ? 1 : 14;
        for (double side : style == Style.HIDDEN ? new double[] { -1 } : new double[] { -1, 1 }) {
            double face = side < 0 ? z0 : 16, step = side;
            parts.add(new Part(zBox(6, hy, face, 7, hy + 1, face + step), HANDLE));
            parts.add(new Part(zBox(9, hy, face, 10, hy + 1, face + step), HANDLE));
            parts.add(new Part(zBox(6, hy, face + step, 10, hy + 1, face + 2 * step), HANDLE));
        }
        UnaryOperator<Vec3d> place = placement(state);
        List<Part> placed = new ArrayList<>(parts.size());
        for (Part part : parts) {
            placed.add(new Part(Orientation.box(part.box, place), part.part));
        }
        return placed;
    }

    /** Box in pixels whose z range may be given in either order. */
    private static Box zBox(double x1, double y1, double za, double x2, double y2, double zb) {
        return Box.px(x1, y1, Math.min(za, zb), x2, y2, Math.max(za, zb));
    }

    @Override
    protected List<Box> outlineBoxes(BlockState state) {
        return Collections.singletonList(Orientation.box(Box.px(0, 0, 16 - thickness(state), 16, 16, 16), placement(state)));
    }

    @Override
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        if (polygon.part == HANDLE) {
            return UncoveredTextures.IRON;
        }
        if (polygon.part != PANE) {
            return null;
        }
        switch (state.getValue(STYLE)) {
            case SCREEN:        return UncoveredTextures.HATCH_SCREEN;
            case FRENCH_WINDOW: return UncoveredTextures.HATCH_FRENCH;
            default:            return UncoveredTextures.HATCH_GLASS;
        }
    }

    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return location.isHost();
    }

    // ---- support --------------------------------------------------------------------------

    /** 1.7.10 supports: a solid side, any slab or stairs, Carpenter's blocks and stairs, glowstone. */
    private static boolean isSupport(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos at = pos.relative(facing.getOpposite());
        BlockState support = level.getBlockState(at);
        Block block = support.getBlock();
        return support.isFaceSturdy(level, at, facing) || block instanceof SlabBlock || block instanceof StairBlock
            || block == CBContent.BLOCK.get() || block == CBBlocks.STAIRS.get() || block == Blocks.GLOWSTONE;
    }

    /** The next valid support going round clockwise from the current one, or null. */
    private static Direction nextSupport(BlockGetter level, BlockPos pos, Direction facing) {
        Direction dir = facing;
        for (int i = 0; i < 4; i++) {
            dir = dir.getClockWise();
            if (isSupport(level, pos, dir)) {
                return dir;
            }
        }
        return null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal() || !isSupport(context.getLevel(), context.getClickedPos(), face)) {
            return null; // only on the side of a support, as in 1.7.10
        }
        double hitY = context.getClickLocation().y - context.getClickedPos().getY();
        return defaultBlockState().setValue(FACING, face).setValue(HIGH, hitY > 0.5)
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isSupport(level, pos, state.getValue(FACING)) || nextSupport(level, pos, state.getValue(FACING)) != null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        Direction facing = state.getValue(FACING);
        if (isSupport(level, pos, facing)) {
            return state;
        }
        Direction next = nextSupport(level, pos, facing);
        return next == null ? Blocks.AIR.defaultBlockState() : state.setValue(FACING, next);
    }

    // ---- ladder ---------------------------------------------------------------------------

    /** 1.7.10: an open hidden hatch in the upper position can be climbed like a ladder. */
    public static boolean isClimbable(BlockState state) {
        return state.getBlock() instanceof HatchBlock && state.getValue(STYLE) == Style.HIDDEN
            && state.getValue(HIGH) && state.getValue(OPEN);
    }

    /** Forge hook (IForgeBlock#isLadder); on Fabric a mixin calls {@link #isClimbable}. */
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        return isClimbable(state);
    }

    // ---- use ------------------------------------------------------------------------------

    @Override
    public InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(RIGID)) {
            return InteractionResult.PASS; // iron-like: redstone only
        }
        if (!level.isClientSide) {
            toggle(state.cycle(OPEN), level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void toggle(BlockState next, Level level, BlockPos pos) {
        level.setBlock(pos, next, BlockFlags.ALL);
        boolean open = next.getValue(OPEN), iron = next.getValue(RIGID);
        level.playSound(null, pos, open ? (iron ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_OPEN)
            : (iron ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_CLOSE), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** Redstone: opens when powered, closes when the power goes (wooden and rigid). */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState next = state.setValue(POWERED, powered);
            if (powered != state.getValue(OPEN)) {
                toggle(next.setValue(OPEN, powered), level, pos);
            } else {
                level.setBlock(pos, next, BlockFlags.CLIENTS);
            }
        }
    }

    /**
     * Hammer: left-click moves to the next support (never breaks the hatch, unlike 1.7.10);
     * right-click changes the style; sneak + right-click toggles rigid (redstone only).
     */
    @Override
    public boolean onHammer(HammerClick click) {
        if (click.leftClick) {
            Direction next = nextSupport(click.level, click.pos, click.state.getValue(FACING));
            if (next == null) {
                return false;
            }
            click.set(click.state.setValue(FACING, next));
        } else if (click.sneaking) {
            BlockState next = click.state.cycle(RIGID);
            click.set(next);
            click.message(next.getValue(RIGID) ? "message.carpentersblocks.activation_iron" : "message.carpentersblocks.activation_wood");
        } else {
            Style[] styles = Style.values();
            click.set(click.state.setValue(STYLE, styles[(click.state.getValue(STYLE).ordinal() + 1) % styles.length]));
        }
        return true;
    }
}
