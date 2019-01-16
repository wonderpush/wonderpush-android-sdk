package com.wonderpush.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Implementation of {@link IWonderPush} that performs the required tasks.
 *
 * This implementation is used when user consent is either provided or not required.
 */
class WonderPushImpl implements IWonderPush {

    @Override
    public void _activate() {
    }

    @Override
    public void _deactivate() {
    }

    @Override
    public String getAccessToken() {
        String accessToken = null;
        try {
            accessToken = WonderPushConfiguration.getAccessToken();
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while getting accessToken", e);
        }
        return accessToken;
    }

    @Override
    public String getDeviceId() {
        String deviceId = null;
        try {
            deviceId = WonderPushConfiguration.getDeviceId();
            if (deviceId == null) {
                // Read from OpenUDID storage to keep a smooth transition off using OpenUDID
                SharedPreferences sharedPrefs =  WonderPush.getApplicationContext().getSharedPreferences("openudid_prefs", Context.MODE_PRIVATE);
                deviceId = sharedPrefs.getString("openudid", null);
                if (deviceId == null) {
                    // Generate an UUIDv4
                    deviceId = UUID.randomUUID().toString();
                }
                // and store it for us
                WonderPushConfiguration.setDeviceId(deviceId);
            }
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while getting deviceId", e);
        }
        return deviceId;
    }

    @Override
    public String getInstallationId() {
        String installationId = null;
        try {
            installationId = WonderPushConfiguration.getInstallationId();
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while getting installationId", e);
        }
        return installationId;
    }

    @Override
    public String getPushToken() {
        String pushToken = null;
        try {
            pushToken = WonderPushConfiguration.getGCMRegistrationId();
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while getting pushToken", e);
        }
        return pushToken;
    }

    @Override
    public boolean getNotificationEnabled() {
        return WonderPushConfiguration.getNotificationEnabled();
    }

    @Override
    public void setNotificationEnabled(boolean status) {
        try {
            WonderPush.logDebug("Set notification enabled: " + status);
            if (status == WonderPushConfiguration.getNotificationEnabled()) {
                WonderPush.logDebug("Set notification enabled: no change to apply");
                return;
            }
            String value = status
                    ? WonderPush.INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTIN
                    : WonderPush.INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTOUT;
            JSONObject properties = new JSONObject();
            JSONObject preferences = new JSONObject();
            properties.put("preferences", preferences);
            preferences.put("subscriptionStatus", value);
            InstallationManager.updateInstallation(properties, false);
            WonderPushConfiguration.setNotificationEnabled(status);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while setting notification enabled to " + status, e);
        }
    }

    @Override
    public JSONObject getInstallationCustomProperties() {
        JSONObject rtn = InstallationManager.getInstallationCustomProperties();
        WonderPush.logDebug("getInstallationCustomProperties() -> " + rtn);
        return rtn;
    }

    @Override
    public void putInstallationCustomProperties(JSONObject customProperties) {
        try {
            InstallationManager.putInstallationCustomProperties(customProperties);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while putting installation custom properties", e);
        }
    }

    @Override
    public void trackEvent(String type) {
        try {
            trackEvent(type, null);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while tracking user event of type \"" + type + "\"", e);
        }
    }

    @Override
    public void trackEvent(String type, JSONObject customData) {
        try {
            WonderPush.logDebug("trackEvent(" + type + ", " + customData + ")");
            WonderPush.trackEvent(type, null, customData);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while tracking user event of type \"" + type + "\"", e);
        }
    }

}
