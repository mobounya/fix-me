public class Main {

    public static void main(String[] args) {
        if (args.length < 1)
        {
            System.err.println("Please provide a market name");
            System.exit(1);
        }
        Server market = new Server(args[0]);
        market.start();
    }
}
