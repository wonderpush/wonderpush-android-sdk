package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WonderPushHuaweiMessagingService extends HmsMessageService {

    private static final String TAG = WonderPush.TAG;
    static final String WONDERPUSH_NOTIFICATION_EXTRA_KEY = "_wp";

    static String getDefaultAppId() {
        String appId = AGConnectServicesConfig.fromContext(WonderPush.getApplicationContext()).getString("client/app_id");
        if (TextUtils.isEmpty(appId)) {
            return null;
        }
        return appId;
    }

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
        WonderPush.logDebug("WonderPushHuaweiMessageService.onNewToken(" + token + ")");
        WonderPush.logDebug("Known HMS AppId: " + WonderPush.getHCMAppId());
        if (TextUtils.isEmpty(token)) {
            Log.w(WonderPush.TAG, "WonderPushHuaweiMessageService.onNewToken() called with an empty token, ignoring");
            return;
        }
        try {
            WonderPush.ensureInitialized(context);
            // To prevent loops, check if we don't already know this token
            if (token.equals(WonderPushConfiguration.getGCMRegistrationId())) { // FIXME Change name? Use another field?
                WonderPush.logDebug("onNewToken() called with an already known token, ignoring");
                return;
            }
            // Check there is only one sender id used, so we know whether can trust this token to be for us
            String ourAppId = WonderPush.getHCMAppId();

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
                WonderPush.logDebug("Storing new token");
                storeRegistrationId(context, "HCM", WonderPush.getHCMAppId(), token);
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
            WonderPush.ensureInitialized(context);
            WonderPush.logDebug("Received a push notification!");

            NotificationModel notif;
            try {
                String wpDataJson = message.getDataOfMap().get(WONDERPUSH_NOTIFICATION_EXTRA_KEY);
                JSONObject wpData = new JSONObject(wpDataJson);
                notif = NotificationModel.fromNotificationJSONObject(wpData);
            } catch (NotificationModel.NotTargetedForThisInstallationException ex) {
                WonderPush.logDebug(ex.getMessage());
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

    static int getNotificationIcon(Context context) {
        int iconId = 0;
        try {
            Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            iconId = metaData.getInt("com.huawei.messaging.default_notification_icon");
        } catch (Exception e) {
            WonderPush.logError("Unexpected error while getting notification icon", e);
        }
        if (iconId == 0) {
            // Default to an embedded icon
            iconId = R.drawable.ic_notifications_white_24dp;
        }
        return iconId;
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
        if (checkHMS(context)) {
            WonderPushHuaweiMessagingService.fetchInstanceId();
        } else {
            Log.w(TAG, "Huawei Mobile Services not present. Check your setup. If on an emulator, use an HMS-enabled system image.");
        }
    }

    private static boolean checkHMS(Context context) {
        // TODO Better check?
        return !TextUtils.isEmpty(WonderPush.getHCMAppId());
    }

    static void fetchInstanceId() {
        final String appId = WonderPush.getHCMAppId();
        WonderPush.logDebug("HmsInstanceId.getToken(\"" + appId + "\", \"HCM\") will be called in the background…");
        WonderPush.safeDeferWithConsent(new Runnable() {
            @Override
            public void run() {
                try {
                    WonderPush.logDebug("HmsInstanceId.getToken(\"" + appId + "\", \"HCM\") called…");
                    String pushToken = HmsInstanceId.getInstance(WonderPush.getApplicationContext()).getToken(appId, "HCM");
                    if (TextUtils.isEmpty(pushToken)) {
                        WonderPush.logDebug("HmsInstanceId.getToken() = null");
                    } else {
                        WonderPush.logDebug("HmsInstanceId.getToken() = " + pushToken);
                        storeRegistrationId(WonderPush.getApplicationContext(), "HCM", WonderPush.getHCMAppId(), pushToken);
                    }
                } catch (Exception ex) {
                    Log.e(WonderPush.TAG, "Could not get HMS InstanceId", ex);
                }
            }
        }, "WonderPushHuaweiMessagingService.fetchInstanceId");
    }

    // FIXME Copied from WonderPushHmsMessageService, refactor
    static void storeRegistrationId(Context context, String service, String senderIds, String registrationId) {
        WonderPush.logDebug("storeRegistrationId(" + service + ", " + senderIds + ", " + registrationId + ")");
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
                // New service
                || service == null && WonderPushConfiguration.getGCMRegistrationService() != null
                || service != null && !service.equals(WonderPushConfiguration.getGCMRegistrationService())
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
                if (senderIds == null || senderIds.length() == 0) {
                    pushToken.put("senderIds", JSONObject.NULL);
                } else {
                    JSONArray senderIdsArray = new JSONArray();
                    for (String senderId : senderIds.split(",")) {
                        senderId = senderId.trim();
                        if (senderId.length() > 0) {
                            senderIdsArray.put(senderId.trim());
                        }
                    }
                    pushToken.put("senderIds", senderIdsArray);
                }
                pushToken.put("service", service);
                properties.put("pushToken", pushToken);

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
        WonderPushConfiguration.setGCMRegistrationService(service);

        if (oldRegistrationId == null && registrationId != null
                || oldRegistrationId != null && !oldRegistrationId.equals(registrationId)) {
            Intent pushTokenChangedIntent = new Intent(WonderPush.INTENT_PUSH_TOKEN_CHANGED);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_OLD_KNOWN_PUSH_TOKEN, oldRegistrationId);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_PUSH_TOKEN, registrationId);
            LocalBroadcastManager.getInstance(context).sendBroadcast(pushTokenChangedIntent);
        }
    }

}
