package client;

import engineFIX.EngineFIX;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

public abstract class Client implements Comparable<Client> {
    public  EngineFIX parser;
    private final String uniqueID;
    private String name;
    private final InetSocketAddress remoteAddress;
    private final SocketChannel socket;


    public Client()
    {
        this.parser = null;
        this.uniqueID = null;
        this.remoteAddress = null;
        this.socket = null;
    }

    public Client(String uniqueID, InetSocketAddress address, SocketChannel socket)
    {
        this.parser = new EngineFIX();
        this.uniqueID = uniqueID;
        this.remoteAddress = address;
        this.socket = socket;
        this.name = null;
    }

    public static String generateRandomString(int len)
    {
        Random random = new Random();
        int min = 33;
        int max = 126;

        return random.ints(min, max + 1).limit(len).toString();
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

    public void read(byte[] data, int size)
    {
        Byte[] bytes = new Byte[size];

        int i = 0;
        for (; i < size; i++)
            bytes[i] = data[i];
        parser.consume(bytes);
        if (this.name == null && parser.getSenderCompID() != null)
            this.name = parser.getSenderCompID();
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
        this.parser = new EngineFIX();
    }
}
