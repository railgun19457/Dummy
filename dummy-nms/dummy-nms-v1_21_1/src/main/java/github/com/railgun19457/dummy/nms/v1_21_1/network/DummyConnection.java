package github.com.railgun19457.dummy.nms.v1_21_1.network;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class DummyConnection extends Connection {

    public DummyConnection(InetAddress address) {
        super(PacketFlow.SERVERBOUND);
        try {
            EmbeddedChannel embeddedChannel = new EmbeddedChannel();
            InetSocketAddress socketAddress = new InetSocketAddress(address, 0);

            Field channelField = null;
            Field addressField = null;

            for (Field field : Connection.class.getDeclaredFields()) {
                if (field.getType() == io.netty.channel.Channel.class) {
                    channelField = field;
                } else if (field.getType() == java.net.SocketAddress.class) {
                    addressField = field;
                }
            }

            if (channelField != null) {
                channelField.setAccessible(true);
                channelField.set(this, embeddedChannel);
            }

            if (addressField != null) {
                addressField.setAccessible(true);
                addressField.set(this, socketAddress);
            }

            Connection.configureSerialization(embeddedChannel.pipeline(), PacketFlow.SERVERBOUND, false, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize DummyConnection", e);
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void send(Packet<?> packet) {
    }
}
