package engineFIX;

public class BadTagValueException extends Exception {
    private static final String prefix = "Bad tag: ";

    BadTagValueException(String tag)
    {
        super(prefix + tag);
    }

    public static String getPrefix()
    {
        return prefix;
    }
}
