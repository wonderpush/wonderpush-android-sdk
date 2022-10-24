package com.wonderpush.tests;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushChannel;
import com.wonderpush.sdk.WonderPushUserPreferences;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WonderPush.addTag("unittest");

        // Example deeplink handled by application logic
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE_DATA.equals(
                        intent.getStringExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE))) {
                    Log.i("WonderPushTests", "Received broadcast, resolving programmatically: " + intent);
                    Log.i("WonderPushTests", "- local broadcast intent: " + intent);
                    Bundle intentExtras = intent.getExtras();
                    if (intentExtras != null) {
                        for (String key : intentExtras.keySet()) {
                            Log.i("WonderPushTests", "..extra " + key + ": " + intentExtras.get(key));
                        }
                    }
                    Intent pushNotificationIntent = intent.getParcelableExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION);
                    Log.i("WonderPushTests", "- push notification intent: " + pushNotificationIntent);
                    Bundle pushNotificationIntentExtras = pushNotificationIntent != null ? pushNotificationIntent.getExtras() : null;
                    if (pushNotificationIntentExtras != null) {
                        for (String key : pushNotificationIntentExtras.keySet()) {
                            Log.i("WonderPushTests", "..extra " + key + ": " + pushNotificationIntentExtras.get(key));
                        }
                    }
                    if (pushNotificationIntent != null && "noop".equals(pushNotificationIntent.getStringExtra("broadcastData"))) {
                        Log.i("WonderPushTests", "Broadcast data asked to be a noop, not starting any activity");
                        return;
                    }
                    Intent openIntent = new Intent();
                    openIntent.setClass(context, NavigationActivity.class);
                    openIntent.fillIn(intent, 0);
                    openIntent.putExtra("resolvedProgrammatically", true);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntentWithParentStack(openIntent);
                    // Revert some flags changes done by TaskStackBuilder to keep a possible current application state behind our added activity
                    Intent[] intents = stackBuilder.getIntents();
                    intents[0].setFlags(intents[0].getFlags() & ~(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME));
                    context.startActivities(intents);
                }
            }
        }, new IntentFilter(WonderPush.INTENT_NOTIFICATION_WILL_OPEN));

        // Example data notification handled by application logic
        WonderPushUserPreferences.putChannel(new WonderPushChannel("data-push-manually-display", null)
            .setDescription("Data push manually displayed"));
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE_DATA.equals(
                        intent.getStringExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE))) {
                    Log.i("WonderPushTests", "Received data notification: " + intent);
                    Log.i("WonderPushTests", "- local broadcast intent: " + intent);
                    Bundle intentExtras = intent.getExtras();
                    if (intentExtras != null) {
                        for (String key : intentExtras.keySet()) {
                            Log.i("WonderPushTests", "..extra " + key + ": " + intentExtras.get(key));
                        }
                    }
                    Intent pushNotificationIntent = intent.getParcelableExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION);
                    Log.i("WonderPushTests", "- push notification intent: " + pushNotificationIntent);
                    Bundle pushNotificationIntentExtras = pushNotificationIntent != null ? pushNotificationIntent.getExtras() : null;
                    if (pushNotificationIntentExtras != null) {
                        for (String key : pushNotificationIntentExtras.keySet()) {
                            Log.i("WonderPushTests", "..extra " + key + ": " + pushNotificationIntentExtras.get(key));
                        }
                    }
                    String display = pushNotificationIntent == null ? null : pushNotificationIntent.getStringExtra("display");
                    if ("none".equals(display)) {
                        Log.i("WonderPushTests", "We won't display this notification ourselves");
                    } else if ("notification".equals(display)) {
                        int localNotificationId = Math.round((float) Math.random() * Integer.MAX_VALUE);
                        Intent manuallyStartApp = new Intent(getApplicationContext(), MainActivity.class);
                        manuallyStartApp.setData(Uri.parse("fakeScheme://manuallyDisplayed?intentFilterEqualsBuster=" + Math.random()));
                        manuallyStartApp.putExtras(intent); // inherit the local broadcast intent extras so that click tracking works when opening the Activity
                        manuallyStartApp.putExtra("closeNotificationTag", (String) null);
                        manuallyStartApp.putExtra("closeNotificationId", localNotificationId);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, manuallyStartApp, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
                        Notification notification = new NotificationCompat.Builder(getApplicationContext(), "data-push-manually-display")
                                .setSmallIcon(R.drawable.ic_notifications_white_24dp)
                                .setContentTitle(pushNotificationIntent.getStringExtra("title"))
                                .setContentText(pushNotificationIntent.getStringExtra("text"))
                                .setContentIntent(pendingIntent)
                                .build();
                        Log.i("WonderPushTests", "Manually showing notification");
                        android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(null, localNotificationId, notification);
                    }
                }
            }
        }, new IntentFilter(WonderPush.INTENT_NOTIFICATION_WILL_OPEN));
    }

}
