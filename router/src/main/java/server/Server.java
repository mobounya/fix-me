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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Server {
    private static final int marketPort = 5001;
    private static final int brokerPort = 5000;

    private ArrayList<String>       marketNames;

    private final HashMap<Integer, MarketClient>          marketClients;
    private final HashMap<Integer, BrokerClient>          brokerClients;

    private final HashMap<Client, Client>                 routingTable;

    public Server()
    {
        this.marketNames = new ArrayList<>();
        this.marketClients = new HashMap<>();
        this.brokerClients = new HashMap<>();
        this.routingTable = new HashMap<>();
    }

    private MarketClient findTargetMarket(String marketName)
    {
        if (marketName == null)
            return null;
        for (MarketClient client : this.marketClients.values()) {
            if (client.getName().compareTo(marketName) == 0)
                return client;
        }
        return null;
    }

    private boolean isMarketNameAlreadyUsed(String marketName)
    {
        return (this.marketNames.contains(marketName));
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

        if (bytesRead <= 0)
            return ;

        Client client = null;

        if (localAddress.getPort() == brokerPort)
            client = brokerClients.get(remoteAddress.getPort());
        if (localAddress.getPort() == marketPort)
            client = marketClients.get(remoteAddress.getPort());

        System.out.println("Client " + client.getName() + " is reading...");
        client.read(buffer.array(), bytesRead);

        // Client is invalid due to some error in the request.
        if (!client.parser.isValid())
        {
            System.err.println("Client message is broken");
            System.exit(1);
        }
        // Client message is complete without errors.
        else if (client.messageComplete())
        {
            System.out.println("Client message is complete");
            socketChannel.register(selector, SelectionKey.OP_WRITE);

            // unique id sent need to be the same on as assigned at first.
            if (client.isIdSent() && !client.getUniqueID().equals(client.parser.getSenderSubID()))
            {
                System.err.println("Unique id: " + client.getUniqueID() + " doesn't match id assigned: " + client.parser.getSenderSubID());
                client.setValid(false);
                return ;
            }

            if (client.parser.getMsgType().compareTo("A") == 0)
            {
                System.out.println("Received Identification message");

                // Client need to provide a name.
                if (client.getName() == null)
                {
                    System.err.println("Client didn't provide a name");
                    client.setValid(false);
                }
                // If client is a market we need to check if the name is unique,
                // since we use the market name to pair it with a broker.
                else if (client.getClientType().equals("market"))
                {
                    if (isMarketNameAlreadyUsed(client.getName()))
                    {
                        System.err.println("Market name is already used !");
                        client.setValid(false);
                    } else
                    {
                        this.marketNames.add(client.getName());
                        client.clean();
                    }
                } else
                    client.clean();
                return ;
            }

            // Only brokers need to be paired in the routing table.
            if (client.getClientType().equals("broker") && routingTable.get(client) == null)
            {
                String targetClientName = client.getTargetMarket();
                Client targetClient = findTargetMarket(targetClientName);

                if (targetClient == null)
                {
                    System.err.println("Target client: " + targetClientName + " not found");
                    client.clearMarketFound();
                    return ;
                }

                // Let's pair the two clients in the routing table, so we can know where to forward the response from market.
                System.out.println("Paired client: " + client.getName() + " with target " + targetClient.getName());
                routingTable.put(client, targetClient);
                routingTable.put(targetClient, client);
            }
        }
    }

    private void write(Selector selector, SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if (socketChannel == null)
            return ;

        InetSocketAddress localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();

        Client client = null;

        if (localAddress.getPort() == brokerPort)
            client = brokerClients.get(remoteAddress.getPort());
        if (localAddress.getPort() == marketPort)
            client = marketClients.get(remoteAddress.getPort());

        // Client is broken which means it's not paired to any other client,
        // send a reject message.
        if (!client.isTargetFound() || !client.parser.isValid() || !client.isValid())
        {
            System.err.println("Client is not valid");
            String message = EngineFIX.getFixSessionRejectMessage();
            byte[] bytes = message.getBytes();
            socketChannel.write(ByteBuffer.wrap(bytes));
            socketChannel.register(selector, SelectionKey.OP_READ);
            client.clean();
            return ;
        }

        // Send a unique id to the client, if we didn't send it before.
        if (!client.isIdSent())
        {
            System.out.println("Sending unique id: " + client.getUniqueID() + " to client");
            String message = EngineFIX.constructIdentificationMessage(client.getUniqueID(), "does not matter here");
            socketChannel.write(ByteBuffer.wrap(message.getBytes()));
            if (client.getClientType().equals("market"))
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            else if (client.getClientType().equals("broker"))
                socketChannel.register(selector, SelectionKey.OP_READ);
            client.setIdSent();
            return ;
        }

        Client targetClient = routingTable.get(client);

        if (targetClient != null && targetClient.messageComplete())
        {
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("Writing data to " + client.getName() + " from " + targetClient.getName());
            byte[] bytes = EngineFIX.toPrimitiveArray(targetClient.parser.getRawData());
            socketChannel.write(ByteBuffer.wrap(bytes));
            targetClient.clean();
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
