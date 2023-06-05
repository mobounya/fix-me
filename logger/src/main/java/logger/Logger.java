package logger;

public class Logger {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void logInfo(String info)
    {
        System.out.print(ANSI_CYAN);
        System.out.print("[INFO] ");
        System.out.print(ANSI_RESET);
        System.out.println(info);
    }

    public static void logWarning(String warning)
    {
        System.out.print(ANSI_YELLOW);
        System.out.print("[WARNING] ");
        System.out.print(ANSI_RESET);
        System.out.println(warning);
    }

    public static void logSuccess(String success)
    {
        System.out.print(ANSI_GREEN);
        System.out.print("[SUCCESS] ");
        System.out.print(ANSI_RESET);
        System.out.println(success);
    }

    public static void logError(String error)
    {
        System.out.print(ANSI_RED);
        System.out.print("[ERROR] ");
        System.out.print(ANSI_RESET);
        System.out.println(error);
    }
}
