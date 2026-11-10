package com.carpentersblocks.block;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coverable block made of boxes (buttons, levers, doors, fences...). Subclasses describe the
 * boxes of each state; faces, outline and collision shapes are derived and cached per state.
 */
public abstract class BoxedBlock extends CoverableBlock {
    /** A box of the model plus the part it belongs to (see {@link Polygon#part}). */
    public static final class Part {
        public final Box box;
        public final int part;

        public Part(Box box, int part) {
            this.box = box;
            this.part = part;
        }

        public static Part of(Box box) {
            return new Part(box, 0);
        }
    }

    private final Map<BlockState, List<Polygon>> polygons = new ConcurrentHashMap<>();
    private final Map<BlockState, VoxelShape> outlines = new ConcurrentHashMap<>();
    private final Map<BlockState, VoxelShape> collisions = new ConcurrentHashMap<>();

    protected BoxedBlock(Properties properties) {
        super(properties);
    }

    protected BoxedBlock(Properties properties, java.util.function.ToIntFunction<BlockState> ownLight) {
        super(properties, ownLight);
    }

    /** Uncovered boxed blocks are plain wood (1.7.10 icon_uncovered_solid); the frame's holes do not suit thin parts. */
    @Override
    public net.minecraft.resources.ResourceLocation uncoveredTexture(BlockState state, Polygon polygon) {
        return com.carpentersblocks.util.UncoveredTextures.SOLID;
    }

    /** Thin and small blocks take one cover for the whole block; stairs override this. */
    @Override
    public boolean canCover(BlockState state, com.carpentersblocks.core.attribute.AttributeLocation location) {
        return location.isHost();
    }

    /**
     * Region of a fixed texture a part shows, as {u0, v0, u1, v1} in pixels, or null for the
     * world-aligned default. For parts drawn from a small region of a texture.
     */
    protected float[] fixedUv(BlockState state, Polygon polygon) {
        return null;
    }

    /** Faces that are not axis-aligned boxes (tilted lever handle, wall torch). */
    protected List<Polygon> extraPolygons(BlockState state) {
        return Collections.emptyList();
    }

    /** Boxes drawn for a state. */
    protected abstract List<Part> renderParts(BlockState state);

    /** Boxes for the outline and clicking; defaults to everything drawn. */
    protected List<Box> outlineBoxes(BlockState state) {
        List<Box> boxes = new ArrayList<>();
        for (Part part : renderParts(state)) {
            boxes.add(part.box);
        }
        return boxes;
    }

    /** Boxes entities collide with; defaults to the outline. Empty = walk through. */
    protected List<Box> collisionBoxes(BlockState state) {
        return outlineBoxes(state);
    }

    @Override
    public List<Polygon> getRenderPolygons(BlockState state) {
        return polygons.computeIfAbsent(state, s -> {
            List<Polygon> out = new ArrayList<>();
            for (Part part : renderParts(s)) {
                out.addAll(Polygons.ofBox(part.box, part.part));
            }
            out.addAll(extraPolygons(s));
            for (int i = 0; i < out.size(); i++) {
                float[] uv = fixedUv(s, out.get(i));
                if (uv != null) {
                    out.set(i, Polygons.fitUvs(out.get(i), uv[0], uv[1], uv[2], uv[3]));
                }
            }
            return Collections.unmodifiableList(out);
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return outlines.computeIfAbsent(state, s -> union(outlineBoxes(s)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisions.computeIfAbsent(state, s -> union(collisionBoxes(s)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    protected static VoxelShape union(List<Box> boxes) {
        VoxelShape shape = Shapes.empty();
        for (Box box : boxes) {
            shape = Shapes.joinUnoptimized(shape, Sides.toShape(box), BooleanOp.OR);
        }
        return shape.optimize();
    }
}
