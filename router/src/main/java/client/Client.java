package client;


import engineFIX.EngineFIX;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.FutureTask;

public abstract class Client implements Comparable<Client> {
    public EngineFIX                parser;
    private final String            uniqueID;
    private String                  name;
    private final String            clientType;
    private final InetSocketAddress remoteAddress;
    private final SocketChannel     socket;

    private boolean                 targetFound;
    private boolean                 valid;
    private boolean                 idSent;

    private int state;

    public static final int NEW = 0; // when Client first created / when calling resetClient.
    public static final int RUNNING = 2; // In the middle of a message, do not try to read yet.
    public static final int ESTABLISHED = 3; // targetFound & valid & idSent are all true.
    public static final int INVALID = 4; // Message is complete but the client is invalid.
    public static final int COMPLETED = 5; // Message is completed successfully.

    public Client()
    {
        this.state = Client.NEW;
        this.parser = null;
        this.uniqueID = null;
        this.remoteAddress = null;
        this.socket = null;
        this.targetFound = true;
        this.valid = true;
        this.idSent = false;
        this.clientType = null;
    }

    public Client(String uniqueID, InetSocketAddress address, SocketChannel socket, String clientType)
    {
        this.state = Client.NEW;
        this.parser = new EngineFIX();
        this.uniqueID = uniqueID;
        this.remoteAddress = address;
        this.socket = socket;
        this.name = null;
        this.valid = true;
        this.targetFound = true;
        this.idSent = false;
        this.clientType = clientType;
    }

    public static String generateRandomString(int len)
    {
        Random random = new Random();
        int min = 65;
        int max = 90;

        return random.ints(min, max + 1).limit(len).
                collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getTargetMarket()
    {
        return this.parser.getTargetCompID();
    }

    public String getName()
    {
        return this.name;
    }

    public SocketChannel getSocket() {
        return socket;
    }

    public void setMarketFound()
    {
        this.targetFound = true;
    }

    public void clearMarketFound()
    {
        this.targetFound = false;
    }

    public boolean isTargetFound()
    {
        return this.targetFound;
    }

    public void setValid(boolean val)
    {
        if (val)
            this.state = Client.COMPLETED;
        else
            this.state = Client.INVALID;
        this.valid = val;
    }

    public boolean isValid()
    {
        return this.valid;
    }

    public boolean isIdSent()
    {
        return this.idSent;
    }

    public void setIdSent()
    {
        this.idSent = true;
    }

    public void clearIdSent()
    {
        this.idSent = false;
    }

    public String getClientType() {
        return clientType;
    }

    public int getClientState()
    {
        return this.state;
    }

    public void setClientState(int newState)
    {
        this.state = newState;
    }

    public void read(byte[] data, int size)
    {
        if (size <= 0)
            return ;
        this.state = Client.RUNNING;
        try {
            Byte[] bytes = new Byte[size];

            int i = 0;
            for (; i < size; i++)
                bytes[i] = data[i];
            parser.consume(bytes);
            if (this.name == null && parser.getSenderCompID() != null)
                this.name = parser.getSenderCompID();
            if (this.parser.isComplete())
                this.state = Client.COMPLETED;
        } catch (Exception e)
        {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    public boolean messageComplete()
    {
        return this.parser.isComplete();
    }

    public int compareTo(Client o) {
        return this.uniqueID.compareTo(o.getUniqueID());
    }

    public void resetClient()
    {
        this.state = Client.NEW;
        this.parser = new EngineFIX();;
        this.name = null;
        this.valid = true;
        this.targetFound = true;
        this.idSent = false;
    }

    public void resetParser()
    {
        this.parser = new EngineFIX();
        this.targetFound = true;
        this.valid = true;
    }
}
