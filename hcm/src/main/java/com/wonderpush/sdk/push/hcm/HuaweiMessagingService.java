package com.wonderpush.sdk.push.hcm;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.wonderpush.sdk.NotificationManager;
import com.wonderpush.sdk.NotificationModel;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushConfiguration;

import org.json.JSONObject;

public class HuaweiMessagingService extends HmsMessageService {

    private static final String TAG = "WonderPush.Push.HCM." + HuaweiMessagingService.class.getSimpleName();
    static final String WONDERPUSH_NOTIFICATION_EXTRA_KEY = "_wp";

    /*
     * Called when a new token for the default Firebase project is generated.
     *
     * This is invoked after app install when a token is first generated, and again if the token changes.
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        onNewToken(getApplicationContext(), token);
    }

    /**
     * Method to be called in your own {@link HmsMessageService} to handle
     * WonderPush push notifications.
     *
     * <b>Note:</b> This is only required if you use your own {@link HmsMessageService}.
     *
     * Implement your {@link HmsMessageService#onNewToken(String)} method as follows:
     *
     * <pre><code>@Override
     * public void onNewToken(String token) {
     *     WonderPushHmsMessageService.onNewToken(this, token);
     *     // Do your own handling here
     * }</code></pre>
     *
     * @param context The current context
     * @param token The received token
     */
    public static void onNewToken(Context context, String token) {
        if (WonderPush.getLogging()) Log.d(TAG, "onNewToken(" + token + ")");
        if (WonderPush.getLogging()) Log.d(TAG, "Known HMS AppId: " + HCMPushService.getHCMAppId());
        if (TextUtils.isEmpty(token)) {
            Log.w(TAG, "WonderPushHuaweiMessageService.onNewToken() called with an empty token, ignoring");
            return;
        }
        try {
            WonderPush.initialize(context);
            // To prevent loops, check if we don't already know this token
            if (token.equals(WonderPushConfiguration.getGCMRegistrationId())) { // FIXME Change name? Use another field?
                if (WonderPush.getLogging()) Log.d(TAG, "onNewToken() called with an already known token, ignoring");
                return;
            }
            // Check there is only one sender id used, so we know whether can trust this token to be for us
            String ourAppId = HCMPushService.getHCMAppId();

            // TODO Check if there's a similar concept of multiple apps in HMS
            //for (FirebaseApp apps : FirebaseApp.getApps(WonderPush.getApplicationContext())) {
            //    String someSenderId = apps.getOptions().getGcmSenderId();
            //    if (someSenderId != null && someSenderId.length() > 0 && !someSenderId.equals(ourAppId)) {
            //        // There are multiple sender ids used in this application
            //        WonderPush.logDebug("Multiple senderIds are used: seen " + someSenderId + " in addition to ours (" + ourAppId + ")");
            //        token = null;
            //        break;
            //    }
            //}
            if (token != null) {
                if (WonderPush.getLogging()) Log.d(TAG, "Storing new token");
                HCMPushService.storeRegistrationId(context, HCMPushService.getHCMAppId(), token);
            } else {
                // We cannot trust this token to be for our sender id, refresh ours
                // Note: we have taken measures to ensure we won't loop if this call triggers new calls to onNewToken()
                if (WonderPush.getLogging()) Log.d(TAG, "Fetching new token");
                HCMPushService.fetchInstanceId();
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
     * Method to be called in your own {@link HmsMessageService} to handle
     * WonderPush push notifications.
     *
     * <b>Note:</b> This is only required if you use your own {@link HmsMessageService}.
     *
     * Implement your {@link HmsMessageService#onMessageReceived(RemoteMessage)} method as follows:
     *
     * <pre><code>@Override
     * public void onMessageReceived(RemoteMessage message) {
     *     if (WonderPushHmsMessageService.onMessageReceived(this, message)) {
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
                String wpDataJson = message.getDataOfMap().get(WONDERPUSH_NOTIFICATION_EXTRA_KEY);
                JSONObject wpData = new JSONObject(wpDataJson);
                notif = NotificationModel.fromNotificationJSONObject(wpData);
            } catch (NotificationModel.NotTargetedForThisInstallationException ex) {
                if (WonderPush.getLogging()) Log.d(TAG, ex.getMessage());
                return true;
            }
            if (notif == null) {
                return false;
            }

            // FIXME I'm unuse about this, and about it's use
            Intent intent = new Intent();
            /*
            What's received and get parsed:
                message_body
                device_token
                inputType
                to
                message_type

            What's being written into their RemoteMessage Bundle:
                device_token
                data
                to
                msgId
                message_type
                notification

            What's being read from their RemoteMessage Bundle and has getters:
                from
                to
                data
                collapseKey
                msgId
                message_type
                sendTime
                ttl
                oriUrgency
                urgency
                device_token
                notification
             */
            // We can also add the whole RemoteMessage itself
            intent.putExtra("from", message.getFrom());
            intent.putExtra("to", message.getTo());
            intent.putExtra("data", message.getData());
            intent.putExtra("collapseKey", message.getCollapseKey());
            intent.putExtra("msgId", message.getMessageId());
            intent.putExtra("message_type", message.getMessageType());
            intent.putExtra("sendTime", message.getSentTime());
            intent.putExtra("ttl", message.getTtl());
            intent.putExtra("oriUrgency", message.getOriginalUrgency());
            intent.putExtra("urgency", message.getUrgency());
            intent.putExtra("device_token", message.getToken());
            intent.putExtra("notification", message.getNotification());
            NotificationManager.onReceivedNotification(context, intent, notif);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling HCM message from:" + message.getFrom() + " bundle:" + message.getData(), e);
        }
        return false;
    }

}
