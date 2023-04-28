import engineFIX.EngineFIX;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        EngineFIX parser = new EngineFIX();
        int bodyLength = 0;
        // Create file.
        File file = new File("test.txt");

        // Write to file.
        FileOutputStream fout = new FileOutputStream(file);
        DataOutputStream dout = new DataOutputStream(fout);

        // Begin string
        dout.writeUTF("8=Fix 4.8");
        dout.writeByte(0x1);

        // Body Length.
        dout.writeUTF("9=69");
        dout.writeByte(0x1);

        // Message Type.
        String str = "35=MESSAGETYPE";
        dout.writeUTF(str);
        dout.writeByte(0x1);
        bodyLength += str.length() + 1;

        // Order quantity.
        str = "38=444";
        dout.writeUTF(str);
        dout.writeByte(0x1);
        bodyLength += str.length() + 1;

        // Price.
        str = "44=14888";
        dout.writeUTF(str);
        dout.writeByte(0x1);
        bodyLength += str.length() + 1;

        // Sender SubID
        str = "50=ASASASUBIDsasa";
        dout.writeUTF(str);
        dout.writeByte(0x1);
        bodyLength += str.length() + 1;

        // CheckSum.
        str = "10=ACheckSum";
        dout.writeUTF(str);
        dout.writeByte(0x1);

        // Write to file.
        FileInputStream fin = new FileInputStream(file);
        DataInputStream din = new DataInputStream(fin);

        parser.consume(din);
    }
}
