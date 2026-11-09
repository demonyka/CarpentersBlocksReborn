package com.carpentersblocks.block;

import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.core.shape.SlopeTransform;
import com.carpentersblocks.interaction.Permissions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import com.carpentersblocks.core.shape.SlopeType;
import com.carpentersblocks.item.SlopeItem;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.resources.ResourceLocation;
import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Carpenter's Slope: all 65 slope shapes of 1.7.10 in one block. The shape is a block state
 * property, so collision, culling and light are handled by vanilla code. Hammer left-click
 * rotates, right-click steps to the next shape.
 */
public class SlopeBlock extends CoverableBlock {
    public static final EnumProperty<SlopeTypeProperty> TYPE = EnumProperty.create("type", SlopeTypeProperty.class);

    private static final VoxelShape[] SHAPES = new VoxelShape[SlopeType.values().length];

    public SlopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, SlopeTypeProperty.WEDGE_POS_N).setValue(LIGHT, 0).setValue(OPAQUE, false));
    }

    /** State shown by a slope item in inventories. */
    public static BlockState displayState(SlopePlacement.Kind kind) {
        return com.carpentersblocks.registry.CBContent.SLOPE.get().defaultBlockState()
            .setValue(TYPE, SlopeTypeProperty.of(SlopeItem.displayType(kind)));
    }

    public static SlopeType type(BlockState state) {
        return state.getValue(TYPE).core();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE, OPAQUE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SlopeType type = type(state);
        VoxelShape shape = SHAPES[type.ordinal()];
        if (shape == null) {
            shape = Shapes.empty();
            for (Box box : type.collisionBoxes()) {
                shape = Shapes.joinUnoptimized(shape, Sides.toShape(box), net.minecraft.world.phys.shapes.BooleanOp.OR);
            }
            shape = shape.optimize();
            SHAPES[type.ordinal()] = shape;
        }
        return shape;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Item item = context.getItemInHand().getItem();
        SlopePlacement.Kind kind = item instanceof SlopeItem ? ((SlopeItem) item).kind : SlopePlacement.Kind.WEDGE;
        // Position on the clicked face; only the two in-plane coordinates matter.
        Vec3 click = context.getClickLocation();
        double hitX = click.x - Mth.floor(click.x);
        double hitY = click.y - Mth.floor(click.y);
        double hitZ = click.z - Mth.floor(click.z);
        SlopeType type = SlopePlacement.place(kind, Sides.toCore(context.getClickedFace()), hitX, hitY, hitZ,
            Sides.toCore(context.getHorizontalDirection()), context.getRotation());
        // Join with neighbouring slopes unless the player sneaks (1.7.10 behaviour).
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            SlopeTransform.Neighbors neighbors = neighbors(context.getLevel(), context.getClickedPos());
            if (kind == SlopePlacement.Kind.WEDGE) {
                type = SlopeTransform.transformWedge(type, neighbors);
            } else if (kind == SlopePlacement.Kind.PRISM) {
                type = SlopeTransform.transformPrism(type, neighbors);
            }
        }
        return defaultBlockState().setValue(TYPE, SlopeTypeProperty.of(type));
    }

    /** After placement (server side), neighbouring wedges and prisms join the new slope. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player) || placer.isShiftKeyDown()) {
            return;
        }
        Player player = (Player) placer;
        SlopeType placed = type(state);
        if (placed.family == SlopeType.Family.WEDGE) {
            SlopeTransform.adjacentWedges(placed, neighbors(level, pos)).forEach((side, newType) ->
                setNeighbour(level, pos.relative(Sides.toMc(side)), newType, player));
        } else if (placed.family.isPrism() && placed.positive) {
            for (com.carpentersblocks.core.Side side : SlopeTransform.HORIZONTAL) {
                BlockPos at = pos.relative(Sides.toMc(side));
                SlopeType current = slopeAt(level, at);
                if (current != null) {
                    setNeighbour(level, at, SlopeTransform.transformPrism(current, neighbors(level, at)), player);
                }
            }
        }
    }

    private void setNeighbour(Level level, BlockPos at, SlopeType newType, Player player) {
        BlockState neighbour = level.getBlockState(at);
        if (neighbour.getBlock() == this && type(neighbour) != newType && Permissions.canModify(player, level, at)) {
            level.setBlock(at, neighbour.setValue(TYPE, SlopeTypeProperty.of(newType)), BlockFlags.ALL);
        }
    }

    private static SlopeTransform.Neighbors neighbors(LevelReader level, BlockPos pos) {
        return side -> slopeAt(level, pos.relative(Sides.toMc(side)));
    }

    /** Slope shape at a position, or null if it is not a slope (or not loaded). */
    private static SlopeType slopeAt(LevelReader level, BlockPos at) {
        if (!level.hasChunkAt(at)) {
            return null;
        }
        BlockState state = level.getBlockState(at);
        return state.getBlock() instanceof SlopeBlock ? type(state) : null;
    }

    @Override
    public boolean onHammer(HammerClick click) {
        SlopeType type = type(click.state);
        click.set(click.state.setValue(TYPE, SlopeTypeProperty.of(click.leftClick ? type.rotateY() : type.next())));
        return true;
    }

    /** Side covers only on full faces, as in 1.7.10. */
    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return location.isHost() || type(state).isFaceFull(location.side());
    }

    /** 1.7.10: oblique slopes have their own sloped-face textures, prisms the quartered frame. */
    @Override
    public ResourceLocation uncoveredTexture(BlockState state, Polygon polygon) {
        if (polygon.coverSide != null) {
            return null; // block-side faces use the full frame like every other block
        }
        SlopeType type = type(state);
        switch (type.family) {
            case OBLIQUE_INT:
            case OBLIQUE_EXT:
                return type.positive ? UncoveredTextures.OBLIQUE_POS : UncoveredTextures.OBLIQUE_NEG;
            case WEDGE_SIDE:
            case WEDGE:
            case WEDGE_INT:
            case WEDGE_EXT:
                return null;
            default:
                return UncoveredTextures.QUARTERED_FRAME;
        }
    }

    @Override
    public List<Polygon> getRenderPolygons(BlockState state) {
        return type(state).polygons();
    }
}
