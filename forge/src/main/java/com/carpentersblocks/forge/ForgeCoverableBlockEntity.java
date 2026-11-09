package com.carpentersblocks.forge;

import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.util.BlockFlags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Forge passes block entity update packets of any type to {@link #onDataPacket}, and hands
 * {@link #getModelData()} to our model when the chunk is built.
 */
public class ForgeCoverableBlockEntity extends CoverableBlockEntity {
    public static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    @Override
    protected void syncToClients() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            // Makes the chunk send getUpdatePacket() to every player tracking this block.
            level.sendBlockUpdated(getBlockPos(), state, state, BlockFlags.CLIENTS);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new ClientboundBlockEntityDataPacket(getBlockPos(), 0, writeAttributes(new CompoundTag()));
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        readAttributes(packet.getTag());
        onClientDataReceived();
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundTag tag) {
        load(state, tag);
        onClientDataReceived();
    }

    @Override
    public IModelData getModelData() {
        return new ModelDataMap.Builder().withInitial(RENDER_DATA, getRenderData()).build();
    }

    @Override
    protected void onClientDataReceived() {
        requestModelDataUpdate();
        super.onClientDataReceived();
    }
}
