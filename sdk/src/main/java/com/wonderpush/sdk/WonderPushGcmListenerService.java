package com.wonderpush.sdk;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class WonderPushGcmListenerService extends GcmListenerService {

    private static final String TAG = WonderPush.TAG;

    private static final String METADATA_ICON_KEYNAME = "notificationIcon";
    private static final String METADATA_ACTIVITY_KEYNAME = "activityName";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        onMessageReceived(getApplicationContext(), from, data);
    }

    /**
     * Method to be called in your own Google Cloud Messaging {@link GcmListenerService} to handle
     * WonderPush push notifications.
     *
     * <b>Note:</b> This is only required if you use your own {@link GcmListenerService}.
     *
     * Implement your {@link GcmListenerService#onMessageReceived(String, Bundle)} method as follows:
     *
     * <pre><code>@Override
     * public void onMessageReceived(String from, Bundle data) {
     *     if (WonderPushGcmListenerService.onMessageReceived(getApplicationContext(), from, data)) {
     *         return;
     *     }
     *     // Do your own handling here
     * }</code></pre>
     *
     * @see WonderPush#onBroadcastReceived(Context, Intent, int, Class)
     * @param context The current context
     * @param from The sender id
     * @param data The notification data
     * @return Whether the notification has been handled by WonderPush
     */
    public static boolean onMessageReceived(Context context, String from, Bundle data) {
        try {
            return onMessageReceived(context, from, data, getNotificationIcon(context), getActivity(context));
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling GCM message from:" + from + " bundle:" + data, e);
        }
        return false;
    }

    /**
     * Method to be called in your own Google Cloud Messaging {@link GcmListenerService} to handle
     * WonderPush push notifications, overriding default icon and default activity.
     *
     * @see #onMessageReceived(Context, String, Bundle)
     * @param context The current context
     * @param from The sender id
     * @param data The notification data
     * @param iconResource The default notification icon to use
     * @param activity The default activity to use
     * @return Whether the notification has been handled by WonderPush
     */
    public static boolean onMessageReceived(Context context, String from, Bundle data, int iconResource, Class<? extends Activity> activity) {
        try {
            WonderPush.ensureInitialized(context);
            WonderPush.logDebug("Received a push notification!");
            // Reconstruct the original intent
            Intent pushIntent = new Intent("com.google.android.c2dm.intent.RECEIVE")
                    .addCategory(context.getPackageName())
                    .putExtras(data)
                    .putExtra("from", from);
            try {
                return WonderPushGcmClient.onBroadcastReceived(context, pushIntent, iconResource, activity);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while treating broadcast intent " + pushIntent, e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling GCM message from:" + from + " bundle:" + data, e);
        }
        return false;
    }

    static int getNotificationIcon(Context context) {
        int iconId = -1;
        ComponentName service = new ComponentName(context, WonderPushService.class);
        try {
            Bundle metaData = context.getPackageManager().getServiceInfo(service, PackageManager.GET_META_DATA).metaData;
            iconId = metaData.getInt(METADATA_ICON_KEYNAME, -1);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get the service meta-data", e);
        } catch (Exception e) {
            WonderPush.logError("Unexpected error while getting notification icon", e);
        }
        if (iconId == -1 || iconId == 0) {
            Log.e(TAG, "Please check the value given in android { defaultConfig { manifestPlaceholders = [ wonderpushNotificationIcon: '@drawable/your_own_notification_icon' ] } } in your app/build.gradle");
            //// Default to the application icon (only the alpha mask is used, it usually does not have a recognizable shape)
            //iconId = context.getApplicationInfo().icon;
            // Default to an embedded icon
            iconId = R.drawable.ic_notifications_white_24dp;
        }
        return iconId;
    }

    static Class<? extends Activity> getActivity(Context context) {
        Class<? extends Activity> activityClass = null;
        ComponentName service = new ComponentName(context, WonderPushService.class);
        try {
            Bundle metaData = context.getPackageManager().getServiceInfo(service, PackageManager.GET_META_DATA).metaData;
            String activityName = metaData.getString(METADATA_ACTIVITY_KEYNAME);
            if (activityName != null) {
                if (activityName.startsWith(".")) {
                    activityName = context.getPackageName() + activityName;
                }
                activityClass = Class.forName(activityName).asSubclass(Activity.class);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get the service meta-data", e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "The default activity class you configured does not exist. It may also have been renamed by ProGuard because it was not referenced by your AndroidManifest.xml", e);
        } catch (ClassCastException e) {
            Log.e(TAG, "The default activity class you configured does not correspond to an Activity", e);
        }
        if (activityClass == null) {
            Log.e(TAG, "Please check the value given in android { defaultConfig { manifestPlaceholders = [ wonderpushDefaultActivity: '.YourMainActivity' ] } } in your app/build.gradle");
        }
        return activityClass;
    }

}
