package com.wonderpush.sdk.push.fcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.RemoteMessage;
import com.wonderpush.sdk.NotificationManager;
import com.wonderpush.sdk.NotificationModel;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "WonderPush.Push.FCM." + FirebaseMessagingService.class.getSimpleName();

    static final String WONDERPUSH_NOTIFICATION_EXTRA_KEY = "_wp";

    /*
     * Called when a new token for the default Firebase project is generated.
     *
     * This is invoked after app install when a token is first generated, and again if the token changes.
     */
    @Override
    public void onNewToken(String token) {
        onNewToken(getApplicationContext(), token);
    }

    /**
     * Method to be called in your own {@link com.google.firebase.messaging.FirebaseMessagingService} to handle
     * WonderPush push notifications.
     *
     * <b>Note:</b> This is only required if you use your own {@link com.google.firebase.messaging.FirebaseMessagingService}.
     *
     * Implement your {@link com.google.firebase.messaging.FirebaseMessagingService#onNewToken(String)} method as follows:
     *
     * <pre><code>@Override
     * public void onNewToken(String token) {
     *     WonderPushFirebaseMessagingService.onNewToken(this, token);
     *     // Do your own handling here
     * }</code></pre>
     *
     * @param context The current context
     * @param token The received token
     */
    public static void onNewToken(Context context, String token) {
        if (WonderPush.getLogging()) Log.d(TAG, "onNewToken(" + token + ")");
        if (WonderPush.getLogging()) Log.d(TAG, "Known Firebase SenderId: " + FCMPushService.getSenderId());
        if (token == null) {
            Log.w(TAG, "onNewToken() called with a null token, ignoring");
            return;
        }
        try {
            WonderPush.initialize(context);
            // To prevent loops, check if we don't already know this token
            if (token.equals(WonderPushConfiguration.getGCMRegistrationId())) {
                if (WonderPush.getLogging()) Log.d(TAG, "onNewToken() called with an already known token, ignoring");
                return;
            }
            // Check there is only one sender id used, so we know whether can trust this token to be for us
            String ourSenderId = FCMPushService.getSenderId();
            for (FirebaseApp apps : FirebaseApp.getApps(FCMPushService.sContext)) {
                String someSenderId = apps.getOptions().getGcmSenderId();
                if (someSenderId != null && someSenderId.length() > 0 && !someSenderId.equals(ourSenderId)) {
                    // There are multiple sender ids used in this application
                    if (WonderPush.getLogging()) Log.d(TAG, "Multiple senderIds are used: seen " + someSenderId + " in addition to ours (" + ourSenderId + ")");
                    token = null;
                    break;
                }
            }
            if (token != null) {
                if (WonderPush.getLogging()) Log.d(TAG, "Storing new token");
                FCMPushService.storeRegistrationId(context, FCMPushService.getSenderId(), token);
            } else {
                // We cannot trust this token to be for our sender id, refresh ours
                // Note: we have taken measures to ensure we won't loop if this call triggers new calls to onNewToken()
                if (WonderPush.getLogging()) Log.d(TAG, "Fetching new token");
                FCMPushService.fetchInstanceId();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling onNewToken", e);
        }
    }

    /*
     * Called when a message is received.
     *
     * This is also called when a notification message is received while the app is in the foreground.
     * @param message Remote message that has been received.
     */
    @Override
    public void onMessageReceived(RemoteMessage message) {
        onMessageReceived(getApplicationContext(), message);
    }

    /**
     * Method to be called in your own {@link com.google.firebase.messaging.FirebaseMessagingService} to handle
     * WonderPush push notifications.
     *
     * <b>Note:</b> This is only required if you use your own {@link com.google.firebase.messaging.FirebaseMessagingService}.
     *
     * Implement your {@link com.google.firebase.messaging.FirebaseMessagingService#onMessageReceived(RemoteMessage)} method as follows:
     *
     * <pre><code>@Override
     * public void onMessageReceived(RemoteMessage message) {
     *     if (WonderPushFirebaseMessagingService.onMessageReceived(this, message)) {
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
            WonderPush.initialize(context);
            if (WonderPush.getLogging()) Log.d(TAG, "Received a push notification!");

            NotificationModel notif;
            try {
                notif = notificationModelFromRemoteMessage(message, context);
            } catch (NotificationModel.NotTargetedForThisInstallationException ex) {
                if (WonderPush.getLogging()) Log.d(TAG, ex.getMessage());
                return true;
            }
            if (notif == null) {
                return false;
            }

            NotificationManager.onReceivedNotification(context, message.toIntent(), notif);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling FCM message from:" + message.getFrom() + " bundle:" + message.getData(), e);
        }
        return false;
    }

    public static NotificationModel notificationModelFromRemoteMessage(RemoteMessage remoteMessage, Context context)
            throws NotificationModel.NotTargetedForThisInstallationException
    {
        return notificationModelFromGCMBroadcastIntent(remoteMessage.toIntent(), context);
    }

    public static NotificationModel notificationModelFromGCMBroadcastIntent(Intent intent, Context context)
            throws NotificationModel.NotTargetedForThisInstallationException
    {
        try {
            Bundle extras = intent.getExtras();
            if (extras == null || extras.isEmpty()) { // has effect of unparcelling Bundle
                if (WonderPush.getLogging()) Log.d(TAG, "Received broadcasted intent has no extra");
                return null;
            }
            String wpDataJson = extras.getString(WONDERPUSH_NOTIFICATION_EXTRA_KEY);
            if (wpDataJson == null) {
                if (WonderPush.getLogging()) Log.d(TAG, "Received broadcasted intent has no data for WonderPush");
                return null;
            }

            if (WonderPush.getLogging()) Log.d(TAG, "Received broadcasted intent: " + intent);
            if (WonderPush.getLogging()) Log.d(TAG, "Received broadcasted intent extras: " + extras.toString());
            for (String key : extras.keySet()) {
                if (WonderPush.getLogging()) Log.d(TAG, "Received broadcasted intent extras " + key + ": " + bundleGetTypeUnsafe(extras, key));
            }

            try {
                JSONObject wpData = new JSONObject(wpDataJson);
                if (WonderPush.getLogging()) Log.d(TAG, "Received broadcasted intent WonderPush data: " + wpDataJson);
                return NotificationModel.fromNotificationJSONObject(wpData);
            } catch (JSONException e) {
                if (WonderPush.getLogging()) Log.d(TAG, "data is not a well-formed JSON object", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while receiving a notification with intent " + intent, e);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static Object bundleGetTypeUnsafe(Bundle bundle, String key) {
        return bundle.get(key);
    }

}
