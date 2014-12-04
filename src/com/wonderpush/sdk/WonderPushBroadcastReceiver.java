package com.wonderpush.sdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

/**
 * Push notification receiver.
 *
 * <p>
 *   Please look at the <a href="../../../packages.html#installing-sdk--configuring-sdk">package index</a> for more information
 *   on how to setup this receiver.
 * </p>
 *
 * <p>
 *   This {@link BroadcastReceiver} will receive all push notifications and handle them to {@link WonderPush},
 *   like you would, by calling manually {@link WonderPush#onBroadcastReceived(Context, Intent, int, Class)}.
 * </p>
 *
 * <p>
 *   Troubleshooting tip: Make sure you added the {@code "notificationIcon"} and {@code "activityName"}
 *   {@code <meta-data>} tags under the corresponding {@code <receiver>} tag of your {@code <application>},
 *   in your {@code AndroidManifest.xml}.
 * </p>
 */
public class WonderPushBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = WonderPush.TAG;

    private static final String METADATA_ICON_KEYNAME = "notificationIcon";
    private static final String METADATA_ACTIVITY_KEYNAME = "activityName";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            WonderPush.logDebug("Received a push notification!");
            ComponentName receiver = new ComponentName(context, this.getClass());
            try {
                Bundle metaData = context.getPackageManager().getReceiverInfo(receiver, PackageManager.GET_META_DATA).metaData;
                int iconId = metaData.getInt(METADATA_ICON_KEYNAME, -1);
                String activityName = metaData.getString(METADATA_ACTIVITY_KEYNAME);
                if (iconId == -1) {
                    Log.e(TAG, "You should specify a <meta-data android:name=\"notificationIcon\" android:resource=\"@drawable/YourApplicationIcon\"/> in your Receiver definition of your AndroidManifest.xml");
                    return;
                }
                if (activityName == null) {
                    Log.e(TAG, "You should specify a <meta-data android:name=\"activityName\" android:value=\"com.package.YourMainActivity\"/> in your Receiver definition of your AndroidManifest.xml");
                    return;
                }
                WonderPushGcmClient.onBroadcastReceived(context, intent, iconId, Class.forName(activityName).asSubclass(Activity.class));
            } catch (NameNotFoundException e) {
                // If we are called, then at least
                // <receiver android:name="com.wonderpush.sdk.WonderPushBroadcastReceiver"
                //     android:permission="com.google.android.c2dm.permission.SEND" />
                // has been added to the AndroidManifest.xml (or this function has been called manually)
                Log.e(TAG, "Could not get the receiver meta-data", e);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "The activity provided in <meta-data android:name=\"activityName\" android:value=\"com.package.YourMainActivity\"/> in your Receiver definition of your AndroidManifest.xml doesn't exist", e);
            } catch (ClassCastException e) {
                Log.e(TAG, "The class provided in <meta-data android:name=\"activityName\" android:value=\"com.package.YourMainActivity\"/> in your Receiver definition of your AndroidManifest.xml is not an Activity", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    static int getNotificationIcon(Context context) {
        ComponentName receiver = new ComponentName(context, WonderPushBroadcastReceiver.class);
        try {
            Bundle metaData = context.getPackageManager().getReceiverInfo(receiver, PackageManager.GET_META_DATA).metaData;
            return metaData.getInt(METADATA_ICON_KEYNAME, -1);
        } catch (NameNotFoundException e) {
            return -1;
        }
    }

}
