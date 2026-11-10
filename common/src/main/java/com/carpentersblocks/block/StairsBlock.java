package com.carpentersblocks.block;

import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.core.shape.SlopeTransform;
import com.carpentersblocks.core.shape.SlopeType;
import com.carpentersblocks.core.shape.StairsShape;
import com.carpentersblocks.interaction.Permissions;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Carpenter's Stairs: 28 shapes (side, straight, inner and outer corners, up and down).
 * Placement, joining and the hammer work like wedge slopes, as in 1.7.10.
 */
public class StairsBlock extends BoxedBlock {
    public static final IntegerProperty SHAPE = IntegerProperty.create("shape", 0, StairsShape.COUNT - 1);

    public StairsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SHAPE, SlopeType.WEDGE_POS_N.ordinal()).setValue(LIGHT, 0).setValue(OPAQUE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SHAPE, OPAQUE);
    }

    public static SlopeType type(BlockState state) {
        return StairsShape.type(state.getValue(SHAPE));
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        List<Part> parts = new ArrayList<>();
        for (Box box : StairsShape.boxes(state.getValue(SHAPE))) {
            parts.add(Part.of(box));
        }
        return parts;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Vec3 click = context.getClickLocation();
        SlopeType type = SlopePlacement.place(SlopePlacement.Kind.WEDGE, Sides.toCore(context.getClickedFace()),
            click.x - Mth.floor(click.x), click.y - Mth.floor(click.y), click.z - Mth.floor(click.z),
            Sides.toCore(context.getHorizontalDirection()), context.getRotation());
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            type = SlopeTransform.transformWedge(type, neighbors(context.getLevel(), context.getClickedPos()));
        }
        if (!StairsShape.isStairs(type)) {
            type = SlopeType.WEDGE_POS_N;
        }
        return defaultBlockState().setValue(SHAPE, type.ordinal());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player) || placer.isShiftKeyDown()) {
            return;
        }
        Player player = (Player) placer;
        SlopeTransform.adjacentWedges(type(state), neighbors(level, pos)).forEach((side, newType) -> {
            BlockPos at = pos.relative(Sides.toMc(side));
            BlockState neighbour = level.getBlockState(at);
            if (neighbour.getBlock() == this && StairsShape.isStairs(newType) && Permissions.canModify(player, level, at)) {
                level.setBlock(at, neighbour.setValue(SHAPE, newType.ordinal()), BlockFlags.ALL);
            }
        });
    }

    private SlopeTransform.Neighbors neighbors(LevelReader level, BlockPos pos) {
        return side -> {
            BlockPos at = pos.relative(Sides.toMc(side));
            if (!level.hasChunkAt(at)) {
                return null;
            }
            BlockState state = level.getBlockState(at);
            return state.getBlock() == this ? type(state) : null;
        };
    }

    @Override
    public boolean onHammer(HammerClick click) {
        int shape = click.state.getValue(SHAPE);
        click.set(click.state.setValue(SHAPE, click.leftClick ? StairsShape.rotate(shape) : StairsShape.next(shape)));
        return true;
    }

    @Override
    public boolean canCover(BlockState state, AttributeLocation location) {
        return true;
    }

    /** Mobs path over it like a slab or stairs, not through it. */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        int shape = state.getValue(SHAPE);
        for (int i = 0; i < rotation.ordinal(); i++) {
            shape = StairsShape.rotate(shape);
        }
        return state.setValue(SHAPE, shape);
    }

    /** 1.7.10: stairs show the quartered frame, like the Carpenter's Block. */
    @Override
    public net.minecraft.resources.ResourceLocation uncoveredTexture(BlockState state, com.carpentersblocks.core.geom.Polygon polygon) {
        return com.carpentersblocks.util.UncoveredTextures.QUARTERED_FRAME;
    }
}
