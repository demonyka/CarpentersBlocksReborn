package com.carpentersblocks.forge.compat;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Jade / HWYLA: the tooltip of a Carpenter's block names the block it is covered with.
 * Found by those mods through the annotation; never loaded without them.
 */
@WailaPlugin
public final class WailaCompat implements IWailaPlugin, IComponentProvider {
    private static final AttributeKey COVER = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);

    @Override
    public void register(IRegistrar registrar) {
        registrar.registerComponentProvider(this, TooltipPosition.BODY, CoverableBlock.class);
    }

    @Override
    public void appendBody(List<Component> tooltip, IDataAccessor accessor, IPluginConfig config) {
        BlockEntity be = accessor.getTileEntity();
        if (be instanceof CoverableBlockEntity) {
            ItemStack cover = ((CoverableBlockEntity) be).getItem(COVER);
            if (!cover.isEmpty()) {
                tooltip.add(new TranslatableComponent("tooltip.carpentersblocks.cover", cover.getHoverName()));
            }
        }
    }
}
