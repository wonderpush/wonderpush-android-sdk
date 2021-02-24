package com.wonderpush.sdk.remoteconfig;

import com.wonderpush.sdk.TimeSync;

import java.util.Date;

public class DateHelper {
    public static Date now() {
        return new Date(TimeSync.getTime());
    }
}
