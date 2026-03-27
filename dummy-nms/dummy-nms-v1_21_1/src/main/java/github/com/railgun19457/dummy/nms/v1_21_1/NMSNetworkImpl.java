package github.com.railgun19457.dummy.nms.v1_21_1;

import github.com.railgun19457.dummy.nms.NMSNetwork;
import github.com.railgun19457.dummy.nms.v1_21_1.network.DummyConnection;

import java.net.InetAddress;

public class NMSNetworkImpl implements NMSNetwork {

    private final DummyConnection connection;

    public NMSNetworkImpl(InetAddress address) {
        this.connection = new DummyConnection(address);
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void disconnect() {
        // Empty implementation for dummy connection
    }
}
