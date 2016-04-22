package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.wonderpush.sdk.NotificationModel.NotTargetedForThisInstallationException;

/**
 * A class that handles all the messages form Google Cloud Messaging service
 */
class WonderPushGcmClient {

    private static final String TAG = WonderPush.TAG;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String WONDERPUSH_NOTIFICATION_EXTRA_KEY = "_wp";

    protected static boolean onBroadcastReceived(Context context, Intent intent, int iconResource, Class<? extends Activity> activity) {
        WonderPush.ensureInitialized(context);

        NotificationModel notif;
        try {
            notif = NotificationModel.fromGCMBroadcastIntent(intent);
        } catch (NotTargetedForThisInstallationException ex) {
            WonderPush.logDebug(ex.getMessage());
            return true;
        }
        if (notif == null) {
            return false;
        }

        try {
            NotificationManager.onReceivedNotification(context, intent, iconResource, activity, notif);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while receiving a notification with intent " + intent, e);
        }
        return false;
    }

    /**
     * Helper method that will register a device for Google Cloud Messaging
     * notification and register the device token to WonderPush. This method is
     * called within {@link WonderPush#initialize(Context, String, String)}.
     *
     * @param context
     *            The current {@link Activity} (preferred) or {@link Application} context.
     */
    protected static void registerForPushNotification(Context context) {
        if (checkPlayService(context)) {
            // Get off the main UI thread for using GCM
            Intent intent = new Intent(context, WonderPushRegistrationIntentService.class);
            context.startService(intent);
        } else {
            Log.w(TAG, "Google Play Services not present. Check your setup. If on an emulator, use a Google APIs system image.");
        }
    }

    private static boolean checkPlayService(Context context) {
        try {
            Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
            if (resultCode != ConnectionResult.SUCCESS) {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    if (context instanceof Activity) {
                        googleApiAvailability.getErrorDialog((Activity) context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                    } else {
                        googleApiAvailability.showErrorNotification(context, resultCode);
                    }
                } else {
                    Log.w(TAG, "This device does not support Google Play Services, push notification are not supported");
                }
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "The Google Play Services have not been added to the application", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while checking the Google Play Services", e);
        }
        return false;
    }

}
