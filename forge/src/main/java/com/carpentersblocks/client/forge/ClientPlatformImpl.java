package com.carpentersblocks.client.forge;

import com.carpentersblocks.forge.ForgeNetwork;

/** Forge side of {@link com.carpentersblocks.client.ClientPlatform}. */
public final class ClientPlatformImpl {
    private ClientPlatformImpl() {
    }

    public static void sendSlopeSwitch(boolean forward) {
        ForgeNetwork.CHANNEL.sendToServer(new ForgeNetwork.SlopeSwitchMessage(forward));
    }
}
