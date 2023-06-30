package com.wonderpush.sdk;

public class TimeSync {

    public static long getUnadjustedSystemCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long getUnadjustedSystemClockElapsedRealtime() {
        return 0;
    }

    public static long getTime() {
        return adjustTime(getUnadjustedSystemCurrentTimeMillis(), getUnadjustedSystemClockElapsedRealtime());
    }

    public static long adjustTime(long unadjustedSystemCurrentTimeMillis, long unadjustedSystemClockElapsedRealtime) {
        return unadjustedSystemCurrentTimeMillis;
    }

}
