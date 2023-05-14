import broker.Broker;

public class Main {
    public static void main(String[] args) {
        Broker client = new Broker("someBroker");
        client.start();
    }
}
