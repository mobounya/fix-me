package broker;

import engineFIX.BadTagValueException;
import engineFIX.EngineFIX;
import engineFIX.TagFormatException;
import engineFIX.UnsupportedTagException;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Broker {
    private static final int brokerPort = 5000;
    private static final String host = "localhost";
    private static final String FIXVersion = "FIX 4.2";
    private static final char delimiter = 0x1;

    private InputStream inputStream;
    private OutputStream outputStream;

    private EngineFIX parser;
    private String uniqueId;
    private final String name;

    public Broker(String name)
    {
        this.uniqueId = null;
        this.parser = new EngineFIX();
        this.name = name;
    }

    private void connect() throws IOException {
        Socket socket = new Socket(host, brokerPort);
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    private static boolean isInteger(String str)
    {
        try {
            Integer.decode(str);
        } catch (NumberFormatException exception) {
            return false;
        }
        return true;
    }

    public static int calculateCheckSum(String str)
    {
        int sum = 0;
        char[] array = str.toCharArray();
        for (char c : array) sum += c;
        return sum;
    }

    private String constructFixOrderMessage(String market, String symbol, String operationType, String quantity, String price)
    {
        // Part of the header.
        String beginStringTag = "8=" + FIXVersion + delimiter;

        // D means new Order.
        String msgType = "35=" + "D" + delimiter;

        // Part of the body.
        String targetCompID = "56=" + market + delimiter;

        String senderSubID = "50=" + uniqueId + delimiter;

        String senderCompID = "49=" + name + delimiter;

        String sideTag = "54=";
        if (operationType.compareTo("buy") == 0)
            sideTag = sideTag.concat("1");
        else if (operationType.compareTo("sell") == 0)
            sideTag = sideTag.concat("2");
        sideTag += delimiter;

        String symbolTag = "55=" + symbol + delimiter;
        String orderQtyTag = "38=" + quantity + delimiter;
        String priceTag = "44=" + price + delimiter;

        // Part of the header.
        String bodyLength = "9=" + String.valueOf(senderCompID.length() + msgType.length() + senderSubID.length() + sideTag.length() + symbolTag.length() + orderQtyTag.length() + priceTag.length() + targetCompID.length()) + delimiter;

        // Message (without the checksum).
        String FIXMessage = beginStringTag + bodyLength + msgType + senderCompID + senderSubID + targetCompID + symbolTag + sideTag + orderQtyTag + priceTag;

        // Calculate the checksum and append it to message.
        int checksum = Broker.calculateCheckSum(FIXMessage) % 256;
        return FIXMessage.concat("10=" + String.valueOf(checksum) + delimiter);
    }

    private void sendRequest(String message) throws IOException {
        this.outputStream.write(message.getBytes());
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
            System.out.println("Reading data from input stream");
            int bytesRead = this.inputStream.read(res);
            if (bytesRead > 0)
            {
                res = ByteBuffer.wrap(res).slice(0, bytesRead).array();
                parser.consume(EngineFIX.toObjectArray(res));
                if (parser.isComplete())
                {
                    System.out.println("Parser is complete");
                    if (parser.isReject())
                        System.out.println("Transaction rejected");
                    if (parser.isSuccess())
                        System.out.println("Transaction success");
                    this.parser = new EngineFIX();
                    break ;
                }
            }
        }
    }

    public void start() {
        try {
            this.connect();
            // Read unique id.
            readUniqueId();
            System.out.println("Unique id: " + uniqueId);
            while (true)
            {
                BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));

                String market;
                String prompt = "Enter target market name: ";
                do {
                    System.out.print(prompt);
                    market = userInputReader.readLine();
                    prompt = "Target market name can't be empty\nEnter target market name: ";
                } while (market.length() <= 0);

                String instrument;
                prompt = "Enter instrument name: ";
                do {
                    System.out.print(prompt);
                    instrument = userInputReader.readLine();
                    prompt = "Instrument name can't be empty\nEnter instrument name: ";
                } while (instrument.length() <= 0);

                String type;
                prompt = "Enter order type (Buy/Sell): ";
                do {
                    System.out.print(prompt);
                    type = userInputReader.readLine().toLowerCase();
                    prompt = "Order type should be either Buy or Sell\nEnter order type (Buy/Sell): ";
                } while (type.compareTo("buy") != 0 && type.compareTo("sell") != 0);


                String quantity;
                prompt = "Enter quantity: ";
                do {
                    System.out.print(prompt);
                    quantity = userInputReader.readLine();
                    prompt = "Please enter a valid integer for quantity\nEnter quantity: ";
                } while (!isInteger(quantity));

                String price;
                prompt = "Enter price: ";
                do {
                    System.out.print(prompt);
                    price = userInputReader.readLine();
                    prompt = "Please enter a valid integer for price\nEnter price: ";
                } while (!isInteger(price));

                String FIXMessage = constructFixOrderMessage(market, instrument, type, quantity, price);
                sendRequest(FIXMessage);
                readResponse();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
