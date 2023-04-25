package market;

import java.util.ArrayList;

public class Market {
    private final ArrayList<Instrument> instruments;

    public Market() {
        this.instruments = new ArrayList<>();
        Instrument temp = new Instrument("apple", 5, 50);
        this.instruments.add(temp);
        temp = new Instrument("google", 17, 100);
        this.instruments.add(temp);
        temp = new Instrument("microsoft", 12, 57);
        this.instruments.add(temp);
        temp = new Instrument("oracle", 8, 64);
        this.instruments.add(temp);
    }

    private boolean buyImplement(Instrument actualInstrument, int price, int quantity) {
        boolean purchased = false;
        int availablePrice = actualInstrument.getPrice();
        int availableQuantity = actualInstrument.getQuantity();
        if (price >= availablePrice && quantity <= availableQuantity) {
            purchased = true;
            actualInstrument.setQuantity(availableQuantity - quantity);
            // TODO: change the price of the instrument.
        }
        return purchased;
    }

    public int sellImplement(Instrument instrument, int price, int quantity)
    {
        Instrument marketInstrument = findInstrument(instrument);
        // We do not sell this instrument currently, Add it with the given price.
        if (marketInstrument == null) {
            instrument.setPrice(price);
            instrument.setQuantity(quantity);
            instruments.add(instrument);
            return price;
        }
        if (marketInstrument.getQuantity() == 0)
        {
            marketInstrument.setPrice(price);
            marketInstrument.setQuantity(quantity);
            return price;
        }
        // We do sell this instrument currently, add the quantity and adjust the price.
        int newQuantity = marketInstrument.getQuantity() + quantity;
        int newPrice = getAdjustedMarketPrice(instrument, price, quantity);
        marketInstrument.setQuantity(newQuantity);
        marketInstrument.setPrice(newPrice);
        return newPrice;
    }

    private Instrument findInstrument(Instrument instrument)
    {
        for (Instrument temp : instruments) {
            if (temp.compareTo(instrument) == 0)
                return temp;
        }
        return null;
    }

    private int getAdjustedMarketPrice(Instrument instrument, int supplyPrice, int supplyQuantity)
    {
        int marketPrice = this.findInstrument(instrument).getPrice();
        int marketQuantity = this.findInstrument(instrument).getQuantity();
        return (((marketPrice * marketQuantity) + (supplyPrice * supplyQuantity)) / (marketQuantity + supplyQuantity));
    }

    public Instrument getInstrumentData(Instrument instrument)
    {
        return findInstrument(instrument);
    }

    public boolean buy(Instrument instrument, int quantity)
    {
        Instrument marketInstrument = findInstrument(instrument);
        if (marketInstrument == null)
            return false;
        else
            return buyImplement(marketInstrument, marketInstrument.getPrice(), quantity);
    }

    public boolean buy(String instrumentName, int quantity)
    {
        Instrument instrument = new Instrument(instrumentName);
        return buy(instrument, quantity);
    }

    /*
     * Try to buy an instrument for a price rather than for market price.
     * */
    public boolean buy(Instrument instrument, int price, int quantity)
    {
        Instrument marketInstrument = findInstrument(instrument);
        if (marketInstrument == null)
            return false;
        else
            return buyImplement(marketInstrument, price, quantity);
    }

    public boolean buy(String instrumentName, int price, int quantity)
    {
        Instrument instrument = new Instrument(instrumentName);
        return buy(instrument, price, quantity);
    }

    public int sell(Instrument instrument, int price, int quantity)
    {
        return this.sellImplement(instrument, price, quantity);
    }

    public int sell(String instrumentName, int price, int quantity)
    {
        Instrument instrument = new Instrument(instrumentName);
        return this.sellImplement(instrument, price, quantity);
    }

    public int sell(Instrument instrument, int quantity)
    {
        Instrument marketInstrument = getInstrumentData(instrument);
        if (marketInstrument == null)
        {
            int defaultPrice = 12;
            return this.sellImplement(marketInstrument, defaultPrice, quantity);
        }
        return this.sellImplement(marketInstrument, marketInstrument.getPrice(), quantity);
    }

    public int sell(String instrumentName, int quantity)
    {
        Instrument instrument = new Instrument(instrumentName);
        return this.sell(instrument, quantity);
    }
}
