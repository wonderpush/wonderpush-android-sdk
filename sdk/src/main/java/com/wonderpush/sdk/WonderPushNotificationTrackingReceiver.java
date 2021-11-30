package com.wonderpush.sdk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class WonderPushNotificationTrackingReceiver extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Track the notification click
            track(getIntent());
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Failed to track notification", ex);
        }

        // Exit as soon as possible
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        try {
            // Track the notification click
            track(getIntent());
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Failed to track notification", ex);
        }

        // Exit as soon as possible
        finish();
    }

    private void track(Intent intent) {
        NotificationModel notif = NotificationModel.fromLocalIntent(intent, this);
        if (notif != null) {
            NotificationManager.handleOpenedNotificationFromTrackingActivity(this, intent, notif);
            ActivityLifecycleMonitor.onNextResume(activity -> WonderPush.showPotentialNotification(activity, intent));

            String targetUrl = intent.getStringExtra("targetUrl");
            if (targetUrl != null) {
                Uri parsedTargetUrl = Uri.parse(targetUrl);
                if (WonderPush.INTENT_NOTIFICATION_SCHEME.equals(parsedTargetUrl.getScheme())
                        && WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY.equals(parsedTargetUrl.getAuthority())
                        && parsedTargetUrl.getPathSegments().size() == 1
                        && WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_BROADCAST.equals(parsedTargetUrl.getLastPathSegment())
                ) {
                    // wonderpush://notificationOpen/broadcast
                    // Broadcast locally that a notification is to be opened, and don't do anything else
                    Intent notificationWillOpenIntent = new Intent();
                    notificationWillOpenIntent.setPackage(getApplicationContext().getPackageName());
                    notificationWillOpenIntent.setAction(WonderPush.INTENT_NOTIFICATION_WILL_OPEN);
                    notificationWillOpenIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                            (Parcelable) intent.getParcelableExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION));
                    notificationWillOpenIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL,
                            // this extra must be removed if handled outside the app,
                            // or we'll get ClassNotFoundException: E/Parcel: Class not found when unmarshalling: com.wonderpush.sdk.Notification*Model
                            notif);
                    notificationWillOpenIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE,
                            notif.getType().toString());
                    notificationWillOpenIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION,
                            intent.getBooleanExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION, true));
                    notificationWillOpenIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN,
                            true);
                    notificationWillOpenIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX,
                            intent.getIntExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX, -1));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(notificationWillOpenIntent);
                }
            }
        }
    }

}
