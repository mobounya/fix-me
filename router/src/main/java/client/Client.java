package client;


import engineFIX.EngineFIX;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

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

    public int                     cleaned;

    public Client()
    {
        this.cleaned = 0;
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
        this.cleaned = 0;
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
        int min = 33;
        int max = 126;

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

    public void read(byte[] data, int size)
    {
        if (size <= 0)
            return ;
        try {
            Byte[] bytes = new Byte[size];

            int i = 0;
            for (; i < size; i++)
                bytes[i] = data[i];
            parser.consume(bytes);
            if (this.name == null && parser.getSenderCompID() != null)
                this.name = parser.getSenderCompID();
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

    public void clean()
    {
        this.cleaned++;
        this.parser = new EngineFIX();
        this.targetFound = true;
        this.valid = true;
    }
}
