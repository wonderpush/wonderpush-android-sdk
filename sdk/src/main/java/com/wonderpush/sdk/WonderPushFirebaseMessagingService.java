package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WonderPushFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = WonderPush.TAG;
    static final String WONDERPUSH_DEFAULT_SENDER_ID = "1023997258979";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String WONDERPUSH_NOTIFICATION_EXTRA_KEY = "_wp";

    static String getDefaultSenderId() {
        Context context = WonderPush.getApplicationContext();
        int firebaseSenderIdRes = context.getResources().getIdentifier("gcm_defaultSenderId", "string", context.getPackageName());
        if (firebaseSenderIdRes == 0) {
            return WONDERPUSH_DEFAULT_SENDER_ID;
        } else {
            return context.getResources().getString(firebaseSenderIdRes);
        }
    }

    /**
     * Called when a new token for the default Firebase project is generated.
     * This is invoked after app install when a token is first generated, and again if the token changes.
     */
    @Override
    public void onNewToken(String token) {
        onNewToken(getApplicationContext(), token);
    }

    /**
     * Called when a new token for the default Firebase project is generated.
     * This is invoked after app install when a token is first generated, and again if the token changes.
     */
    public static void onNewToken(Context context, String token) {
        WonderPush.logDebug("WonderPushFirebaseMessagingService.onNewToken(" + token + ")");
        WonderPush.logDebug("Known Firebase SenderId: " + WonderPush.getSenderId());
        if (token == null) {
            Log.w(WonderPush.TAG, "WonderPushFirebaseMessagingService.onNewToken() called with a null token, ignoring");
            return;
        }
        try {
            WonderPush.ensureInitialized(context);
            // To prevent loops, check if we don't already know this token
            if (token.equals(WonderPushConfiguration.getGCMRegistrationId())) {
                WonderPush.logDebug("onNewToken() called with an already known token, ignoring");
                return;
            }
            // Check there is only one sender id used, so we know whether can trust this token to be for us
            String ourSenderId = WonderPush.getSenderId();
            for (FirebaseApp apps : FirebaseApp.getApps(WonderPush.getApplicationContext())) {
                String someSenderId = apps.getOptions().getGcmSenderId();
                if (someSenderId != null && someSenderId.length() > 0 && !someSenderId.equals(ourSenderId)) {
                    // There are multiple sender ids used in this application
                    WonderPush.logDebug("Multiple senderIds are used: seen " + someSenderId + " in addition to ours (" + ourSenderId + ")");
                    token = null;
                    break;
                }
            }
            if (token != null) {
                WonderPush.logDebug("Storing new token");
                storeRegistrationId(context, WonderPush.getSenderId(), token);
            } else {
                // We cannot trust this token to be for our sender id, refresh ours
                // Note: we have taken measures to ensure we won't loop if this call triggers new calls to onNewToken()
                WonderPush.logDebug("Fetching new token");
                fetchInstanceId();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling onNewToken", e);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        onMessageReceived(getApplicationContext(), message);
    }

    /**
     * Method to be called in your own {@link FirebaseMessagingService} to handle
     * WonderPush push notifications.
     *
     * <b>Note:</b> This is only required if you use your own {@link FirebaseMessagingService}.
     *
     * Implement your {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)} method as follows:
     *
     * <pre><code>@Override
     * public void onMessageReceived(String from, Bundle data) {
     *     if (WonderPushGcmListenerService.onMessageReceived(getApplicationContext(), from, data)) {
     *         return;
     *     }
     *     // Do your own handling here
     * }</code></pre>
     *
     * @param context The current context
     * @param message The received message
     * @return Whether the notification has been handled by WonderPush
     */
    public static boolean onMessageReceived(Context context, RemoteMessage message) {
        try {
            WonderPush.ensureInitialized(context);
            WonderPush.logDebug("Received a push notification!");

            NotificationModel notif;
            try {
                notif = NotificationModel.fromRemoteMessage(message);
            } catch (NotificationModel.NotTargetedForThisInstallationException ex) {
                WonderPush.logDebug(ex.getMessage());
                return true;
            }
            if (notif == null) {
                return false;
            }

            NotificationManager.onReceivedNotification(context, message.toIntent(), notif);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling GCM message from:" + message.getFrom() + " bundle:" + message.getData(), e);
        }
        return false;
    }

    static int getNotificationIcon(Context context) {
        int iconId = 0;
        try {
            Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            iconId = metaData.getInt("com.google.firebase.messaging.default_notification_icon");
        } catch (Exception e) {
            WonderPush.logError("Unexpected error while getting notification icon", e);
        }
        if (iconId == 0) {
            // Default to an embedded icon
            iconId = R.drawable.ic_notifications_white_24dp;
        }
        return iconId;
    }

    static int getNotificationColor(Context context) {
        int color = 0;
        try {
            Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            int resId = metaData.getInt("com.google.firebase.messaging.default_notification_color");
            color = ContextCompat.getColor(context, resId);
        } catch (Exception e) {
            WonderPush.logError("Unexpected error while getting notification color", e);
        }
        return color;
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
            WonderPushFirebaseMessagingService.fetchInstanceId();
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

    static void fetchInstanceId() {
        WonderPush.logDebug("FirebaseInstanceId.getInstanceId() called…");
        FirebaseInstanceId.getInstance(WonderPush.getFirebaseApp()).getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.e(WonderPush.TAG, "Could not get Firebase InstanceId", task.getException());
                            return;
                        }
                        InstanceIdResult result = task.getResult();
                        if (result == null) {
                            WonderPush.logDebug("FirebaseInstanceId.getInstanceId() = null");
                            return;
                        }
                        WonderPush.logDebug("FirebaseInstanceId.getInstanceId() = " + result.getToken());
                        storeRegistrationId(WonderPush.getApplicationContext(), WonderPush.getSenderId(), result.getToken());
                    }
                });
    }

    static void storeRegistrationId(Context context, String senderIds, String registrationId) {
        WonderPush.logDebug("storeRegistrationId(" + senderIds + ", " + registrationId + ")");
        WonderPushConfiguration.initialize(context);
        String oldRegistrationId = WonderPushConfiguration.getGCMRegistrationId();
        try {
            if (
                // New registration id
                registrationId == null && WonderPushConfiguration.getGCMRegistrationId() != null
                || registrationId != null && !registrationId.equals(WonderPushConfiguration.getGCMRegistrationId())
                // New sender id
                || senderIds == null && WonderPushConfiguration.getGCMRegistrationSenderIds() != null
                || senderIds != null && !senderIds.equals(WonderPushConfiguration.getGCMRegistrationSenderIds())
                // Last associated with an other userId?
                || WonderPushConfiguration.getUserId() == null && WonderPushConfiguration.getCachedGCMRegistrationIdAssociatedUserId() != null
                || WonderPushConfiguration.getUserId() != null && !WonderPushConfiguration.getUserId().equals(WonderPushConfiguration.getCachedGCMRegistrationIdAssociatedUserId())
                // Last associated with an other access token?
                || WonderPushConfiguration.getAccessToken() == null && WonderPushConfiguration.getCachedGCMRegistrationIdAccessToken() != null
                || WonderPushConfiguration.getAccessToken() != null && !WonderPushConfiguration.getAccessToken().equals(WonderPushConfiguration.getCachedGCMRegistrationIdAccessToken())
            ) {
                JSONObject properties = new JSONObject();
                JSONObject pushToken = new JSONObject();
                pushToken.put("data", registrationId);
                properties.put("pushToken", pushToken);
                if (senderIds == null || senderIds.length() == 0) {
                    properties.put("senderIds", JSONObject.NULL);
                } else {
                    JSONArray senderIdsArray = new JSONArray();
                    for (String senderId : senderIds.split(",")) {
                        senderId = senderId.trim();
                        if (senderId.length() > 0) {
                            senderIdsArray.put(senderId.trim());
                        }
                    }
                    properties.put("senderIds", senderIdsArray);
                }

                WonderPush.ensureInitialized(context);
                InstallationManager.updateInstallation(properties, false);
                WonderPushConfiguration.setCachedGCMRegistrationIdDate(System.currentTimeMillis());
                WonderPushConfiguration.setCachedGCMRegistrationIdAssociatedUserId(WonderPushConfiguration.getUserId());
                WonderPushConfiguration.setCachedGCMRegistrationIdAccessToken(WonderPushConfiguration.getAccessToken());
            }
        } catch (JSONException e) {
            Log.e(WonderPush.TAG, "Failed to update push token to WonderPush", e);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while updating push token to WonderPush", e);
        }

        WonderPushConfiguration.setGCMRegistrationId(registrationId);
        WonderPushConfiguration.setGCMRegistrationSenderIds(senderIds);

        if (oldRegistrationId == null && registrationId != null
                || oldRegistrationId != null && !oldRegistrationId.equals(registrationId)) {
            Intent pushTokenChangedIntent = new Intent(WonderPush.INTENT_PUSH_TOKEN_CHANGED);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_OLD_KNOWN_PUSH_TOKEN, oldRegistrationId);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_PUSH_TOKEN, registrationId);
            LocalBroadcastManager.getInstance(context).sendBroadcast(pushTokenChangedIntent);
        }
    }

}
