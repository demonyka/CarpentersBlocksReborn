package com.carpentersblocks.util;

import com.carpentersblocks.CarpentersBlocks;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;

/**
 * Textures used in code rather than by JSON models: uncovered faces and parts that never take
 * a cover (glass panes, flames, iron rails). The loaders add {@link #ALL} to the block atlas.
 */
public final class UncoveredTextures {
    public static final ResourceLocation FULL_FRAME = CarpentersBlocks.id("block/general/uncovered_full_frame");
    public static final ResourceLocation QUARTERED_FRAME = CarpentersBlocks.id("block/general/uncovered_quartered_frame");
    public static final ResourceLocation SOLID = CarpentersBlocks.id("block/general/uncovered_solid");
    public static final ResourceLocation OBLIQUE_POS = CarpentersBlocks.id("block/slope/oblique_pos");
    public static final ResourceLocation OBLIQUE_NEG = CarpentersBlocks.id("block/slope/oblique_neg");

    public static final ResourceLocation LEVER = CarpentersBlocks.id("block/lever/lever");
    public static final ResourceLocation TORCH_HEAD_LIT = CarpentersBlocks.id("block/torch/torch_head_lit");
    public static final ResourceLocation TORCH_HEAD_SMOLDERING = CarpentersBlocks.id("block/torch/torch_head_smoldering");
    public static final ResourceLocation TORCH_HEAD_UNLIT = CarpentersBlocks.id("block/torch/torch_head_unlit");
    public static final ResourceLocation LANTERN_GLASS = CarpentersBlocks.id("block/torch/lantern_glass");
    public static final ResourceLocation DOOR_GLASS_TOP = CarpentersBlocks.id("block/door/door_glass_top");
    public static final ResourceLocation DOOR_GLASS_TALL_TOP = CarpentersBlocks.id("block/door/door_glass_tall_top");
    public static final ResourceLocation DOOR_GLASS_TALL_BOTTOM = CarpentersBlocks.id("block/door/door_glass_tall_bottom");
    public static final ResourceLocation DOOR_SCREEN_TALL = CarpentersBlocks.id("block/door/door_screen_tall");
    public static final ResourceLocation DOOR_FRENCH_TOP = CarpentersBlocks.id("block/door/door_french_glass_top");
    public static final ResourceLocation DOOR_FRENCH_BOTTOM = CarpentersBlocks.id("block/door/door_french_glass_bottom");
    public static final ResourceLocation HATCH_GLASS = CarpentersBlocks.id("block/hatch/hatch_glass");
    public static final ResourceLocation HATCH_SCREEN = CarpentersBlocks.id("block/hatch/hatch_screen");
    public static final ResourceLocation HATCH_FRENCH = CarpentersBlocks.id("block/hatch/hatch_french_glass");
    public static final ResourceLocation GARAGE_GLASS = CarpentersBlocks.id("block/garagedoor/glass");
    public static final ResourceLocation GARAGE_GLASS_TOP = CarpentersBlocks.id("block/garagedoor/glass_top");

    public static final ResourceLocation IRON = new ResourceLocation("minecraft", "block/iron_block");
    public static final ResourceLocation LAPIS = new ResourceLocation("minecraft", "block/lapis_block");
    public static final ResourceLocation REDSTONE = new ResourceLocation("minecraft", "block/redstone_block");

    public static final List<ResourceLocation> ALL = Arrays.asList(FULL_FRAME, QUARTERED_FRAME, SOLID, OBLIQUE_POS, OBLIQUE_NEG,
        LEVER, TORCH_HEAD_LIT, TORCH_HEAD_SMOLDERING, TORCH_HEAD_UNLIT, LANTERN_GLASS,
        DOOR_GLASS_TOP, DOOR_GLASS_TALL_TOP, DOOR_GLASS_TALL_BOTTOM, DOOR_SCREEN_TALL, DOOR_FRENCH_TOP, DOOR_FRENCH_BOTTOM,
        HATCH_GLASS, HATCH_SCREEN, HATCH_FRENCH, GARAGE_GLASS, GARAGE_GLASS_TOP);

    private UncoveredTextures() {
    }
}
