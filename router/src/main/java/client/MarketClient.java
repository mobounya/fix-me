package client;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class MarketClient extends Client {
    public MarketClient(String uniqueID, InetSocketAddress address, SocketChannel socket)
    {
        super(uniqueID, address, socket);
    }
}
