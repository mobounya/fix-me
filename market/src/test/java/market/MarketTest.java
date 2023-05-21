package market;

import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class MarketTest {
    @Test
    public void testSuccessfulBuy1() {
        Market market = new Market();
        Instrument instrument = new Instrument("apple");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        boolean purchased = market.buy(instrument, 5, 50);
        int newQuantity = market.getInstrumentData(instrument).getQuantity();
        assertTrue(purchased);
        assertEquals(newQuantity, (originalQuantity - 50));
    }

    @Test
    public void testSuccessfulBuy2() {
        Market market = new Market();
        Instrument instrument = new Instrument("microsoft");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        boolean purchased = market.buy(instrument, 12, 50);
        int newQuantity = market.getInstrumentData(instrument).getQuantity();
        assertTrue(purchased);
        assertEquals(newQuantity, (originalQuantity - 50));
    }

    @Test
    public void testNotEnoughQuantity1() {
        Market market = new Market();
        Instrument instrument = new Instrument("oracle");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        boolean purchased = market.buy(instrument, 17, originalQuantity + 1);
        int newQuantity = market.getInstrumentData(instrument).getQuantity();
        assertFalse(purchased);
        assertEquals(newQuantity, originalQuantity);
    }

    @Test
    public void testNotEnoughQuantity2() {
        Market market = new Market();
        Instrument instrument = new Instrument("apple");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        int originalPrice = market.getInstrumentData(instrument).getPrice();
        boolean purchased = market.buy(instrument, originalPrice, originalQuantity + 1);
        int newQuantity = market.getInstrumentData(instrument).getQuantity();
        assertFalse(purchased);
        assertEquals(newQuantity, originalQuantity);
    }

    @Test
    public void testPriceTooLow1() {
        Market market = new Market();
        Instrument instrument = new Instrument("google");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        boolean purchased = market.buy(instrument, 16, 17);
        int newQuantity = market.getInstrumentData(instrument).getQuantity();
        assertFalse(purchased);
        assertEquals(newQuantity, originalQuantity);
    }

    @Test
    public void testPriceTooLow2() {
        Market market = new Market();
        Instrument instrument = new Instrument("microsoft");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        boolean purchased = market.buy(instrument, 11,17);
        int newQuantity = market.getInstrumentData(instrument).getQuantity();
        assertFalse(purchased);
        assertEquals(newQuantity, originalQuantity);
    }

    @Test
    public void testNoInstrument1() {
        Market market = new Market();
        Instrument instrument = new Instrument("companydoesnotexist");
        Instrument marketInstrument = market.getInstrumentData(instrument);
        boolean purchased = market.buy(instrument, 20, 20);
        assertNull(marketInstrument);
        assertFalse(purchased);
    }

    @Test
    public void testNoInstrument2() {
        Market market = new Market();
        Instrument instrument = new Instrument("companydoesnotexistagain");
        Instrument marketInstrument = market.getInstrumentData(instrument);
        boolean purchased = market.buy(instrument, 20, 20);
        assertNull(marketInstrument);
        assertFalse(purchased);
    }

    @Test
    public void testSellNewInstrument1() {
        Market market = new Market();
        Instrument myNewInstrument = new Instrument("MyNewCompany1");
        int price = market.sell(myNewInstrument, 15, 100);
        assertEquals(price, 15);
        assertEquals(15, market.getInstrumentData(myNewInstrument).getPrice());
        assertEquals(100, market.getInstrumentData(myNewInstrument).getQuantity());
    }

    @Test
    public void testSellNewInstrument2() {
        Market market = new Market();
        Instrument myNewInstrument = new Instrument("NewCompany");
        market.sell(myNewInstrument, 15, 100);
        // Buy all the new instruments to make the supply 0.
        market.buy(myNewInstrument, 15, 100);
        assertEquals(0, market.getInstrumentData(myNewInstrument).getQuantity());
        // Sell the new instrument again.
        int price = market.sell(myNewInstrument, 69, 1000);
        assertEquals(69, price);
        assertEquals(1000, market.getInstrumentData(myNewInstrument).getQuantity());
        assertEquals(69, market.getInstrumentData(myNewInstrument).getPrice());
        assertEquals(myNewInstrument.getName(), market.getInstrumentData(myNewInstrument).getName());
    }

    @Test
    public void testSellExistingInstrument1()
    {
        Market market = new Market();
        Instrument instrument = new Instrument("google");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        int originalPrice = market.getInstrumentData(instrument).getPrice();
        // Reduce the price of the instrument.
        int marketPrice = market.sell(instrument, 25, 100);
        assertEquals(originalQuantity + 100, market.getInstrumentData(instrument).getQuantity());
        assertNotEquals(marketPrice, originalPrice);
    }

    @Test
    public void testSellExistingInstrument2()
    {
        Market market = new Market();
        Instrument instrument = new Instrument("microsoft");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        int originalPrice = market.getInstrumentData(instrument).getPrice();
        // Keep the price of the instrument as is.
        int marketPrice = market.sell(instrument, 12, 100);
        assertEquals(originalQuantity + 100, market.getInstrumentData(instrument).getQuantity());
        assertEquals(originalPrice, marketPrice);
    }

    @Test
    public void testSellExistingInstrument3()
    {
        Market market = new Market();
        Instrument instrument = new Instrument("oracle");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        int originalPrice = market.getInstrumentData(instrument).getPrice();
        // Keep the price of the instrument as is.
        int marketPrice = market.sell(instrument, 8, 1);
        assertEquals(originalQuantity + 1, market.getInstrumentData(instrument).getQuantity());
        assertEquals(originalPrice, marketPrice);
    }

    @Test
    public void testSellExistingInstrument4()
    {
        Market market = new Market();
        Instrument instrument = new Instrument("apple");
        int originalQuantity = market.getInstrumentData(instrument).getQuantity();
        int originalPrice = market.getInstrumentData(instrument).getPrice();
        // Increase the price of the instrument.
        int marketPrice = market.sell(instrument, 9, 50);
        assertEquals(originalQuantity + 50, market.getInstrumentData(instrument).getQuantity());
        assertNotEquals(originalPrice, marketPrice);
    }
}
