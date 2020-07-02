package android.util;

public class Log {

    private static int log(String level, String tag, String msg, Throwable tr) {
        System.out.println(level + "/" + tag + ": " + msg);
        if (tr != null) {
            tr.printStackTrace(System.out);
        }
        return 0;
    }

    public static int d(String tag, String msg) {
        return log("D", tag, msg, null);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return log("D", tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return log("W", tag, msg, null);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return log("W", tag, msg, tr);
    }

    public static int e(String tag, String msg) {
        return log("E", tag, msg, null);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return log("E", tag, msg, tr);
    }

}
