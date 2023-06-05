import logger.Logger;
import engineFIX.BadTagValueException;
import engineFIX.EngineFIX;
import engineFIX.TagFormatException;
import engineFIX.UnsupportedTagException;
import market.Instrument;
import market.Market;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Server {
    private static final int marketPort = 5001;
    private static final String host = "localhost";
    private static final String FIXVersion = "FIX 4.2";
    private static final char delimiter = 0x1;

    private final String marketName;

    private InputStream inputStream;
    private OutputStream outputStream;

    private String uniqueId;
    private EngineFIX parser;

    private Market market;

    Server(String name)
    {
        this.marketName = name;
        this.parser = new EngineFIX();
        this.uniqueId = null;
        this.market = new Market();
    }

    public void connect() throws IOException {
        Socket socket = new Socket(host, marketPort);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    public void sendRejectMessage() throws IOException {
        String message = EngineFIX.getFixBusinessRejectMessage(uniqueId);
        outputStream.write(message.getBytes());
    }

    public void sendSuccessMessage() throws IOException {
        String message = EngineFIX.constructSuccessMessage(this.uniqueId);
        outputStream.write(message.getBytes());
    }

    public void sendIdentificationMessage() throws IOException {
        String message = EngineFIX.constructIdentificationMessage(uniqueId, marketName, marketName);
        outputStream.write(message.getBytes());
    }

    private void readUniqueId() throws IOException, UnsupportedTagException, BadTagValueException, TagFormatException {
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
                    if (parser.isSessionReject())
                    {
                        Logger.logError("Identification rejected");
                        System.exit(1);
                    }
                    this.uniqueId = parser.getSenderSubID();
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
                    String instrumentName = parser.getSymbol();
                    Instrument instrument = new Instrument(instrumentName);
                    int quantity = parser.getOrderQty();
                    int price = parser.getPrice();
                    String side = parser.getSide();

                    if (quantity == 0 || instrumentName == null || side == null)
                    {
                        sendRejectMessage();
                        Logger.logError("Invalid buy/sell request");
                        this.parser = new EngineFIX();
                        break ;
                    }

                    // if price is 0, we will buy or sell the instrument at market price.
                    if (price == 0)
                    {
                        Instrument marketInstrument = market.getInstrumentData(instrument);
                        if (marketInstrument != null)
                            price = marketInstrument.getPrice();
                    }

                    if (side.equals("buy"))
                    {
                        if (market.buy(instrument, price, quantity))
                        {
                            sendSuccessMessage();
                            Logger.logSuccess("Successfully bought " + quantity + " of " + instrumentName + " at " + price);
                        }
                        else
                        {
                            sendRejectMessage();
                            Logger.logError("Failed to buy " + quantity + " of (" + instrumentName + ") at " + price);
                        }
                    } else if (side.equals("sell"))
                    {
                        int actualSellingPrice = market.sell(instrument, price, quantity);
                        sendSuccessMessage();
                        Logger.logSuccess("Successfully sold " + quantity + " of " + instrumentName + " at " + actualSellingPrice);
                    }
                    parser = new EngineFIX();
                    break ;
                }
            }
        }
    }

    public void start()
    {
        System.out.println("Started " + marketName + " market");
        try {
            connect();
            sendIdentificationMessage();
            readUniqueId();
            System.out.println("Assigned id: " + uniqueId);
            while (true)
                readResponse();
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}
