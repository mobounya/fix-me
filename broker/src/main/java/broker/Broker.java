package broker;

import java.io.*;
import java.net.Socket;

public class Broker {
    private static final int brokerPort = 5000;
    private static final String host = "localhost";
    private static final String FIXVersion = "FIX 4.2";
    private static final char delimiter = 0x1;

    private InputStream inputStream;
    private OutputStream outputStream;

    private void connect() throws IOException {
        Socket socket = new Socket(host, brokerPort);
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    public static int calculateCheckSum(String str)
    {
        int sum = 0;
        char[] array = str.toCharArray();
        for (char c : array) sum += c;
        return sum;
    }

    private static String constructFixMessage(String market, String operationType, String quantity, String price)
    {
        // Part of the header.
        String beginStringTag = "8=" + FIXVersion + delimiter;

        // Part of the body.
        String targetCompID = "56=" + market + delimiter;

        String sideTag = "54=";
        if (operationType.compareTo("buy") == 0)
            sideTag = sideTag.concat("1");
        else if (operationType.compareTo("sell") == 0)
            sideTag = sideTag.concat("2");
        sideTag += delimiter;

        String orderQtyTag = "38=" + quantity + delimiter;
        String priceTag = "44=" + price + delimiter;

        // Part of the header.
        String bodyLength = "9=" + String.valueOf(sideTag.length() + orderQtyTag.length() + priceTag.length() + targetCompID.length()) + delimiter;

        // Message (without the checksum).
        String FIXMessage = beginStringTag + bodyLength + targetCompID + sideTag + orderQtyTag + priceTag;

        // Calculate the checksum and append it to message.
        int checksum = Broker.calculateCheckSum(FIXMessage);
        return FIXMessage.concat("10=" + String.valueOf(checksum) + delimiter);
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

    public void start() {
        try {
            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));

            String market;
            String prompt = "Enter target market name: ";
            do {
                System.out.print(prompt);
                market = userInputReader.readLine();
                prompt = "Target market name can't be empty\nEnter target market name: ";
            } while (market.length() <= 0);

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

            String FIXMessage = Broker.constructFixMessage(market, type, quantity, price);
            FIXMessage = FIXMessage.replaceAll("\\x01", "|");
            System.out.println(FIXMessage);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
