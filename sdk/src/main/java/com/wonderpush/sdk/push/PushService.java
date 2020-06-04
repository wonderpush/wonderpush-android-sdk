package com.wonderpush.sdk.push;

import android.content.Context;

public interface PushService {

    String getIdentifier();
    String getVersion();
    String toString();
    void initialize(Context context);
    boolean isAvailable();
    void execute();
    int getNotificationIcon();
    int getNotificationColor();

}
