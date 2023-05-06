package engineFIX;

import org.junit.Test;

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
        assertFalse(parser.isComplete());
    }

    @Test
    public void testBodyLength() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "9=77");
        assertEquals(77, parser.getBodyLength());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testMsgType() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "35=ThisTheMessageType");
        assertEquals("ThisTheMessageType", parser.getMsgType());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testOrderQty() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "38=455");
        assertEquals(455, parser.getOrderQty());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testPrice() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "44=485");
        assertEquals(485, parser.getPrice());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testSenderCompID1()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "49=me");
        assertEquals("me", parser.getSenderCompID());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testSenderCompID2()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "49=meAgain");
        assertEquals("meAgain", parser.getSenderCompID());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testSenderSubID() {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "50=BblablaTestID");
        assertEquals("BblablaTestID", parser.getSenderSubID());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testSide1()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "54=1");
        assertEquals("buy", parser.getSide());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testSide2()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "54=2");
        assertEquals("sell", parser.getSide());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testSymbol1()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "55=StockName1");
        assertEquals("StockName1", parser.getSymbol());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testSymbol2()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "55=apple");
        assertEquals("apple", parser.getSymbol());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testTargetCompID1()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "56=nasdaq");
        assertEquals("nasdaq", parser.getTargetCompID());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testTargetCompID2()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "56=londonStockExchange");
        assertEquals("londonStockExchange", parser.getTargetCompID());
        assertFalse(parser.isComplete());
    }

    @Test
    public void testChecksum()
    {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "10=777");
        assertEquals(777, parser.getCheckSum());
        assertTrue(parser.isComplete());
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

        tag = "49=me";
        writeString(parser, tag);
        assertEquals("me", parser.getSenderCompID());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "50=DAP1sASA";
        writeString(parser, tag);
        assertEquals("DAP1sASA", parser.getSenderSubID());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag= "54=1";
        writeString(parser, tag);
        assertEquals("buy", parser.getSide());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "55=APLLE";
        writeString(parser, tag);
        assertEquals("APLLE", parser.getSymbol());
        assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "56=nasdaq";
        writeString(parser, tag);
        assertEquals("nasdaq", parser.getTargetCompID());
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