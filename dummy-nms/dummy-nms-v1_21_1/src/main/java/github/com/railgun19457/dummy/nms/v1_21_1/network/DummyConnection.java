package github.com.railgun19457.dummy.nms.v1_21_1.network;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class DummyConnection extends Connection {

    public DummyConnection(InetAddress address) {
        super(PacketFlow.SERVERBOUND);
        this.channel = new EmbeddedChannel();
        this.address = new InetSocketAddress(address, 0);
        Connection.configureSerialization(this.channel.pipeline(), PacketFlow.SERVERBOUND, false, null);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void send(Packet<?> packet) {
    }
}
