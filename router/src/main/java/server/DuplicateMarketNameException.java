package server;

public class DuplicateMarketNameException extends Exception {
    private static final String prefix = "Duplicate market name: ";

    public DuplicateMarketNameException(String marketName)
    {
        super(prefix + marketName);
    }
}
