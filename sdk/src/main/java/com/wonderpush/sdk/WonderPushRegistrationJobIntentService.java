package com.wonderpush.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;

public class WonderPushRegistrationJobIntentService extends JobIntentService {

    private static final String TAG = WonderPush.TAG;

    static final int JOB_ID = 0x0F7694E0; // CRC32("WonderPushRegistrationJobIntentService")

    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WonderPushRegistrationJobIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            String pushSenderId = null;
            try {
                ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                pushSenderId = bundle.getString("GCMSenderId");
            } catch (PackageManager.NameNotFoundException | NullPointerException e) {
                Log.e(TAG, "Could not get GCMSenderId meta data from your manifest. Did you add: <meta-data android:name=\"GCMSenderId\" android:value=\"@string/push_sender_ids\"/> under <application> in your AndroidManifest.xml?");
            }

            if (pushSenderId == null) {
                Log.w(TAG, "Defaulting to built-in push_sender_ids string resource");
                pushSenderId = getString(R.string.push_sender_ids);
            }

            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(pushSenderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            if (token == null) {
                Log.e(WonderPush.TAG, "Device could not register");
            } else {
                WonderPush.logDebug("Device registered, registration ID=" + token);
            }

            storeRegistrationId(this, pushSenderId, token);
        } catch (Exception e) {
            Log.w(TAG, "Could not register for push notifications", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
        }
    }

    static void storeRegistrationId(Context context, String senderIds, String registrationId) {
        WonderPushConfiguration.initialize(context);
        String oldRegistrationId = WonderPushConfiguration.getGCMRegistrationId();
        try {
            if (
                    // New registration id
                       registrationId == null && WonderPushConfiguration.getGCMRegistrationId() != null
                    || registrationId != null && !registrationId.equals(WonderPushConfiguration.getGCMRegistrationId())
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
        WonderPushConfiguration.setGCMRegistrationAppVersion(InstallationManager.getApplicationVersionCode());

        if (oldRegistrationId == null && registrationId != null
                || oldRegistrationId != null && !oldRegistrationId.equals(registrationId)) {
            Intent pushTokenChangedIntent = new Intent(WonderPush.INTENT_PUSH_TOKEN_CHANGED);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_OLD_KNOWN_PUSH_TOKEN, oldRegistrationId);
            pushTokenChangedIntent.putExtra(WonderPush.INTENT_PUSH_TOKEN_CHANGED_EXTRA_PUSH_TOKEN, registrationId);
            LocalBroadcastManager.getInstance(context).sendBroadcast(pushTokenChangedIntent);
        }
    }

}
