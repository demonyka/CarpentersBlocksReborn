package com.carpentersblocks.interaction;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.block.HammerClick;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.interaction.AttributeRules;
import com.carpentersblocks.core.interaction.HeldItemKind;
import com.carpentersblocks.util.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Player clicks on coverable blocks. Loader hooks call these on both sides: the client uses
 * the result to predict (swing the arm, skip vanilla handling), the server does the work.
 * Changes go through {@link Permissions#canModify}; plain use (doors, buttons) through
 * {@link Permissions#canUse}.
 */
public final class Interactions {
    private Interactions() {
    }

    /** Right-click. Returns {@link InteractionResult#PASS} to let vanilla handle it. */
    public static InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        if (!level.hasChunkAt(pos)) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CoverableBlock)) {
            return InteractionResult.PASS;
        }
        CoverableBlock block = (CoverableBlock) state.getBlock();
        CoverableBlockEntity be = blockEntity(level, pos);
        if (be == null) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        HeldItemKind kind = ItemClassifier.classify(held);
        Direction face = hit.getDirection();
        boolean client = level.isClientSide;

        // Plain use (open, press, toggle): allowed wherever vanilla allows using a door.
        if (kind == HeldItemKind.OTHER) {
            return activate(block, state, player, level, hand, hit);
        }
        if (!Permissions.canModify(player, level, pos)) {
            return activate(block, state, player, level, hand, hit); // adventure mode still opens a door with a block in hand
        }
        if (kind == HeldItemKind.HAMMER) {
            Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            if (!block.onHammer(new HammerClick(state, level, pos, player, face, local, false, client))) {
                return activate(block, state, player, level, hand, hit); // e.g. a lever: the hammer just flips it
            }
            if (!client) {
                damageTool(player, hand, held);
            }
            return InteractionResult.sidedSuccess(client);
        }

        AttributeKey target = AttributeRules.addTarget(kind, player.isShiftKeyDown(), Sides.toCore(face), be,
            location -> block.canCover(state, location));
        if (target == null) {
            // Slot taken: open the door; blocks without a use let vanilla place the held block.
            return activate(block, state, player, level, hand, hit);
        }
        if (!client) {
            be.putItem(target, held);
            if (!player.abilities.instabuild) {
                held.shrink(1); // the stack in the hand that was actually used
            }
            playPlaceSound(level, pos, target.type == AttributeType.COVER ? held : be.getItem(coverFor(target)));
        }
        return InteractionResult.sidedSuccess(client);
    }

    /**
     * Opens, presses or toggles the block. Like vanilla, sneaking with an item in hand never
     * uses a block (the item is used instead, e.g. placed against a door).
     */
    private static InteractionResult activate(CoverableBlock block, BlockState state, Player player, Level level,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean holding = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
        if (player.isShiftKeyDown() && holding) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS; // the main hand already had its turn
        }
        return Permissions.canUse(player, level, hit.getBlockPos())
            ? block.activate(state, level, hit.getBlockPos(), player, hand, hit) : InteractionResult.PASS;
    }

    /**
     * Left-click start. Returns {@code true} if handled, in which case block breaking must be
     * cancelled. With {@code simulate} the world is not changed (client-side prediction).
     */
    public static boolean attack(Player player, Level level, BlockPos pos, Direction face, boolean simulate) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CoverableBlock)) {
            return false;
        }
        CoverableBlock block = (CoverableBlock) state.getBlock();
        CoverableBlockEntity be = blockEntity(level, pos);
        ItemStack held = player.getMainHandItem();
        if (be == null || !player.isAlive() || ItemClassifier.classify(held) != HeldItemKind.HAMMER
            || !Permissions.canModify(player, level, pos)) {
            return false;
        }

        if (player.isShiftKeyDown()) {
            List<AttributeKey> targets = AttributeRules.popTargets(be, Sides.toCore(face));
            if (targets.isEmpty()) {
                return false; // nothing to remove: let the player break the block
            }
            if (!simulate) {
                be.removeAndDrop(targets);
                damageTool(player, InteractionHand.MAIN_HAND, held);
            }
            return true;
        }
        if (block.onHammer(new HammerClick(state, level, pos, player, face, hitPoint(player, pos), true, simulate))) {
            if (!simulate) {
                damageTool(player, InteractionHand.MAIN_HAND, held);
            }
            return true;
        }
        return false;
    }

    /** Where the player is looking on the block (left-click events carry no hit position). */
    private static Vec3 hitPoint(Player player, BlockPos pos) {
        HitResult hit = player.pick(8.0, 1.0F, false);
        if (hit instanceof BlockHitResult && ((BlockHitResult) hit).getBlockPos().equals(pos)) {
            return hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        }
        return new Vec3(0.5, 0.5, 0.5);
    }

    private static CoverableBlockEntity blockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CoverableBlockEntity ? (CoverableBlockEntity) be : null;
    }

    private static AttributeKey coverFor(AttributeKey key) {
        return AttributeKey.of(AttributeType.COVER, key.location.isHost() || key.type == AttributeType.ILLUMINATOR
            ? AttributeLocation.HOST : key.location);
    }

    private static void damageTool(Player player, InteractionHand hand, ItemStack tool) {
        tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
    }

    private static void playPlaceSound(Level level, BlockPos pos, ItemStack source) {
        SoundType sound = source.getItem() instanceof BlockItem
            ? ((BlockItem) source.getItem()).getBlock().defaultBlockState().getSoundType()
            : SoundType.WOOD;
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }
}
