package com.carpentersblocks.client;

import com.carpentersblocks.item.SlopeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client side of sneak + mouse wheel slope switching (see {@code SlopeItemSwitch}). */
public final class SlopeScrollHandler {
    private SlopeScrollHandler() {
    }

    /**
     * @param delta scroll amount, positive = wheel up
     * @return whether the scroll was used (and must not change the hotbar slot)
     */
    public static boolean onScroll(double delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (delta == 0 || player == null || mc.screen != null || !player.isShiftKeyDown()
            || !(player.getMainHandItem().getItem() instanceof SlopeItem)) {
            return false;
        }
        ClientPlatform.sendSlopeSwitch(delta > 0);
        return true;
    }
}
