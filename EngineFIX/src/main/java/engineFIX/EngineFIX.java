package engineFIX;

import java.util.ArrayList;

public class EngineFIX {
    private static final String supportedFixVersion = "FIX.4.4";
    private static final char fixDelimiter = 0x1; // (SOH) character.

    // This is where we store the raw data of the message.
    private ArrayList<Byte> rawData;

    // Tag 8: FIX protocol version.
    private String  beginString;

    // Tag 9: Number of bytes in message body.
    private int     bodyLength;

    // Tag 10: FIX Message Checksum.
    private int      checkSum;

    // Tag 35: Identifies FIX message type.
    private String  msgType;

    // Tag 38: Order quantity for sale and buy.
    private int     orderQty;

    // Tag 44: Price per single unit.
    private int     price;

    // Tag 49: Identifies entity sending the message.
    private String senderCompID;

    // Tag 50: we will use this for the 6 digits ID.
    private String   senderSubID;

    // Tag 54: Side of order. (1 = Buy, 2 = Sell)
    private String side;

    // Tag 55: This tag contains the Group Code for the instrument.
    private String symbol;

    // Tag 56: Identifies entity receiving the message.
    private String   targetCompID;

    public int      asciiSum;

    private boolean complete;

    private int bytesRead;

    private String lastParsedTag;

    private boolean valid;

    public EngineFIX()
    {
        this.valid = true;
        this.complete = false;
        this.asciiSum = 0;
        this.rawData = new ArrayList<>();
    }

    public ArrayList<Byte> getRawData() {
        return rawData;
    }

