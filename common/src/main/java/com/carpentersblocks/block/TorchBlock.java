package com.carpentersblocks.block;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Orientation;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.geom.Vec3d;
import com.carpentersblocks.util.BlockFlags;
import com.carpentersblocks.util.Sides;
import com.carpentersblocks.util.UncoveredTextures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Carpenter's Torch (1.7.10): a torch or a lantern on the floor or a wall. Vanilla-style
 * torches go out in the rain (lit, smouldering, out) and come back when it stops; right-click
 * with a torch relights them.
 */
public class TorchBlock extends AttachedBlock {
    public enum Type implements StringRepresentable {
        TORCH, LANTERN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Flame implements StringRepresentable {
        LIT(14), SMOLDERING(9), UNLIT(0);

        final int light;

        Flame(int light) {
            this.light = light;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);
    public static final EnumProperty<Flame> FLAME = EnumProperty.create("flame", Flame.class);
    private static final int HEAD = 1, GLASS = 2, LANTERN_FLAME = 3;

    public TorchBlock(Properties properties) {
        // 1.7.10 let a lit torch ignore its cover's light; here the brighter of the two wins.
        super(properties.noCollission(), state -> state.getValue(FLAME).light);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(TYPE, Type.TORCH)
            .setValue(FLAME, Flame.LIT).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE, FLAME);
    }

    /** Floor and walls only. */
    @Override
    protected boolean canFace(Direction facing) {
        return facing != Direction.DOWN;
    }

    private static boolean onWall(BlockState state) {
        return state.getValue(FACING).getAxis() != Direction.Axis.Y;
    }

    @Override
    protected List<Part> renderParts(BlockState state) {
        if (state.getValue(TYPE) == Type.TORCH) {
            return Collections.emptyList(); // drawn in extraPolygons (tilted on walls)
        }
        List<Part> parts = new ArrayList<>();
        boolean wall = onWall(state);
        int turns = wall ? Orientation.turnsFromNorth(Sides.toCore(state.getValue(FACING))) + 2 : 0;
        if (wall) {
            // Arm from the wall (designed against the north wall, then turned).
            parts.add(Part.of(Box.px(7, 6, 0, 9, 12, 4).rotateY(turns)));
            parts.add(Part.of(Box.px(6, 6, 4, 10, 12, 6).rotateY(turns)));
            parts.add(Part.of(Box.px(6, 4, 6, 10, 6, 10)));
        } else {
            parts.add(Part.of(Box.px(6, 0, 6, 10, 6, 10)));
        }
        for (int corner = 0; corner < 4; corner++) {
            parts.add(Part.of(Box.px(4, 4, 4, 6, 12, 6).rotateY(corner)));        // posts
            parts.add(Part.of(Box.px(4, 4, 6, 6, 6, 10).rotateY(corner)));        // bottom bars
            boolean wallSide = wall && corner == turns % 4;  // the north side, turned like the arm
            if (!wallSide) {
                parts.add(new Part(Box.px(6, 6, 5, 10, 12, 5).rotateY(corner), GLASS));
            }
        }
        parts.add(Part.of(Box.px(3, 12, 3, 13, 14, 13)));                         // roof
        parts.add(Part.of(Box.px(5, 14, 5, 11, 15, 11)));                         // cap
        parts.add(new Part(Box.px(7, 6, 7, 9, 8, 9), LANTERN_FLAME));
        return parts;
    }

    @Override
    protected List<Polygon> extraPolygons(BlockState state) {
        if (state.getValue(TYPE) != Type.TORCH) {
            return Collections.emptyList();
        }
        List<Polygon> torch = new ArrayList<>();
        torch.addAll(Polygons.ofBox(Box.px(7, 0, 7, 9, 8, 9), 0));   // handle takes the cover
        torch.addAll(Polygons.ofBox(Box.px(7, 8, 7, 9, 10, 9), HEAD));
        if (!onWall(state)) {
            return torch;
        }
        // Wall torch: stands 3 pixels up near the wall and leans 22 degrees away from it,
        // designed against the north wall then turned to the real one.
        int turns = Orientation.turnsFromNorth(Sides.toCore(state.getValue(FACING))) + 2;
        Vec3d base = new Vec3d(0.5, 0, 0.5);
        return Orientation.polygons(torch, p -> {
            Vec3d q = p.sub(base);
            double rad = Math.toRadians(22), cos = Math.cos(rad), sin = Math.sin(rad);
            Vec3d tilted = new Vec3d(q.x, q.y * cos - q.z * sin, q.y * sin + q.z * cos);
            Vec3d moved = tilted.add(new Vec3d(0.5, 3.5 / 16, 1.5 / 16));
            return Orientation.rotateY(moved, turns);
        });
    }

