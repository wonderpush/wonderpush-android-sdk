package com.wonderpush.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
    public void subscribeToNotifications() {
        setNotificationEnabled(true);
    }

    @Override
    public void unsubscribeFromNotifications() {
        setNotificationEnabled(false);
    }

    @Override
    public boolean isSubscribedToNotifications() {
        return getNotificationEnabled();
    }

    @Override
    @Deprecated
    public boolean getNotificationEnabled() {
        return WonderPushConfiguration.getNotificationEnabled();
    }

    @Override
    @Deprecated
    public void setNotificationEnabled(boolean status) {
        try {
            WonderPush.logDebug("Set notification enabled: " + status);

            boolean previousStatus = WonderPushConfiguration.getNotificationEnabled();
            boolean osAreNotificationsEnabled = NotificationManagerCompat.from(WonderPush.getApplicationContext()).areNotificationsEnabled();
            boolean cachedOsAreNotificationsEnabled = WonderPushConfiguration.getCachedOsAreNotificationsEnabled();
            Set<String> cachedDisabledChannels = WonderPushConfiguration.getCachedDisabledNotificationChannelIds();
            Set<String> disabledChannels = WonderPushUserPreferences.getDisabledChannelIds();
            String previousValue = previousStatus && cachedOsAreNotificationsEnabled
                    ? WonderPush.INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTIN
                    : WonderPush.INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTOUT;
            String value = status && osAreNotificationsEnabled
                    ? WonderPush.INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTIN
                    : WonderPush.INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTOUT;

            if (previousStatus == status
                    && value.equals(previousValue)
                    && osAreNotificationsEnabled == cachedOsAreNotificationsEnabled
                    && disabledChannels.equals(cachedDisabledChannels)
            ) {
                WonderPush.logDebug("Set notification enabled: no change to apply");
                return;
            }

            JSONObject properties = new JSONObject();
            JSONObject preferences = new JSONObject();
            properties.put("preferences", preferences);
            preferences.put("subscriptionStatus", value);
            preferences.put("subscribedToNotifications", status);
            preferences.put("osNotificationsVisible", osAreNotificationsEnabled);
            preferences.put("disabledAndroidChannels", new JSONArray(disabledChannels));
            InstallationManager.updateInstallation(properties, false);
            WonderPushConfiguration.setNotificationEnabled(status);
            WonderPushConfiguration.setCachedOsAreNotificationsEnabled(osAreNotificationsEnabled);
            WonderPushConfiguration.setCachedOsAreNotificationsEnabledDate(TimeSync.getTime());
            WonderPushConfiguration.setCachedDisabledNotificationChannelIds(disabledChannels);
            WonderPushConfiguration.setCachedDisabledNotificationChannelIdsDate(TimeSync.getTime());
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while setting notification enabled to " + status, e);
        }
    }

    @Override
    public JSONObject getProperties() {
        return getInstallationCustomProperties();
    }

    @Override
    public void putProperties(JSONObject properties) {
        putInstallationCustomProperties(properties);
    }

    @Override
    @Deprecated
    public JSONObject getInstallationCustomProperties() {
        JSONObject rtn = InstallationManager.getInstallationCustomProperties();
        WonderPush.logDebug("getInstallationCustomProperties() -> " + rtn);
        return rtn;
    }

    @Override
    @Deprecated
    public void putInstallationCustomProperties(JSONObject customProperties) {
        try {
            InstallationManager.putInstallationCustomProperties(customProperties);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while putting installation custom properties", e);
        }
    }

    @Override
    public void setProperty(String field, Object value) {
        try {
            InstallationManager.setProperty(field, value);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while calling setProperty", e);
        }
    }

    @Override
    public void unsetProperty(String field) {
        try {
            InstallationManager.unsetProperty(field);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while calling unsetProperty", e);
        }
    }

    @Override
    public void addProperty(String field, Object value) {
        try {
            InstallationManager.addProperty(field, value);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while calling addProperty", e);
        }
    }

    @Override
    public void removeProperty(String field, Object value) {
        try {
            InstallationManager.removeProperty(field, value);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while calling removeProperty", e);
        }
    }

    @Override
    public Object getPropertyValue(String field) {
        try {
            return InstallationManager.getPropertyValue(field);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while calling getPropertyValue", e);
            return JSONObject.NULL;
        }
    }

    @Override
    public List<Object> getPropertyValues(String field) {
        try {
            return InstallationManager.getPropertyValues(field);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while calling getPropertyValues", e);
            return Collections.emptyList();
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

    @Override
    public void addTag(String... tag) {
        try {
            InstallationManager.addTag(tag);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while adding tags " + Arrays.toString(tag), e);
        }
    }

    @Override
    public void removeTag(String... tag) {
        try {
            InstallationManager.removeTag(tag);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while removing tags " + Arrays.toString(tag), e);
        }
    }

    @Override
    public void removeAllTags() {
        try {
            InstallationManager.removeAllTags();
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while removing all tags", e);
        }
    }

    @Override
    public Set<String> getTags() {
        try {
            return InstallationManager.getTags();
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while getting tags", e);
            return new TreeSet<>();
        }
    }

    @Override
    public boolean hasTag(String tag) {
        try {
            return InstallationManager.hasTag(tag);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while testing tag " + tag, e);
            return false;
        }
    }

}
