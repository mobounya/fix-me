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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int marketPort = 5001;
    private static final int brokerPort = 5000;

    private ArrayList<String>       marketNames;

    private final HashMap<Integer, MarketClient>          marketClients;
    private final HashMap<Integer, BrokerClient>          brokerClients;

    private final HashMap<Client, Client>                 routingTable;

    private final ExecutorService service;

    private final Object monitor = new Object();

    public Server()
    {
        this.service = Executors.newFixedThreadPool(35);
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

    private void accept(Selector selector, SelectionKey key) {
        service.submit(() -> {
            synchronized (monitor)
            {
                try {
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
                        synchronized (brokerClients)
                        {
                            brokerClients.put(remoteAddress.getPort(), new BrokerClient(uniqueID, localAddress, socketChannel));
                        }
                        System.out.println("Registered Broker with Id: " + uniqueID);
                    }
                    if (localAddress.getPort() == marketPort)
                    {
                        String uniqueID = Client.generateRandomString(6);
                        synchronized (marketClients)
                        {
                            marketClients.put(remoteAddress.getPort(), new MarketClient(uniqueID, localAddress, socketChannel));
                        }
                        System.out.println("Registered market with Id: " + uniqueID);
                    }
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    selector.wakeup();
                } catch (IOException e) {
                    System.out.println("Accept: " + e.getMessage());
                    System.exit(1);
                }
            }
        });
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
        {
            synchronized (brokerClients)
            {
                client = brokerClients.get(remoteAddress.getPort());
            }
        }
        if (localAddress.getPort() == marketPort)
        {
            synchronized (marketClients)
            {
                client = marketClients.get(remoteAddress.getPort());
            }
        }

        Client finalClient = client;

        service.submit(() -> {
            try {
                synchronized (monitor)
                {
                    if (finalClient.getClientState() >= Client.INVALID)
                        return ;

                    System.out.println("Client " + finalClient.getName() + " is reading...");
                    finalClient.read(buffer.array(), bytesRead);

                    // Client is invalid due to some error in the request.
                    if (!finalClient.parser.isValid())
                    {
                        System.err.println("Client message is broken");
                        System.exit(1);
                    }

                    // Client message is complete without errors.
                    else if (finalClient.messageComplete())
                    {
                        System.out.println("Client message is complete");

                        // unique id sent need to be the same on as assigned at first.
                        if (finalClient.isIdSent() && !finalClient.getUniqueID().equals(finalClient.parser.getSenderSubID()))
                        {
                            System.err.println("Unique id: " + finalClient.getUniqueID() + " doesn't match id assigned: " + finalClient.parser.getSenderSubID());
                            finalClient.setValid(false);
                        }
                        else if (finalClient.parser.getMsgType().compareTo("A") == 0)
                        {
                            System.out.println("Received Identification message");

                            // Client need to provide a name.
                            if (finalClient.getName() == null)
                            {
                                System.err.println("Client didn't provide a name");
                                finalClient.setValid(false);
                            }
                            // If client is a market we need to check if the name is unique,
                            // since we use the market name to pair it with a broker.
                            else if (finalClient.getClientType().equals("market"))
                            {
                                if (isMarketNameAlreadyUsed(finalClient.getName()))
                                {
                                    System.err.println("Market name is already used !");
                                    finalClient.setValid(false);
                                } else
                                {
                                    System.out.println("Market is fine");
                                    this.marketNames.add(finalClient.getName());
                                    finalClient.resetParser();
                                    finalClient.setClientState(Client.COMPLETED);
                                }
                            } else if (finalClient.getClientType().equals("broker"))
                            {
                                String targetClientName = finalClient.getTargetMarket();
                                Client targetClient = findTargetMarket(targetClientName);

                                if (targetClient == null)
                                {
                                    System.err.println("Target client: " + targetClientName + " not found");
                                    finalClient.clearMarketFound();
                                    finalClient.setClientState(Client.INVALID);
                                    socketChannel.register(selector, SelectionKey.OP_WRITE);
                                    selector.wakeup();
                                    return ;
                                }

                                // Let's pair the two clients in the routing table, so we can know where to forward the response from market.
                                System.out.println("Paired client: " + finalClient.getName() + " with target " + targetClient.getName());
                                routingTable.put(finalClient, targetClient);
                                routingTable.put(targetClient, finalClient);
                                finalClient.resetParser();
                                finalClient.setClientState(Client.COMPLETED);
                            }
                        }
                        socketChannel.register(selector, SelectionKey.OP_WRITE);
                        selector.wakeup();
                    }
                }
            } catch (IOException ex) {
                System.err.println("Reading task failed: " + ex.getMessage());
            }
        });
    }

    private void write(Selector selector, SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        if (socketChannel == null)
            return;

        InetSocketAddress localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();

        Client client = null;

        if (localAddress.getPort() == brokerPort) {
            synchronized (brokerClients) {
                client = brokerClients.get(remoteAddress.getPort());
            }
        }
        if (localAddress.getPort() == marketPort) {
            synchronized (marketClients) {
                client = marketClients.get(remoteAddress.getPort());
            }
        }

        Client finalClient = client;

        service.submit(() -> {
            synchronized (monitor) {
                if (finalClient.getClientState() < Client.ESTABLISHED)
                    return ;
                if (!finalClient.isTargetFound() || !finalClient.parser.isValid() || !finalClient.isValid()) {
                    // Client is broken which means it's not paired to any other client,
                    // start a task to send a reject message.
                    try {
                        System.err.println("Client is not valid");
                        String message = EngineFIX.getFixSessionRejectMessage();
                        byte[] bytes = message.getBytes();
                        socketChannel.write(ByteBuffer.wrap(bytes));
                        System.out.println("Cleaning client");
                        finalClient.resetClient();
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        selector.wakeup();
                    } catch (IOException ex) {
                        System.err.println("Send reject message task failed: " + ex.getMessage());
                    }
                    return ;
                }

                if (!finalClient.isIdSent()) {
                    // Start a task to send a unique id to the client, if we didn't send it before.
                    try {
                        System.out.println("Sending unique id: " + finalClient.getUniqueID() + " to client");
                        String message = EngineFIX.constructIdentificationMessage(finalClient.getUniqueID(), "does not matter here", finalClient.getName());
                        socketChannel.write(ByteBuffer.wrap(message.getBytes()));
                        finalClient.setIdSent();
                        finalClient.setClientState(Client.ESTABLISHED);
                        if (finalClient.getClientType().equals("market")) {
                            socketChannel.register(selector, SelectionKey.OP_WRITE);
                        } else if (finalClient.getClientType().equals("broker")) {
                            socketChannel.register(selector, SelectionKey.OP_READ);
                        }
                        selector.wakeup();
                    } catch (IOException ex) {
                        System.err.println("Send unique id message task failed: " + ex.getMessage());
                    }
                    return ;
                }

                Client targetClient = routingTable.get(finalClient);

                if (targetClient != null && targetClient.messageComplete()) {
                    try {
                        byte[] bytes;
                        String targetName;

                        targetName = targetClient.getName();
                        bytes = EngineFIX.toPrimitiveArray(targetClient.parser.getRawData());
                        targetClient.resetParser();
                        targetClient.setClientState(Client.ESTABLISHED);

                        socketChannel.write(ByteBuffer.wrap(bytes));
                        System.out.println("Writing data to " + finalClient.getName() + " from " + targetName);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        selector.wakeup();
                    } catch (IOException ex) {
                        System.err.println("Data forward task failed: " + ex.getMessage());
                    }
                }
            }
        });
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
                Iterator<SelectionKey> iterator;

                selector.select();
                iterator = selector.selectedKeys().iterator();

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
