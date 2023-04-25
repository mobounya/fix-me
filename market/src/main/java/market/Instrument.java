package market;

import java.lang.Comparable;

public class Instrument implements Comparable<Instrument> {
    private final String name;
    private int price;
    private int quantity;

    Instrument(String name, int price, int quantity)
    {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    Instrument(String name, int price)
    {
        this.name = name;
        this.price = price;
        this.quantity = 0;
    }

    Instrument(String name)
    {
        this.name = name;
        this.price = 0;
        this.quantity = 0;
    }

    String getName()
    {
        return this.name;
    }

    int getPrice()
    {
        return this.price;
    }

    void setPrice(int newPrice)
    {
        this.price = newPrice;
    }

    int getQuantity()
    {
        return this.quantity;
    }

    void setQuantity(int newQuantity)
    {
        this.quantity = newQuantity;
    }

    @Override
    public int compareTo(Instrument o) {
        return (this.name.compareTo(o.getName()));
    }
}
