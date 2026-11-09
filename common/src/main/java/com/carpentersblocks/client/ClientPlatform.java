package com.carpentersblocks.client;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Client-only loader differences. Separate from {@code Platform} so that dedicated servers
 * never load the implementations (they reference client networking classes).
 */
public final class ClientPlatform {
    private ClientPlatform() {
    }

    /** Asks the server to switch the held slope item to the next/previous kind. */
    @ExpectPlatform
    public static void sendSlopeSwitch(boolean forward) {
        throw new AssertionError();
    }
}