    public String getBeginString() {
        return beginString;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public String getMsgType() {
        return msgType;
    }

    public int getOrderQty() {
        return orderQty;
    }

    public int getPrice() {
        return price;
    }

    public String getSenderSubID() {
        return senderSubID;
    }

    public int getCheckSum() {
        return checkSum;
    }

    public boolean isComplete() {
        return complete;
    }

    public int getBytesRead() {
        return bytesRead;
    }

    public String getTargetCompID() {
        return targetCompID;
    }

    public String getSenderCompID() {
        return senderCompID;
    }

    public int getAsciiSum() {
        return asciiSum % 256;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public boolean isValid()
    {
        return valid;
    }

    public static String getFixRejectMessage(String uniqueId)
    {
        String beginString = "8=" + supportedFixVersion + fixDelimiter;

        // j means reject. (case sensitive)
        String msgType = "35=j" + fixDelimiter;

        String senderSubID = "50=" + uniqueId + fixDelimiter;

        int contentLength = msgType.length() + senderSubID.length();
        String bodyLength = "9=" + contentLength + fixDelimiter;

        int checksum = EngineFIX.calculateCheckSum(beginString + bodyLength + msgType + senderSubID) % 256;
        String checksumStr = "10=" + checksum + fixDelimiter;
        return beginString + bodyLength + senderSubID + msgType + checksumStr;
    }

    public static String constructSuccessMessage(String uniqueId)
    {
        String beginString = "8=" + supportedFixVersion + fixDelimiter;

        // s means Success. (case sensitive)
        String msgType = "35=s" + fixDelimiter;

        String senderSubID = "50=" + uniqueId + fixDelimiter;

        int contentLength = msgType.length() + senderSubID.length();
        String bodyLength = "9=" + contentLength + fixDelimiter;

        int checksum = EngineFIX.calculateCheckSum(beginString + bodyLength + msgType + senderSubID) % 256;
        String checksumStr = "10=" + checksum + fixDelimiter;
        return beginString + bodyLength + senderSubID + msgType + checksumStr;
    }

    public static String constructIdentificationMessage(String uniqueId, String name)
    {
        String beginString = "8=" + supportedFixVersion + fixDelimiter;
        // A means logon. (case sensitive)
        String msgType = "35=A" + fixDelimiter;

        String senderSubID = "50=" + uniqueId + fixDelimiter;
        String senderCompID = "49=" + name + fixDelimiter;

        int contentLength = msgType.length() + senderSubID.length() + senderCompID.length();
        String bodyLength = "9=" + contentLength + fixDelimiter;

        int checksum = EngineFIX.calculateCheckSum(beginString + bodyLength + msgType + senderCompID + senderSubID) % 256;
        String checksumStr = "10=" + checksum + fixDelimiter;
        return beginString + bodyLength + msgType + senderCompID + senderSubID + checksumStr;
    }

    // Note: this method does not calculate the 0x1 (SOH) character.
    public static int calculateCheckSum(String str)
    {
        int sum = 0;
        char[] array = str.toCharArray();
        for (char c : array) sum += c;
        return sum;
    }

    public static byte[] toPrimitiveArray(ArrayList<Byte> bytes)
    {
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        for (Byte mbyte : bytes)
        {
            byteArray[i] = mbyte.byteValue();
            i++;
        }
        return byteArray;
    }

    public static Byte[] toObjectArray(byte[] data)
    {
        ArrayList<Byte> list = new ArrayList<>(data.length);
        for (byte datum : data)
            list.add(datum);
        return list.toArray(new Byte[0]);
    }

    private void parseTag(String tag) throws UnsupportedTagException, TagFormatException {
        String[] ar = tag.split("=");
        if (ar.length == 2)
        {
            lastParsedTag = ar[0] + "=" + ar[1];
            if (ar[0].compareTo("10") == 0)
            {
                this.checkSum = Integer.parseInt(ar[1]);
                this.complete = true;
            }
            else if (ar[0].compareTo("8") == 0)
            {
                this.beginString = ar[1];
            }
            else if (ar[0].compareTo("9") == 0)
            {
                this.bodyLength = Integer.parseInt(ar[1]);
            }
            else if (ar[0].compareTo("35") == 0)
            {
                // 2 bytes for the "=" and 0x1 (SOH) characters.
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.msgType = ar[1];
            }
            else if (ar[0].compareTo("38") == 0)
            {
                // 2 bytes for the "=" and 0x1 (SOH) characters.
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.orderQty = Integer.parseInt(ar[1]);
            }
            else if (ar[0].compareTo("44") == 0)
            {
                // 2 bytes for the "=" and 0x1 (SOH) characters.
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.price = Integer.parseInt(ar[1]);
            }
            else if (ar[0].compareTo("49") == 0)
            {
                // 2 bytes for the "=" and 0x1 (SOH) characters.
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.senderCompID = ar[1];
            }
            else if (ar[0].compareTo("50") == 0)
            {
                // 2 bytes for the "=" and 0x1 (SOH) characters.
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.senderSubID = ar[1];
            }
            else if (ar[0].compareTo("54") == 0)
            {
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                if (ar[1].compareTo("1") == 0)
                    this.side = "buy";
                else if (ar[1].compareTo("2") == 0)
                    this.side = "sell";
            }
            else if (ar[0].compareTo("55") == 0)
            {
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.symbol = ar[1];
            }
            else if (ar[0].compareTo("56") == 0)
            {
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.targetCompID = ar[1];
            }
            else
            {
                valid = false;
                throw new UnsupportedTagException(lastParsedTag);
            }

            // calculate Sum.
            if (ar[0].compareTo("10") != 0)
            {
                if (this.asciiSum == -1)
                    this.asciiSum = 0;
                this.asciiSum += EngineFIX.calculateCheckSum(ar[0] + "=" + ar[1]);
                this.asciiSum += 0x1;
            }
        } else
        {
            lastParsedTag = ar[0];
            valid = false;
            throw new TagFormatException(lastParsedTag);
        }
    }

    public void consume(Byte[] data) throws UnsupportedTagException, TagFormatException, BadTagValueException {
        ArrayList<Byte> bytes = new ArrayList<>();

        try {
            for (Byte mbyte : data)
            {
                rawData.add(mbyte);
                // 0x1 is the SOH character.
                if (mbyte == 0x1 || mbyte == '\n')
                {
                    byte[] byteArray = toPrimitiveArray(bytes);
                    parseTag(new String(byteArray));
                    bytes.clear();
                    continue ;
                }
                bytes.add(mbyte);
            }
        } catch (NumberFormatException e)
        {
            valid = false;
            throw new TagFormatException(lastParsedTag);
        }

        if (isComplete())
        {
            if (bytesRead != getBodyLength())
            {
                valid = false;
                throw new BadTagValueException("9=" + getBodyLength());
            }
            if (checkSum != getAsciiSum())
            {
                valid = false;
                throw new BadTagValueException("10=" + getCheckSum());
            }
        }
    }

    public boolean isReject()
    {
        return (this.msgType != null && this.msgType.compareTo("j") == 0);
    }

    public boolean isSuccess()
    {
        return (this.msgType != null && this.msgType.compareTo("s") == 0);
    }
}
