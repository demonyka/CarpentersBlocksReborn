package com.carpentersblocks.client.fabric;

import com.carpentersblocks.fabric.FabricNetworking;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;

/** Fabric side of {@link com.carpentersblocks.client.ClientPlatform}. */
public final class ClientPlatformImpl {
    private ClientPlatformImpl() {
    }

    public static void sendSlopeSwitch(boolean forward) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(forward);
        ClientPlayNetworking.send(FabricNetworking.SLOPE_SWITCH, buf);
    }
}
