import engineFIX.BadTagValueException;
import engineFIX.EngineFIX;
import engineFIX.TagFormatException;
import engineFIX.UnsupportedTagException;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Server {
    private static final int marketPort = 5001;
    private static final String host = "localhost";
    private static final String FIXVersion = "FIX 4.2";
    private static final char delimiter = 0x1;
    private static final String marketName = "nasdaq";

    private InputStream inputStream;
    private OutputStream outputStream;

    private String uniqueId;
    private EngineFIX parser;

    Server()
    {
        this.parser = new EngineFIX();
        this.uniqueId = null;
    }

    public void connect() throws IOException {
        Socket socket = new Socket(host, marketPort);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    public void sendRejectMessage() throws IOException {
        String message = EngineFIX.getFixRejectMessage(this.uniqueId);
        outputStream.write(message.getBytes());
    }

    public void sendSuccessMessage() throws IOException {
        String message = EngineFIX.constructSuccessMessage(this.uniqueId);
        outputStream.write(message.getBytes());
    }

    public void sendIdentificationMessage() throws IOException {
        String message = EngineFIX.constructIdentificationMessage(uniqueId, marketName);
        outputStream.write(message.getBytes());
    }

    private void readUniqueId() throws IOException, UnsupportedTagException, BadTagValueException, TagFormatException {
        while (!parser.isComplete())
        {
            byte[] res = new byte[1000];
            System.out.println("Reading from socket input stream...");
            int bytesRead = this.inputStream.read(res);
            if (bytesRead > 0)
            {
                res = ByteBuffer.wrap(res).slice(0, bytesRead).array();
                parser.consume(EngineFIX.toObjectArray(res));
                if (parser.isComplete())
                {
                    String assignedUniqueId = parser.getSenderSubID();
                    if (assignedUniqueId == null)
                    {
                        System.err.println("Response need to include a unique id from the router.");
                        System.exit(1);
                    }
                    this.uniqueId = assignedUniqueId;
                    this.parser = new EngineFIX();
                    break ;
                }
            }
        }
    }

    private void readResponse() throws IOException, UnsupportedTagException, BadTagValueException, TagFormatException {
        while (!parser.isComplete())
        {
            byte[] res = new byte[1000];
            int bytesRead = this.inputStream.read(res);
            if (bytesRead > 0)
            {
                res = ByteBuffer.wrap(res).slice(0, bytesRead).array();
                parser.consume(EngineFIX.toObjectArray(res));
                if (parser.isComplete())
                {
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Press enter to continue");
                    String str = buffer.readLine();
                    if (parser.getSymbol().compareTo("apple") == 0)
                    {
                        sendSuccessMessage();
                        System.out.println("Sent success message");
                    }
                    else
                    {
                        sendRejectMessage();
                        System.out.println("Sent reject message");
                    }
                    parser = new EngineFIX();
                }
            }
        }
    }

    public void start()
    {
        try {
            connect();
            readUniqueId();
            System.out.println("Assigned unique id: " + uniqueId);
            sendIdentificationMessage();
            while (true)
            {
                readResponse();
                System.exit(1);
            }
        } catch (Exception e)
        {
            System.out.println("Market: " + e.getMessage());
        }
    }
}
