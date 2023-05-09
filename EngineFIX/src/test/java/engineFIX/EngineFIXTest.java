package engineFIX;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class EngineFIXTest {
    private void writeString(EngineFIX parser, String str) throws UnsupportedTagException, TagFormatException, BadTagValueException {
        Byte[] bytes = new Byte[str.length() + 1];

        int i = 0;
        for (; i < str.length(); i++)
            bytes[i] = (byte) str.charAt(i);
        bytes[i] = 0x1;
        parser.consume(bytes);
    }

    @Test
    public void testIncorrectFormatTag1() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();

        writeString(parser, "8=FIX 4.2");
        writeString(parser, "35=Sometype");

        Exception exception = Assert.assertThrows(TagFormatException.class, () -> {
            writeString(parser, "tags are not like this");
        });

        String expectedMessage = TagFormatException.getPrefix() + "tags are not like this";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testIncorrectFormatTag2() {
        EngineFIX parser = new EngineFIX();

        Exception exception = Assert.assertThrows(TagFormatException.class, () -> {
            writeString(parser, "8 FIX.4.2");
        });

        String expectedMessage = TagFormatException.getPrefix() + "8 FIX.4.2";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testIncorrectFormatTag3() {
        EngineFIX parser = new EngineFIX();

        Exception exception = Assert.assertThrows(TagFormatException.class, () -> {
            writeString(parser, "9=ShouldBeAnInteger");
        });

        String expectedMessage = TagFormatException.getPrefix() + "9=ShouldBeAnInteger";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testIncorrectFormatTag4() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();

        writeString(parser, "35=SomeType");

        Exception exception = Assert.assertThrows(TagFormatException.class, () -> {
            writeString(parser, "38=ShouldBeAnInteger");
        });

        String expectedMessage = TagFormatException.getPrefix() + "38=ShouldBeAnInteger";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testUnsupportedTag1()
    {
        EngineFIX parser = new EngineFIX();

        Exception exception = Assert.assertThrows(UnsupportedTagException.class, () -> {
            writeString(parser, "1337=there\'s not such tag in the FIX protocol");
        });

        String expectedMessage = UnsupportedTagException.getPrefix() + "1337=there\'s not such tag in the FIX protocol";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testUnsupportedTag2()
    {
        EngineFIX parser = new EngineFIX();

        Exception exception = Assert.assertThrows(UnsupportedTagException.class, () -> {
            writeString(parser, "0=there\'s not such tag in the FIX protocol");
        });

        String expectedMessage = UnsupportedTagException.getPrefix() + "0=there\'s not such tag in the FIX protocol";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testProtocolVersion() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "8=FIX 4.2");
        assertEquals("FIX 4.2", parser.getBeginString());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testBodyLength() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "9=77");
        assertEquals(77, parser.getBodyLength());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testMsgType() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "35=ThisTheMessageType");
        assertEquals("ThisTheMessageType", parser.getMsgType());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testOrderQty() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "38=455");
        assertEquals(455, parser.getOrderQty());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testPrice() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "44=485");
        assertEquals(485, parser.getPrice());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testSenderCompID1() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "49=me");
        assertEquals("me", parser.getSenderCompID());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testSenderCompID2() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "49=meAgain");
        assertEquals("meAgain", parser.getSenderCompID());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testSenderSubID() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "50=BblablaTestID");
        assertEquals("BblablaTestID", parser.getSenderSubID());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testSide1() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "54=1");
        assertEquals("buy", parser.getSide());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testSide2() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "54=2");
        assertEquals("sell", parser.getSide());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testSymbol1() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "55=StockName1");
        assertEquals("StockName1", parser.getSymbol());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testSymbol2() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "55=apple");
        assertEquals("apple", parser.getSymbol());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testTargetCompID1() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "56=nasdaq");
        assertEquals("nasdaq", parser.getTargetCompID());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testTargetCompID2() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        EngineFIX parser = new EngineFIX();
        writeString(parser, "56=londonStockExchange");
        assertEquals("londonStockExchange", parser.getTargetCompID());
        Assert.assertFalse(parser.isComplete());
    }

    @Test
    public void testWrongChecksum1()
    {
        EngineFIX parser = new EngineFIX();

        Exception exception = Assert.assertThrows(BadTagValueException.class, () -> {
            writeString(parser, "10=1");
        });

        String expectedMessage = BadTagValueException.getPrefix() + "10=1";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testWrongChecksum2() throws UnsupportedTagException, BadTagValueException, TagFormatException {
        EngineFIX parser = new EngineFIX();
        int expectedBytesRead = 0;
        int checksum = 0;

        String tag = "8=FIX 4.4";
        writeString(parser, tag);
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "35=SomeType";
        writeString(parser, tag);
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.

        tag = "9=" + expectedBytesRead;
        writeString(parser, tag);
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        Exception exception = Assert.assertThrows(BadTagValueException.class, () -> {
            writeString(parser, "10=1");
        });

        String expectedMessage = BadTagValueException.getPrefix() + "10=1";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testWrongBodyLength1() throws UnsupportedTagException, BadTagValueException, TagFormatException {
        EngineFIX parser = new EngineFIX();
        int expectedBytesRead = 0;
        int checksum = 0;

        String tag = "8=FIX 4.4";
        writeString(parser, tag);
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.

        tag = "35=SomeType";
        writeString(parser, tag);
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.

        tag = "9=1";
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;
        writeString(parser, "9=1");

        Exception exception = Assert.assertThrows(BadTagValueException.class, () -> {
            // This is the wrong checksum, however the parser will raise BadTagValueException for tag 9 first,
            // which is the expected behaviour in this test.
            writeString(parser, "10=4");
        });

        String expectedMessage = BadTagValueException.getPrefix() + "9=1";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testWholeMessage1() throws UnsupportedTagException, TagFormatException, BadTagValueException {
        int expectedBytesRead = 0;
        int checksum = 0;

        EngineFIX parser = new EngineFIX();

        String tag = "8=FIX 4.4";
        writeString(parser, tag);
        assertEquals("FIX 4.4", parser.getBeginString());
        Assert.assertFalse(parser.isComplete());
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "35=SomeType";
        writeString(parser, tag);
        assertEquals("SomeType", parser.getMsgType());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "38=329";
        writeString(parser, tag);
        assertEquals(329, parser.getOrderQty());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "44=69";
        writeString(parser, tag);
        assertEquals(69, parser.getPrice());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "49=me";
        writeString(parser, tag);
        assertEquals("me", parser.getSenderCompID());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "50=DAP1sASA";
        writeString(parser, tag);
        assertEquals("DAP1sASA", parser.getSenderSubID());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag= "54=1";
        writeString(parser, tag);
        assertEquals("buy", parser.getSide());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "55=APLLE";
        writeString(parser, tag);
        assertEquals("APLLE", parser.getSymbol());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "56=nasdaq";
        writeString(parser, tag);
        assertEquals("nasdaq", parser.getTargetCompID());
        Assert.assertFalse(parser.isComplete());
        expectedBytesRead += tag.length() + 1; // 1 byte for the 0x1 (SOH) char.
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "9=" + expectedBytesRead;
        writeString(parser, tag);
        assertEquals(expectedBytesRead, parser.getBodyLength());
        Assert.assertFalse(parser.isComplete());
        checksum += EngineFIX.calculateCheckSum(tag) + 0x1;

        tag = "10=" + (checksum % 256);
        writeString(parser, tag);

        assertEquals((checksum % 256), parser.getCheckSum());
        assertEquals(expectedBytesRead, parser.getBytesRead());
        Assert.assertTrue(parser.isComplete());
    }
}
