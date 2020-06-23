package com.wonderpush.sdk.push;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.wonderpush.sdk.InstallationManager;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.TreeMap;

public class PushServiceManager {

    private static final String TAG = "WonderPush.Push";

    private static Context sContext;

    private static TreeMap<String, PushService> sKnownPushServices = new TreeMap<>();
    private static PushService sUsedPushService;

    public static void initialize(Context context) {
        sContext = context.getApplicationContext();
        for (PushService pushService : DiscoveryService.instantiatePushServices(context)) {
            register(pushService);
        }
        initializePushServices();
    }

    public static void register(PushService service) {
        Log.d(TAG, "Registering push service: " + service.getIdentifier() + ": " + service.getName() + " v" + service.getVersion());
        sKnownPushServices.put(service.getIdentifier(), service);
    }

    private static void initializePushServices() {
        Log.d(TAG, "Known push services:");
        for (PushService pushService : sKnownPushServices.values()) {
            Log.d(TAG, "- " + pushService.getIdentifier() + ": " + pushService.getName() + " v" + pushService.getVersion());
            pushService.initialize(sContext);
            if (sUsedPushService == null && pushService.isAvailable()) {
                sUsedPushService = pushService;
            }
        }
        Log.d(TAG, "Used push service: " + (sUsedPushService == null ? "(none)" : sUsedPushService.getIdentifier() + ": " + sUsedPushService.getName() + " v" + sUsedPushService.getVersion()));
    }

    public static void refreshSubscription() {
        if (sUsedPushService == null) {
            Log.e(TAG, "Cannot refresh push subscription, no push service available");
            return;
        }
        sUsedPushService.execute();
    }

    public static int getNotificationIcon() {
        if (sUsedPushService == null) return 0;
        return sUsedPushService.getNotificationIcon();
    }

    public static int getNotificationColor() {
        if (sUsedPushService == null) return 0;
        return sUsedPushService.getNotificationColor();
    }

    public static void onResult(PushServiceResult result) {
        if (WonderPush.getLogging()) Log.d(TAG, "onResult(" + result + ")");
        WonderPushConfiguration.initialize(sContext);
        String oldRegistrationId = WonderPushConfiguration.getGCMRegistrationId();
        String registrationId = result.getData();
        String senderIds = result.getSenderIds();
        String service = result.getService();
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
                pushToken.put("service", service);
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
                properties.put("pushToken", pushToken);

                WonderPush.initialize(sContext);
                InstallationManager.updateInstallation(properties, false);
                WonderPushConfiguration.setCachedGCMRegistrationIdDate(System.currentTimeMillis());
                WonderPushConfiguration.setCachedGCMRegistrationIdAssociatedUserId(WonderPushConfiguration.getUserId());
                WonderPushConfiguration.setCachedGCMRegistrationIdAccessToken(WonderPushConfiguration.getAccessToken());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update push token to WonderPush", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while updating push token to WonderPush", e);
        }

        WonderPushConfiguration.setGCMRegistrationId(registrationId);
        WonderPushConfiguration.setGCMRegistrationSenderIds(senderIds);

        if (oldRegistrationId == null && registrationId != null
                || oldRegistrationId != null && !oldRegistrationId.equals(registrationId)) {
            Intent pushTokenChangedIntent = new Intent(WonderPush.INTENT_PUSH_TOKEN_CHANGED);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_OLD_KNOWN_PUSH_TOKEN, oldRegistrationId);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_PUSH_TOKEN, registrationId);
            LocalBroadcastManager.getInstance(sContext).sendBroadcast(pushTokenChangedIntent);
        }

    }

}
