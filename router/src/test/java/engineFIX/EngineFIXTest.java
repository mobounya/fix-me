package engineFIX;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class EngineFIXTest {
    private void writeString(EngineFIX parser, String str) {
        Byte[] bytes = new Byte[str.length() + 1];

        int i = 0;
        for (; i < str.length(); i++)
            bytes[i] = (byte) str.charAt(i);
        bytes[i] = 0x1;
        parser.consume(bytes);
    }

    @Test
    public void testProtocolVersion() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "8=FIX 4.2");
        assertEquals("FIX 4.2", parser.getBeginString());
    }

    @Test
    public void testBodyLength() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "9=77");
        assertEquals(77, parser.getBodyLength());
    }

    @Test
    public void testMsgType() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "35=ThisTheMessageType");
        assertEquals("ThisTheMessageType", parser.getMsgType());
    }

    @Test
    public void testOrderQty() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "38=455");
        assertEquals(455, parser.getOrderQty());
    }

    @Test
    public void testPrice() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "44=485");
        assertEquals(485, parser.getPrice());
    }

    @Test
    public void testSenderSubID() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "50=BblablaTestID");
        assertEquals("BblablaTestID", parser.getSenderSubID());
    }

    @Test
    public void testWholeMessage1() {
        int expectedBytesRead = 0;
        int checksum = 0;

        EngineFIX parser = new EngineFIX();

        String tag = "8=FIX 4.4";
        writeString(parser, tag);
        assertEquals("FIX 4.4", parser.getBeginString());
        assertFalse(parser.isComplete());
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "35=SomeType";
        writeString(parser, tag);
        assertEquals("SomeType", parser.getMsgType());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "38=329";
        writeString(parser, tag);
        assertEquals(329, parser.getOrderQty());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "44=69";
        writeString(parser, tag);
        assertEquals(69, parser.getPrice());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "50=DAP1sASA";
        writeString(parser, tag);
        assertEquals("DAP1sASA", parser.getSenderSubID());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "9=" + expectedBytesRead;
        writeString(parser, tag);
        assertEquals(expectedBytesRead, parser.getBodyLength());
        assertFalse(parser.isComplete());
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "10=" + (checksum % 256);
        writeString(parser, tag);
        assertEquals((checksum % 256), parser.getCheckSum());
        assertEquals(expectedBytesRead, parser.getBytesRead());
        assertTrue(parser.isComplete());
    }
}