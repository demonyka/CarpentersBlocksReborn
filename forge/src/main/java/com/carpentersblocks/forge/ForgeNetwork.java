package com.carpentersblocks.forge;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.interaction.SlopeItemSwitch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** The mod's only Forge packet: sneak + mouse wheel slope switching, client to server. */
public final class ForgeNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        CarpentersBlocks.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ForgeNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, SlopeSwitchMessage.class,
            (message, buf) -> buf.writeBoolean(message.forward),
            buf -> new SlopeSwitchMessage(buf.readBoolean()),
            ForgeNetwork::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static void handle(SlopeSwitchMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();
        // Runs on the server thread, never on the network thread.
        ctx.enqueueWork(() -> {
            if (player != null) {
                SlopeItemSwitch.apply(player, message.forward);
            }
        });
        ctx.setPacketHandled(true);
    }

    public static final class SlopeSwitchMessage {
        final boolean forward;

        public SlopeSwitchMessage(boolean forward) {
            this.forward = forward;
        }
    }
}
