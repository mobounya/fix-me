package market;

import java.lang.Comparable;

public class Instrument implements Comparable<Instrument> {
    private final String name;
    private int price;
    private int quantity;

    public Instrument(String name, int price, int quantity)
    {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public Instrument(String name, int price)
    {
        this.name = name;
        this.price = price;
        this.quantity = 0;
    }

    public Instrument(String name)
    {
        this.name = name;
        this.price = 0;
        this.quantity = 0;
    }

    public String getName()
    {
        return this.name;
    }

    public int getPrice()
    {
        return this.price;
    }

    public void setPrice(int newPrice)
    {
        this.price = newPrice;
    }

    public int getQuantity()
    {
        return this.quantity;
    }

    public void setQuantity(int newQuantity)
    {
        this.quantity = newQuantity;
    }

    @Override
    public int compareTo(Instrument o) {
        return (this.name.compareTo(o.getName()));
    }
}
