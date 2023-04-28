package engineFIX;

import java.io.DataInputStream;
import java.util.ArrayList;

public class EngineFIX {
    // Tag 8: FIX protocol version.
    public String  beginString;

    // Tag 9: Number of bytes in message body.
    public int     bodyLength;

    // Tag 35: Identifies FIX message type.
    public String  MsgType;

    // Tag 38: Order quantity for sale and buy.
    public int     orderQty;

    // Tag 44: Price per single unit.
    public int     price;

    // Tag 50: we will use this for the 6 digits ID.
    public String   SenderSubID;

    // Tag 10: FIX Message Checksum.
    public String     checkSum;

    public boolean complete;

    public int bytesRead;

    public EngineFIX()
    {
        this.complete = false;
        this.bytesRead = 0;
    }

    public EngineFIX(DataInputStream in)
    {
        this.complete = false;
        this.bytesRead = 0;
        consume(in);
    }

    private void parseTag(String tag)
    {
        String[] ar = tag.split("=");
        if (ar.length == 2)
        {
            if (ar[0].compareTo("10") == 0)
            {
                this.checkSum = ar[1];
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
                this.MsgType = ar[1];
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
                this.SenderSubID = ar[1];
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
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        return true;
    }
}
