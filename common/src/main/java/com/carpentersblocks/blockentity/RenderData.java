package com.carpentersblocks.blockentity;

import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Map;

/**
 * Immutable snapshot of what a coverable block looks like. Built on the main thread whenever
 * attributes change and read by chunk-building threads, so it must never be mutated.
 * Contains no client classes: safe on dedicated servers.
 */
public final class RenderData {
    public static final RenderData EMPTY = new RenderData(new BlockState[7], filled(-1), 0);

    /** Cover block state per {@link AttributeLocation} ordinal, or null. */
    private final BlockState[] covers;
    /** Dye colour (0xRRGGBB) per location, or -1. */
    private final int[] dyes;
    /** Extra shape data from the block entity (see {@code CoverableBlockEntity#getShapeData}). */
    public final int shape;

    private RenderData(BlockState[] covers, int[] dyes, int shape) {
        this.covers = covers;
        this.dyes = dyes;
        this.shape = shape;
    }

    static RenderData of(Map<AttributeKey, ItemStack> items, int shape) {
        if (items.isEmpty() && shape == 0) {
            return EMPTY;
        }
        BlockState[] covers = new BlockState[7];
        int[] dyes = filled(-1);
        items.forEach((key, stack) -> {
            int index = key.location.ordinal();
            if (key.type == AttributeType.COVER && stack.getItem() instanceof BlockItem) {
                covers[index] = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            } else if (key.type == AttributeType.DYE && stack.getItem() instanceof DyeItem) {
                float[] rgb = ((DyeItem) stack.getItem()).getDyeColor().getTextureDiffuseColors();
                dyes[index] = ((int) (rgb[0] * 255) << 16) | ((int) (rgb[1] * 255) << 8) | (int) (rgb[2] * 255);
            }
        });
        return new RenderData(covers, dyes, shape);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    /** @return the cover at that location, or null. */
    public BlockState cover(AttributeLocation location) {
        return covers[location.ordinal()];
    }

    /** @return the dye colour at that location, or -1. */
    public int dye(AttributeLocation location) {
        return dyes[location.ordinal()];
    }

    /**
     * Which location supplies the look of a face: the side itself if it has a side cover,
     * otherwise the host. Same rule as clicks use ({@code AttributeRules#effectiveLocation}).
     */
    public AttributeLocation faceLocation(AttributeLocation side) {
        return covers[side.ordinal()] != null ? side : AttributeLocation.HOST;
    }

    /** Whether every side shows the host cover (no side covers). */
    public boolean hasOnlyHostCover() {
        for (int i = 0; i < AttributeLocation.HOST.ordinal(); i++) {
            if (covers[i] != null) {
                return false;
            }
        }
        return true;
    }

    private static int[] filled(int value) {
        int[] array = new int[7];
        Arrays.fill(array, value);
        return array;
    }
}
