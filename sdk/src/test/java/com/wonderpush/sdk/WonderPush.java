package com.wonderpush.sdk;

public class WonderPush {

    public static boolean getLogging() {
        return true;
    }

    protected static void logDebug(String message) {
        System.out.println(message);
    }

    protected static void logDebug(String message, Throwable ex) {
        System.out.println(message + ": " + ex.getMessage());
        ex.printStackTrace(System.out);
    }

}
