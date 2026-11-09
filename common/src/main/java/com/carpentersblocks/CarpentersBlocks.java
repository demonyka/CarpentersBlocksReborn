package com.carpentersblocks;

import com.carpentersblocks.registry.CBContent;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Loader-independent entry point. Each platform calls {@link #init()} from its own initializer. */
public final class CarpentersBlocks {
    public static final String MOD_ID = "carpentersblocks";
    public static final Logger LOGGER = LogManager.getLogger("Carpenter's Blocks");

    private CarpentersBlocks() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static void init() {
        CBContent.init();
    }
}
