package com.carpentersblocks.interaction;

import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.item.SlopeItem;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Sneak + mouse wheel with a slope in hand switches it to the next/previous slope kind
 * (1.7.10 behaviour). The client only asks; this runs on the server thread and changes
 * nothing but the held stack's item (count and NBT are kept), so it cannot create items.
 */
public final class SlopeItemSwitch {
    private SlopeItemSwitch() {
    }

    public static SlopePlacement.Kind cycle(SlopePlacement.Kind kind, boolean forward) {
        SlopePlacement.Kind[] kinds = SlopePlacement.Kind.values();
        return kinds[(kind.ordinal() + (forward ? 1 : kinds.length - 1)) % kinds.length];
    }

    /** @return whether the held item was switched */
    public static boolean apply(Player player, boolean forward) {
        ItemStack held = player.getMainHandItem();
        if (player.isSpectator() || !(held.getItem() instanceof SlopeItem)) {
            return false;
        }
        SlopePlacement.Kind next = cycle(((SlopeItem) held.getItem()).kind, forward);
        ItemStack replacement = new ItemStack(CBContent.slopeItem(next), held.getCount());
        replacement.setTag(held.getTag() == null ? null : held.getTag().copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
        return true;
    }
}
