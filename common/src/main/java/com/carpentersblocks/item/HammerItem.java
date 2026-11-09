package com.carpentersblocks.item;

import net.minecraft.world.item.Item;

/**
 * Carpenter's Hammer. Right-click or left-click alters a block's shape; sneak + left-click
 * removes the last attribute. All logic lives in {@code Interactions}.
 */
public class HammerItem extends Item {
    public HammerItem(Properties properties) {
        super(properties);
    }
}