    @Override
    protected List<Box> outlineBoxes(BlockState state) {
        if (state.getValue(TYPE) == Type.LANTERN) {
            return Collections.singletonList(onWall(state)
                ? Box.px(3, 4, 0, 13, 15, 13).rotateY(Orientation.turnsFromNorth(Sides.toCore(state.getValue(FACING))) + 2)
                : Box.px(3, 0, 3, 13, 15, 13));
        }
        return Collections.singletonList(onWall(state)
            ? Box.px(5.5, 3, 0, 10.5, 13, 5).rotateY(Orientation.turnsFromNorth(Sides.toCore(state.getValue(FACING))) + 2)
            : Box.px(6, 0, 6, 10, 10, 10));
    }

    @Override
    protected List<Box> collisionBoxes(BlockState state) {
        return state.getValue(TYPE) == Type.LANTERN ? outlineBoxes(state) : Collections.emptyList();
    }

    @Override
    public ResourceLocation fixedTexture(BlockState state, Polygon polygon) {
        switch (polygon.part) {
            case HEAD:
                switch (state.getValue(FLAME)) {
                    case LIT:        return UncoveredTextures.TORCH_HEAD_LIT;
                    case SMOLDERING: return UncoveredTextures.TORCH_HEAD_SMOLDERING;
                    default:         return UncoveredTextures.TORCH_HEAD_UNLIT;
                }
            case GLASS:         return UncoveredTextures.LANTERN_GLASS;
            case LANTERN_FLAME: return state.getValue(FLAME) == Flame.UNLIT ? UncoveredTextures.TORCH_HEAD_UNLIT : UncoveredTextures.TORCH_HEAD_LIT;
            default:            return null;
        }
    }

    // ---- weather (1.7.10, issue #299: torches go out in the rain and relight after) -----

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(TYPE) == Type.TORCH;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        Flame flame = state.getValue(FLAME), next = nextFlame(flame, level.isRainingAt(pos));
        if (next != flame) {
            level.setBlock(pos, state.setValue(FLAME, next), BlockFlags.ALL);
            if (next.ordinal() > flame.ordinal()) {
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                    2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
            }
        }
    }

    /** One random tick of weather: rain puts the flame out step by step, dry weather brings it back. */
    public static Flame nextFlame(Flame flame, boolean wet) {
        switch (flame) {
            case LIT:        return wet ? Flame.SMOLDERING : Flame.LIT;
            case SMOLDERING: return wet ? Flame.UNLIT : Flame.LIT;
            default:         return wet ? Flame.UNLIT : Flame.SMOLDERING;
        }
    }

    /** Right-click with a torch relights it (the torch in hand is not used up, as in 1.7.10). */
    @Override
    public InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Item held = player.getItemInHand(hand).getItem();
        boolean isTorch = held == Items.TORCH || held == Items.SOUL_TORCH || held == asItem();
        if (!isTorch || state.getValue(FLAME) == Flame.LIT) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(FLAME, Flame.LIT), BlockFlags.ALL);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        Flame flame = state.getValue(FLAME);
        if (flame == Flame.UNLIT) {
            return;
        }
        Vec3d head;
        if (state.getValue(TYPE) == Type.LANTERN) {
            head = new Vec3d(0.5, 0.5625, 0.5);
        } else if (onWall(state)) {
            Side away = Sides.toCore(state.getValue(FACING));
            head = new Vec3d(0.5 + away.dx * 0.27, 0.92, 0.5 + away.dz * 0.27);
        } else {
            head = new Vec3d(0.5, 0.7, 0.5);
        }
        double x = pos.getX() + head.x, y = pos.getY() + head.y, z = pos.getZ() + head.z;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        if (flame == Flame.LIT) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }

    /** Hammer: either click switches torch and lantern. */
    @Override
    public boolean onHammer(HammerClick click) {
        click.set(click.state.cycle(TYPE));
        return true;
    }
}
