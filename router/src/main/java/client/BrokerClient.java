package client;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class BrokerClient extends Client {
    public BrokerClient(String uniqueID, InetSocketAddress address, SocketChannel socket)
    {
        super(uniqueID, address, socket);
    }
}
