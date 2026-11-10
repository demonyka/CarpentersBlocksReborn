package com.carpentersblocks.fabric;

import com.carpentersblocks.blockentity.CoverableBlockEntity;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * On Fabric 1.16 vanilla ignores block entity update packets for modded types, so syncing
 * goes through the Fabric API interface {@link BlockEntityClientSerializable}. The render
 * attachment hands {@link #getRenderData()} to our model when the chunk is built.
 */
public class FabricCoverableBlockEntity extends CoverableBlockEntity
    implements BlockEntityClientSerializable, RenderAttachmentBlockEntity {
    @Override
    protected void syncToClients() {
        if (level instanceof ServerLevel) {
            sync();
        }
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        readAttributes(tag);
        onClientDataReceived();
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return writeClientAttributes(tag);
    }

    @Override
    public Object getRenderAttachmentData() {
        return getRenderData();
    }
}
