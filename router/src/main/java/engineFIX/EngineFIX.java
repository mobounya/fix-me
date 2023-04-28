package engineFIX;

import java.io.DataInputStream;
import java.text.StringCharacterIterator;
import java.util.ArrayList;

public class EngineFIX {
    // Tag 8: FIX protocol version.
    private String  beginString;

    // Tag 9: Number of bytes in message body.
    private int     bodyLength;

    // Tag 35: Identifies FIX message type.
    private String  msgType;

    // Tag 38: Order quantity for sale and buy.
    private int     orderQty;

    // Tag 44: Price per single unit.
    private int     price;

    // Tag 50: we will use this for the 6 digits ID.
    private String   senderSubID;

    // Tag 10: FIX Message Checksum.
    private int      checkSum;
    private int         asciiSum;

    private boolean complete;

    private int bytesRead;

    public EngineFIX()
    {
        this.asciiSum = -1;
    }

    public EngineFIX(DataInputStream in)
    {
        this.asciiSum = -1;
        consume(in);
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

    // Note: this method does not calculate the 0x1 (SOH) character.
    public static int calculateCheckSum(String str)
    {
        int sum = 0;
        char[] array = str.toCharArray();
        for (char c : array) sum += c;
        return sum;
    }

    private void parseTag(String tag)
    {
        String[] ar = tag.split("=");
        if (ar.length == 2)
        {
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
            else if (ar[0].compareTo("50") == 0)
            {
                // 2 bytes for the "=" and 0x1 (SOH) characters.
                this.bytesRead += ar[0].length() + ar[1].length() + 2;
                this.senderSubID = ar[1];
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
            System.out.println("Waa hya ach wa9e3");
    }

    private byte[] toPrimitiveArray(ArrayList<Byte> bytes)
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

    public  boolean consume(DataInputStream in) {
        ArrayList<Byte> bytes = new ArrayList<>();

        try {
            in.skip(2);
            while (in.available() > 0)
            {
                byte byteRead = in.readByte();
                // 0x1 is the SOH character.
                if (Byte.toUnsignedInt(byteRead) == 0x1)
                {
                    in.skip(2);
                    byte[] byteArray = toPrimitiveArray(bytes);
                    parseTag(new String(byteArray));
                    bytes.clear();
                    continue ;
                }
                bytes.add(byteRead);
            }
            if (this.complete)
            {
                assert (bytesRead == bodyLength) : "Number of bytes read: " + bytesRead + " does not match message body length: " + bodyLength;
                assert ((asciiSum % 256) == checkSum) : "CheckSums don't match";
            }
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        return true;
    }
}
