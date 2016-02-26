package com.wonderpush.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * @deprecated You should use {@link WonderPushGcmListenerService} instead.
 */
public class WonderPushBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = WonderPush.TAG;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Bundle bundle = intent.getExtras();
            String from = bundle.getString("from");
            WonderPushGcmListenerService.onMessageReceived(context, from, bundle);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while treating broadcast intent " + intent, e);
        }
    }

}
