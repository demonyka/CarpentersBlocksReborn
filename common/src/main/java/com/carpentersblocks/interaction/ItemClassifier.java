package com.carpentersblocks.interaction;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.core.interaction.HeldItemKind;
import com.carpentersblocks.item.HammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decides what a held item means to a Carpenter's block. Configurable through item tags
 * ({@code data/carpentersblocks/tags/items/*.json}) so server owners can adjust it without code.
 */
public final class ItemClassifier {
    /** Items that light the block up when sneak-right-clicked (glowstone dust). */
    public static final ResourceLocation ILLUMINATORS = CarpentersBlocks.id("illuminators");
    /** Items that add an overlay (grass, snow, hay, ...). */
    public static final ResourceLocation OVERLAYS = CarpentersBlocks.id("overlays");
    /** Block items accepted as covers even if they are not full cubes (slabs, glass, ...). */
    public static final ResourceLocation COVER_WHITELIST = CarpentersBlocks.id("cover_whitelist");
    /** Block items never accepted as covers. */
    public static final ResourceLocation COVER_BLACKLIST = CarpentersBlocks.id("cover_blacklist");

    private ItemClassifier() {
    }

    public static HeldItemKind classify(ItemStack stack) {
        if (stack.isEmpty()) {
            return HeldItemKind.OTHER;
        }
        Item item = stack.getItem();
        if (item instanceof HammerItem) {
            return HeldItemKind.HAMMER;
        }
        if (inTag(ILLUMINATORS, item)) {
            return HeldItemKind.ILLUMINATOR;
        }
        if (inTag(OVERLAYS, item)) {
            return HeldItemKind.OVERLAY;
        }
        if (item instanceof DyeItem) {
            return HeldItemKind.DYE;
        }
        if (isCover(item)) {
            return HeldItemKind.COVER;
        }
        return HeldItemKind.OTHER;
    }

    /** A cover must be a plain block: full cube, normal model, no block entity, not one of ours. */
    public static boolean isCover(Item item) {
        if (!(item instanceof BlockItem) || inTag(COVER_BLACKLIST, item)) {
            return false;
        }
        Block block = ((BlockItem) item).getBlock();
        if (block instanceof CoverableBlock || block instanceof EntityBlock) {
            return false;
        }
        if (inTag(COVER_WHITELIST, item)) {
            return true;
        }
        BlockState state = block.defaultBlockState();
        return state.getRenderShape() == RenderShape.MODEL
            && Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    private static boolean inTag(ResourceLocation tag, Item item) {
        // Looked up at use time: tags are reloadable and are synced to clients by the server.
        return ItemTags.getAllTags().getTagOrEmpty(tag).contains(item);
    }
}
