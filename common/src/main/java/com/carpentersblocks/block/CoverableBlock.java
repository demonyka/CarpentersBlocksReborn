package com.carpentersblocks.block;

import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.Collections;
import java.util.List;

/**
 * Base for every block that accepts covers, dyes, overlays and so on.
 * <p>
 * Shape-like state (slab side, slope type, open/closed) lives in the {@link BlockState} so that
 * collision, light and culling work through vanilla code. Decorations live in the
 * {@link CoverableBlockEntity}.
 * <p>
 * Right-clicks reach {@code Interactions} through {@link #use}, after other mods' interaction
 * events (claims, protection) had their say. Vanilla skips {@code use} when the player sneaks
 * with an item, so each loader also routes that case here (Forge event, Fabric mixin).
 */
public abstract class CoverableBlock extends Block implements EntityBlock {
    /** Light emitted by the block, derived from its cover and illuminator. */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    /**
     * Whether the host cover is an opaque full block (stone, planks...). Only then do neighbours
     * hide their faces against this block and does it block light: an uncovered frame, glass or
     * leaves must let the faces behind show through. Added by blocks that can be full.
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty OPAQUE =
        net.minecraft.world.level.block.state.properties.BooleanProperty.create("opaque");

    protected CoverableBlock(Properties properties) {
        this(properties, state -> 0);
    }

    /** @param ownLight light the block itself gives (e.g. a lit torch); the cover/illuminator light adds on top as a maximum */
    protected CoverableBlock(Properties properties, java.util.function.ToIntFunction<BlockState> ownLight) {
        super(properties.lightLevel(state -> Math.max(state.getValue(LIGHT), ownLight.applyAsInt(state))));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }

    /** Neighbours cull against this block only when it has an opaque cover (see {@link #OPAQUE}). */
    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.hasProperty(OPAQUE) && state.getValue(OPAQUE)
            ? super.getOcclusionShape(state, level, pos) : net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                                     net.minecraft.world.InteractionHand hand,
                                                     net.minecraft.world.phys.BlockHitResult hit) {
        return com.carpentersblocks.interaction.Interactions.use(player, level, hand, hit);
    }

    /** Turns facing-like properties; blocks with their own orientation encoding override this. */
    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return Rotations.rotate(state, rotation);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return Rotations.mirror(state, mirror);
    }

    @Override
    public BlockEntity newBlockEntity(BlockGetter level) {
        return Platform.createCoverableBlockEntity();
    }

    private static final List<Polygon> FULL_CUBE = Collections.unmodifiableList(Polygons.ofBox(Box.FULL));

    /** Faces the renderer covers with textures. Core types only, so safe on servers. */
    public List<Polygon> getRenderPolygons(BlockState state) {
        return FULL_CUBE;
    }

    /**
     * Faces for a state plus the block entity snapshot, for blocks whose shape is stored in the
     * block entity (collapsible block). Defaults to {@link #getRenderPolygons(BlockState)}.
     */
    public List<Polygon> getRenderPolygons(BlockState state, com.carpentersblocks.blockentity.RenderData data) {
        return getRenderPolygons(state);
    }

    /** Faces of the item in inventories, for a display state; defaults to the block's own shape. */
    public List<Polygon> getItemPolygons(BlockState displayState) {
        return getRenderPolygons(displayState);
    }

    /**
     * Plain right-click with an empty hand or an item that is not a tool, cover, dye or
     * illuminator: open a door, press a button... Called on both sides. Return PASS to let
     * vanilla handle the click.
     */
    public net.minecraft.world.InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player,
                                                         net.minecraft.world.InteractionHand hand,
                                                         net.minecraft.world.phys.BlockHitResult hit) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    /**
     * Texture of a face when it has no cover, or null for the block model's particle texture
     * (the uncovered frame).
     */
    public ResourceLocation uncoveredTexture(BlockState state, Polygon polygon) {
        return null;
    }

    /**
     * Fixed texture for a face that never shows a cover (e.g. the flame of a torch), or null
     * for normal cover rendering. Textures returned here must be listed in
     * {@code UncoveredTextures#ALL} so they are in the block atlas.
     */
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        return null;
    }

    /**
     * Whether the selection outline should follow the drawn faces instead of the click shape
     * (blocks whose shape is only a boxy approximation of the model: slopes).
     */
    public boolean hasModelOutline(BlockState state) {
        return false;
    }

    /** Whether a cover may be placed at this location. */
    public boolean canCover(BlockState state, AttributeLocation location) {
        return true;
    }

    /**
     * Hammer click: any right-click, or a left-click without sneaking (sneak + left-click
     * removes attributes). Called on both sides; change the world only through
     * {@link HammerClick#set} or when {@code click.simulate} is false.
     *
     * @return whether the click did something (and so should not fall through to vanilla)
     */
    public abstract boolean onHammer(HammerClick click);

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !isMoving) {
            // Covers, dyes, etc. drop however the block disappears: player, explosion, /setblock.
            // Not when a mod moves the block together with its block entity.
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CoverableBlockEntity) {
                ((CoverableBlockEntity) blockEntity).dropAttributes();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
