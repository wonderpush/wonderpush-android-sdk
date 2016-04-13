package com.wonderpush.sdk;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;

public class WonderPushRegistrationIntentService extends IntentService {

    private static final String TAG = WonderPush.TAG;

    public WonderPushRegistrationIntentService() {
        super("WonderPushRegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
                WonderPush.logError("Device could not register");
            } else {
                WonderPush.logDebug("Device registered, registration ID=" + token);
            }

            storeRegistrationId(pushSenderId, token);
        } catch (Exception e) {
            Log.w(TAG, "Could not register for push notifications", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
        }
    }

    private void storeRegistrationId(String senderIds, String registrationId) {
        WonderPushConfiguration.initialize(this);
        try {
            if (
                    // New registration id
                       registrationId == null && WonderPushConfiguration.getGCMRegistrationId() != null
                    || registrationId != null && !registrationId.equals(WonderPushConfiguration.getGCMRegistrationId())
                    // Last associated with an other userId?
                    || WonderPushConfiguration.getUserId() == null && WonderPushConfiguration.getCachedGCMRegistrationIdAssociatedUserId() != null
                    || WonderPushConfiguration.getUserId() != null && !WonderPushConfiguration.getUserId().equals(WonderPushConfiguration.getCachedGCMRegistrationIdAssociatedUserId())
            ) {
                JSONObject properties = new JSONObject();
                JSONObject pushToken = new JSONObject();
                pushToken.put("data", registrationId);
                properties.put("pushToken", pushToken);

                WonderPush.ensureInitialized(this);
                WonderPush.updateInstallation(properties, false, null);
                WonderPushConfiguration.setCachedGCMRegistrationIdDate(System.currentTimeMillis());
                WonderPushConfiguration.setCachedGCMRegistrationIdAssociatedUserId(WonderPushConfiguration.getUserId());
            }
        } catch (JSONException e) {
            Log.e(WonderPush.TAG, "Failed to update push token to WonderPush", e);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while updating push token to WonderPush", e);
        }

        WonderPushConfiguration.setGCMRegistrationId(registrationId);
        WonderPushConfiguration.setGCMRegistrationSenderIds(senderIds);
        WonderPushConfiguration.setGCMRegistrationAppVersion(WonderPush.getApplicationVersionCode());
    }

}
