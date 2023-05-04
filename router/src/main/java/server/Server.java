package server;

import client.*;
import engineFIX.EngineFIX;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

public class Server {
    private static final int marketPort = 5001;
    private static final int brokerPort = 5000;

    HashMap<Integer, MarketClient>          marketClients;
    HashMap<Integer, BrokerClient>          brokerClients;

    HashMap<Client, Client>                 routingTable;

    public Server()
    {
        this.marketClients = new HashMap<>();
        this.brokerClients = new HashMap<>();
        this.routingTable = new HashMap<>();
    }

    private MarketClient findTargetMarket(String marketName)
    {
        if (marketName == null) {
            System.out.println("Why market name is NULL ?!?!");
            System.exit(74);
        }
        for (MarketClient client : this.marketClients.values()) {
            if (client.getName() == null)
            {
                System.out.println("Why name is NULL ?!?!");
                System.exit(69);
            }
            if (client.getName().compareTo(marketName) == 0)
                return client;
        }
        return null;
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (socketChannel == null)
            return ;

        socketChannel.configureBlocking(false);

        InetSocketAddress localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();

        if (localAddress.getPort() == brokerPort)
        {
            String uniqueID = Client.generateRandomString(6);
            System.out.println("Registered Broker with Id: " + uniqueID);
            brokerClients.put(remoteAddress.getPort(), new BrokerClient(uniqueID, localAddress, socketChannel));
        }
        if (localAddress.getPort() == marketPort)
        {
            String uniqueID = Client.generateRandomString(6);
            System.out.println("Registered market with Id: " + uniqueID);
            marketClients.put(remoteAddress.getPort(), new MarketClient(uniqueID, localAddress, socketChannel));
        }

        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void read(Selector selector, SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel)key.channel();

        if (socketChannel == null)
            return ;

        InetSocketAddress localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);

        if (localAddress.getPort() == brokerPort)
        {
            BrokerClient broker = brokerClients.get(remoteAddress.getPort());
            broker.read(buffer.array(), bytesRead);
            System.out.println("Reading data from broker " + broker.getUniqueID());
            if (broker.messageComplete())
            {
                System.out.println("Broker message is complete");
                // We finished parsing the message, let's try to pair this broker with the target market.
                String targetMarketName = broker.getTargetMarket();
                Client marketClient = findTargetMarket(targetMarketName);

                if (marketClient == null)
                    System.exit(1);

                socketChannel.register(selector, SelectionKey.OP_WRITE);

                // Let's pair the two clients in the routing table, so we can know where to forward the response from market.
                routingTable.put(broker, marketClient);
                routingTable.put(marketClient, broker);
            }
        }
        if (localAddress.getPort() == marketPort)
        {
            MarketClient market = marketClients.get(remoteAddress.getPort());
            market.read(buffer.array(), bytesRead);
            System.out.println("Reading data from market " + market.getName());
            if (market.messageComplete())
            {
                System.out.println("Market message is complete");
                socketChannel.register(selector, SelectionKey.OP_WRITE);

                // Market is identifying itself, nothing to forward.
                if (market.parser.getMsgType().compareTo("identification") == 0)
                    market.clean();
            }
        }
    }

    private void write(Selector selector, SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if (socketChannel == null)
            return ;

        InetSocketAddress localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();

        if (localAddress.getPort() == brokerPort)
        {
            Client broker = brokerClients.get(remoteAddress.getPort());
            Client targetMarket = routingTable.get(broker);

            if (targetMarket != null && targetMarket.messageComplete())
            {
                System.out.println("Writing to broker " + broker.getUniqueID());
                byte[] bytes = EngineFIX.toPrimitiveArray(targetMarket.parser.getRawData());
                socketChannel.write(ByteBuffer.wrap(bytes));
                socketChannel.register(selector, SelectionKey.OP_READ);
                targetMarket.clean();
            }
        }
        if (localAddress.getPort() == marketPort)
        {
            Client market = marketClients.get(remoteAddress.getPort());
            Client targetBroker = routingTable.get(market);

            if (targetBroker != null && targetBroker.messageComplete())
            {
                System.out.println("Writing to market " + market.getName());
                byte[] bytes = EngineFIX.toPrimitiveArray(targetBroker.parser.getRawData());
                socketChannel.write(ByteBuffer.wrap(bytes));
                socketChannel.register(selector, SelectionKey.OP_READ);
                targetBroker.clean();
            }
        }
    }

    public void start() {
        try {
            Selector selector = Selector.open();

            ServerSocketChannel marketServerSocketChannel =  ServerSocketChannel.open();
            marketServerSocketChannel.configureBlocking(false);
            marketServerSocketChannel.bind(new InetSocketAddress("localhost", marketPort));
            marketServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            ServerSocketChannel brokerServerSocketChannel =  ServerSocketChannel.open();
            brokerServerSocketChannel.configureBlocking(false);
            brokerServerSocketChannel.bind(new InetSocketAddress("localhost", brokerPort));
            brokerServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true)
            {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        accept(selector, key);
                    }
                    else if (key.isReadable()) {
                        read(selector, key);
                        iterator.remove();
                    }
                    else if (key.isWritable()) {
                        write(selector, key);
                        iterator.remove();
                    }
                }
            }
        } catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
}
