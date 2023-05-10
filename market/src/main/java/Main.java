import engineFIX.BadTagValueException;
import engineFIX.EngineFIX;
import engineFIX.TagFormatException;
import engineFIX.UnsupportedTagException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Main {
    private static final int marketPort = 5001;
    private static final String host = "localhost";
    private static final String FIXVersion = "FIX 4.2";
    private static final char delimiter = 0x1;
    private static final String marketName = "nasdaq";
    private static InputStream inputStream;
    private static OutputStream outputStream;

    public static void connect() throws IOException {
        Socket socket = new Socket(host, marketPort);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    public static void sendIdentificationMessage() throws IOException {
        String beginString = "8=" + FIXVersion + delimiter;
        String msgType = "35=" + "identification" + delimiter;
        String senderCompID = "49=" + marketName + delimiter;
        String bodyLength = "9=" + (senderCompID.length() + msgType.length()) + delimiter;
        int checksum = EngineFIX.calculateCheckSum(beginString + bodyLength + msgType + senderCompID) % 256;
        String checksumTag = "10=" + String.valueOf(checksum) + delimiter;
        String identificationMessage = beginString + bodyLength + msgType + senderCompID + checksumTag;
        outputStream.write(identificationMessage.getBytes());
    }

    public static void sendRejectMessage() throws IOException {
        String message = EngineFIX.getFixRejectMessage();
        outputStream.write(message.getBytes());
    }

    public static void main(String[] args) {
        try {
            connect();
            sendIdentificationMessage();
            EngineFIX parser = new EngineFIX();
            while (true)
            {
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0)
                {
                    Byte[] objectBuffer = EngineFIX.toObjectArray(ByteBuffer.wrap(buffer).slice(0, bytesRead).array());
                    parser.consume(objectBuffer);
                }
                if (parser.isComplete())
                {
                    System.out.println("Message completed");
                    sendRejectMessage();
                    parser = new EngineFIX();
                }
            }
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}
