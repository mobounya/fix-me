package server;


import logger.Logger;
import client.*;
import engineFIX.EngineFIX;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class Server {
    private static final int marketPort = 5001;
    private static final int brokerPort = 5000;

    private ArrayList<String> marketNames;

    private final HashMap<Integer, Client> marketClients;
    private final HashMap<Integer, Client> brokerClients;

    private final HashMap<Client, Client> routingTable;

    private final ExecutorService service;

    private final Object monitor = new Object();

    public Server() {
        this.service = Executors.newFixedThreadPool(35);
        this.marketNames = new ArrayList<>();
        this.marketClients = new HashMap<>();
        this.brokerClients = new HashMap<>();
        this.routingTable = new HashMap<>();
    }

    private void sendSessionRejectMessage(SocketChannel socketChannel, Client client, Selector selector) throws IOException {
        String message = EngineFIX.getFixSessionRejectMessage();
        byte[] bytes = message.getBytes();
        socketChannel.write(ByteBuffer.wrap(bytes));
        Logger.logError("Sent a session level reject to (" + client.getName() + ").");
        client.resetClient();
        socketChannel.register(selector, SelectionKey.OP_READ);
        selector.wakeup();
    }

    private void sendUniqueId(SocketChannel socketChannel, Client client, Selector selector) throws IOException {
        String message = EngineFIX.constructIdentificationMessage(client.getUniqueID(), "does not matter here", client.getName());
        socketChannel.write(ByteBuffer.wrap(message.getBytes()));
        Logger.logSuccess("Sent unique id (" + client.getUniqueID() + ") to client (" + client.getName() + ").");
        client.setIdSent();
        client.setClientState(Client.ESTABLISHED);
        if (client.getClientType().equals("market")) {
            socketChannel.register(selector, SelectionKey.OP_WRITE);
        } else if (client.getClientType().equals("broker")) {
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
        selector.wakeup();
    }

    private void forwardMessage(SocketChannel destinationSocket, Client destination, Client source, Selector selector) throws IOException {
        byte[] bytes;
        String targetName;

        targetName = source.getName();
        bytes = EngineFIX.toPrimitiveArray(source.parser.getRawData());
        source.resetParser();
        source.setClientState(Client.ESTABLISHED);

        destinationSocket.write(ByteBuffer.wrap(bytes));
        Logger.logSuccess("Wrote data to (" + destination.getName() + ") from (" + targetName + ").");
        destinationSocket.register(selector, SelectionKey.OP_READ);
        selector.wakeup();
    }

    private void pairClient(SocketChannel socketChannel, Client client, Selector selector) throws IOException {
        String targetClientName = client.getTargetMarket();
        ArrayList<Client> clientsFound = findTargetMarket(targetClientName);

        if (clientsFound == null) {
            Logger.logError("Target client (" + targetClientName + ") not found");
            client.clearMarketFound();
            client.setClientState(Client.INVALID);
            socketChannel.register(selector, SelectionKey.OP_WRITE);
            selector.wakeup();
            return;
        }

        Client targetClient = clientsFound.get(0);
        Client temp = routingTable.get(targetClient);

        // Client you're trying to connect to is already paired.
        if (temp != null) {
            // if the target Market is already paired with another broker
            // check if the broker is still open, if not allow this broker
            // to be paired with it, else mark message as invalid.
            if (!isSocketValid(temp.getSocket())) {
                client.setValid(false);
                Logger.logError("Market (" + targetClientName + ") is already connected to a broker");
                socketChannel.register(selector, SelectionKey.OP_WRITE);
                selector.wakeup();
            } else {
                Logger.logWarning("Market (" + targetClientName + ") is connected to a closed broker, cleaning old broker...");
                brokerClients.remove(temp.getRemoteAddress().getPort());
                routingTable.remove(targetClient);
                routingTable.remove(temp);
            }
        }

        // Let's pair the two clients in the routing table, so we can know where to forward the response from market.
        Logger.logSuccess("Paired client (" + client.getName() + ") with target (" + targetClient.getName() + ").");
        routingTable.put(client, targetClient);
        routingTable.put(targetClient, client);
        client.resetParser();
        client.setClientState(Client.COMPLETED);
    }

    private void purgeMarket(Client market)
    {
        try {
            SocketChannel socketChannel = market.getSocket();
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            this.marketClients.remove(remoteAddress.getPort());
            this.routingTable.remove(market);
        } catch (IOException ex) {
            Logger.logError("Failed to purge market (" + market.getName() + ") :" + ex.getMessage());
        }
    }

    private void registerMarket(Client client) throws DuplicateMarketNameException
    {
        String marketName = client.getName();
        if (!isMarketNameAlreadyUsed(marketName))
        {
            this.marketNames.add(client.getName());
            client.resetParser();
            client.setClientState(Client.COMPLETED);
        } else {
            ArrayList<Client> marketsFound = findTargetMarket(marketName);

            marketsFound.removeIf((clientArg) -> {
                boolean val = clientArg.getUniqueID().equals(client.getUniqueID());
                if (val)
                    System.out.println("Removing client with id (" + clientArg.getUniqueID() + ") and name (" + clientArg.getName() + ").");
                return val;
            });

            Client market = marketsFound.get(0);

            SocketChannel socketChannel = market.getSocket();

            if (isSocketValid(socketChannel))
                throw new DuplicateMarketNameException(marketName);
            else {
                purgeMarket(market);
                Logger.logWarning("Purged market (" + market.getName() + ") with Id (" + market.getUniqueID() + ").");
                client.resetParser();
                client.setClientState(Client.COMPLETED);
            }
        }
    }

    private ArrayList<Client> findTargetMarket(String marketName)
    {
        if (marketName == null)
            return null;
        ArrayList<Client> clients = new ArrayList<>();

        synchronized (marketClients)
        {
            for (Client client : this.marketClients.values()) {
                String name = client.getName();
                if (name != null && name.compareTo(marketName) == 0)
                {
                    clients.add(client);
                }
            }
        }
        return (clients.size() > 0) ? clients : null;
    }

    private boolean isSocketValid(SocketChannel socket)
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        try {
            return socket.read(buf) != -1;
        } catch (IOException e) {
            return false;
        }
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
                        Logger.logSuccess("Registered broker with Id: " + uniqueID);
                    }
                    if (localAddress.getPort() == marketPort)
                    {
                        String uniqueID = Client.generateRandomString(6);
                        synchronized (marketClients)
                        {
                            marketClients.put(remoteAddress.getPort(), new MarketClient(uniqueID, localAddress, socketChannel));
                        }
                        Logger.logSuccess("Registered market with Id: " + uniqueID);
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

    private void read(Selector selector, SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel)key.channel();

        if (socketChannel == null)
            return ;

        InetSocketAddress localAddress = null;
        InetSocketAddress remoteAddress = null;
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead;

        try {
            localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
            remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        } catch (IOException exception) {
            Logger.logError(exception.getMessage());
            System.exit(1);
        }

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

        try {
            bytesRead = socketChannel.read(buffer);
            if (bytesRead <= 0)
                return ;
        } catch (IOException exception) {
            Logger.logError("Read: " + exception.getMessage());
            synchronized (monitor)
            {
                client.setSocketValid(false);
            }
            key.cancel();
            return ;
        }

        Client finalClient = client;

        int finalBytesRead = bytesRead;

        service.submit(() -> {
                synchronized (monitor)
                {
                    try {
                        if (finalClient.getClientState() >= Client.INVALID)
                            return ;

                        Logger.logInfo("Client (" + finalClient.getName() + ") is reading...");

                        finalClient.read(buffer.array(), finalBytesRead);

                        // Client is invalid due to some error in the request.
                        if (!finalClient.parser.isValid())
                        {
                            Logger.logError("Client message is broken");
                            System.exit(1);
                        }
                        // Client message is complete without errors.
                        else if (finalClient.messageComplete())
                        {
                            Logger.logInfo("Client completed reading message");

                            // unique id sent need to be the same on as assigned at first.
                            if (finalClient.isIdSent() && !finalClient.getUniqueID().equals(finalClient.parser.getSenderSubID()))
                            {
                                Logger.logError("Unique id: (" + finalClient.parser.getSenderSubID() + ") doesn't match id assigned: (" + finalClient.getUniqueID() + ").");
                                finalClient.setValid(false);
                            }
                            else if (finalClient.parser.getMsgType().compareTo("A") == 0)
                            {
                                Logger.logInfo("Received identification message");

                                // Client need to provide a name.
                                if (finalClient.getName() == null)
                                {
                                    Logger.logError("Client didn't provide a name");
                                    finalClient.setValid(false);
                                }
                                // If client is a market we need to check if the name is unique,
                                // since we use the market name to pair it with a broker.
                                else if (finalClient.getClientType().equals("market"))
                                {
                                    try {
                                        registerMarket(finalClient);
                                    } catch (DuplicateMarketNameException ex)
                                    {
                                        Logger.logError(ex.getMessage());
                                        finalClient.setValid(false);
                                    }
                                } else if (finalClient.getClientType().equals("broker"))
                                {
                                    pairClient(socketChannel, finalClient, selector);
                                }
                            }
                            socketChannel.register(selector, SelectionKey.OP_WRITE);
                            selector.wakeup();
                        }
                    } catch (IOException e) {
                        Logger.logError("Read task failed: " + e.getMessage());
                        finalClient.setSocketValid(false);
                        key.cancel();
                    }
                }
        });
    }

    private void write(Selector selector, SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        if (socketChannel == null)
            return;

        InetSocketAddress localAddress = null;
        InetSocketAddress remoteAddress = null;

        try {
            localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
            remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        } catch (IOException exception) {
            Logger.logError("Write: " + exception.getMessage());
            System.exit(1);
        }

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

                if (!finalClient.isTargetFound() || !finalClient.parser.isValid() || !finalClient.isValid())
                {
                    try {
                        sendSessionRejectMessage(socketChannel, finalClient, selector);
                    } catch (IOException ex) {
                        Logger.logError("Sending session reject message to (" + finalClient.getName() + ") failed: " + ex.getMessage());
                        finalClient.setSocketValid(false);
                        key.cancel();
                    }
                    return ;
                }

                if (!finalClient.isIdSent())
                {
                    try {
                        sendUniqueId(socketChannel, finalClient, selector);
                    } catch (IOException ex) {
                        Logger.logError("Sending unique id to (" + finalClient.getName() + ") failed: " + ex.getMessage());
                        finalClient.setSocketValid(false);
                        key.cancel();
                    }
                    return ;
                }

                Client sourceClient = routingTable.get(finalClient);

                if (sourceClient != null)
                {
                    if (!sourceClient.isSocketValid())
                    {
                        try {
                            sendSessionRejectMessage(socketChannel, finalClient, selector);
                        } catch (IOException ex) {
                            Logger.logError("Sending session reject message to (" + finalClient.getName() + ") failed: " + ex.getMessage());
                            finalClient.setSocketValid(false);
                            key.cancel();
                        }
                    }
                    else if (sourceClient.messageComplete())
                    {
                        try {
                            forwardMessage(socketChannel, finalClient, sourceClient, selector);
                        } catch (IOException ex) {
                            Logger.logError("Forwarding data to (" + finalClient.getName() + ") failed: " + ex.getMessage());
                            finalClient.setSocketValid(false);
                            key.cancel();
                        }
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
        } catch (IOException ex)
        {
            Logger.logError("Server failed: " + ex.getMessage());
        }
    }
}
