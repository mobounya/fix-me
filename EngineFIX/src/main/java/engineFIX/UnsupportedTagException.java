package engineFIX;

public class UnsupportedTagException extends Exception {
    private static final String prefix = "Unsupported tag: ";

    public UnsupportedTagException(String tag)
    {
        super(prefix + tag);
    }

    public static String getPrefix()
    {
        return prefix;
    }
}
