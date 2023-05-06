package engineFIX;

public class TagFormatException extends Exception {
    private static final String prefix = "Format error: ";

    public TagFormatException(String tag)
    {
        super(prefix + tag);
    }

    public static String getPrefix()
    {
        return prefix;
    }
}
