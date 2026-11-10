package com.carpentersblocks.util;

import com.carpentersblocks.CarpentersBlocks;

/**
 * Switches for the automated tests ({@code SmokeTest}, {@code MultiplayerTest}, the client
 * test harness). Those classes are only in jars built with {@code -PwithTests}; release jars
 * leave them out, so a JVM flag cannot turn a production server into a test server.
 */
public final class DevTests {
    private DevTests() {
    }

    /** {@code -Dcarpentersblocks.smokeTest=true} or {@code -Dcarpentersblocks.mpTest=true} on a test build. */
    public static boolean server() {
        return (Boolean.getBoolean("carpentersblocks.smokeTest") || Boolean.getBoolean("carpentersblocks.mpTest"))
            && present("com.carpentersblocks.util.SmokeTest");
    }

    /** {@code -Dcarpentersblocks.clientTest=...} on a test build. */
    public static boolean client() {
        String mode = System.getProperty("carpentersblocks.clientTest", "");
        return !mode.isEmpty() && !mode.equals("false") && present("com.carpentersblocks.client.devtest.ClientTestHarness");
    }

    private static boolean present(String className) {
        if (DevTests.class.getResource("/" + className.replace('.', '/') + ".class") != null) {
            return true;
        }
        CarpentersBlocks.LOGGER.warn("A Carpenter's Blocks test flag is set, but this is a release build without the tests; ignoring it");
        return false;
    }
}
