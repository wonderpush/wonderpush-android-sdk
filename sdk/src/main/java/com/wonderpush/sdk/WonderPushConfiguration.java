package com.wonderpush.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class WonderPushConfiguration {

    private static final String PREF_FILE = "wonderpush";

    private static final String PER_USER_ARCHIVE_PREF_NAME = "__per_user_archive";

    private static final String ACCESS_TOKEN_PREF_NAME = "__wonderpush_access_token";
    private static final String SID_PREF_NAME = "__wonderpush_sid";
    private static final String INSTALLATION_ID_PREF_NAME = "__installation_id";
    private static final String USER_ID_PREF_NAME = "__user_id";

    private static final String NOTIFICATION_ENABLED_PREF_NAME = "__wonderpush_notification_enabled";
    private static final String CHANNEL_PREFERENCES_PREF_NAME = "__wonderpush_channel_preferences";

    private static final String CACHED_INSTALLATION_CORE_PROPERTIES_NAME = "__cached_installation_core_properties";
    private static final String CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME = "__cached_installation_core_properties_date";
    private static final String CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME = "__cached_installation_core_properties_access_token";

    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME = "__cached_installation_custom_properties_written";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME = "__cached_installation_custom_properties_written_date";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME = "__cached_installation_custom_properties_updated";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME = "__cached_installation_custom_properties_updated_date";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME = "__cached_installation_custom_properties_first_delayed_write_date";
    private static final String INSTALLATION_CUSTOM_SYNC_STATE_PER_USER_ID_PREF_NAME = "__installation_sync_state_per_user_id";

    private static final String GCM_REGISTRATION_ID_PREF_NAME = "__wonderpush_gcm_registration_id";
    private static final String CACHED_GCM_REGISTRATION_ID_PREF_DATE_NAME = "__wonderpush_gcm_registration_id_date";
    private static final String CACHED_GCM_REGISTRATION_ID_ASSOCIATED_TO_USER_ID_PREF_NAME = "__wonderpush_gcm_registration_id_associated_to_user_id";
    private static final String CACHED_GCM_REGISTRATION_ID_PREF_ACCESS_TOKEN_NAME = "__wonderpush_gcm_registration_id_access_token";
    private static final String GCM_REGISTRATION_APP_VERSION_PREF_NAME = "__wonderpush_gcm_registration_app_version";
    private static final String GCM_REGISTRATION_APP_VERSION_FOR_UPDATE_RECEIVER_PREF_NAME = "__wonderpush_gcm_registration_app_version_for_update_receiver";
    private static final String GCM_REGISTRATION_SENDER_IDS_PREF_NAME = "__wonderpush_gcm_registration_sender_ids";

    private static final String LAST_RECEIVED_NOTIFICATION_INFO_JSON_PREF_NAME = "__last_received_notification_info_json";
    private static final String LAST_OPENED_NOTIFICATION_INFO_JSON_PREF_NAME = "__last_opened_notification_info_json";

    private static final String LAST_INTERACTION_DATE_PREF_NAME = "__last_interaction_date";
    private static final String LAST_APPOPEN_DATE_PREF_NAME = "__last_appopen_date";
    private static final String LAST_APPOPEN_INFO_PREF_NAME = "__last_appopen_info_json";
    private static final String LAST_APPCLOSE_DATE_PREF_NAME = "__last_appclose_date";

    private static final String DEVICE_DATE_SYNC_OFFSET_PREF_NAME = "__device_date_sync_offset";
    private static final String DEVICE_DATE_SYNC_UNCERTAINTY_PREF_NAME = "__device_date_sync_uncertainty";

    private static final String LAST_TAGLESS_NOTIFICATION_MANAGER_ID_PREF_NAME = "__last_tagless_notification_manager_id";

    private static final String OVERRIDE_SET_LOGGING_PREF_NAME = "__override_set_logging";
    private static final String OVERRIDE_NOTIFICATION_RECEIPT_PREF_NAME = "__override_notification_receipt";

    private static Context sContext;

    static void initialize(Context context) {
        sContext = context.getApplicationContext();
    }

    protected static boolean isInitialized() {
        return sContext != null;
    }

    protected static Context getApplicationContext() {
        if (sContext != null) {
            return sContext;
        } else {
            if (WonderPush.getApplicationContext() == null) {
                Log.e(WonderPush.TAG, "WonderPushConfiguration is not initialized, nor is WonderPush, returning null context", new NullPointerException("Stack"));
            } else {
                Log.w(WonderPush.TAG, "WonderPushConfiguration is not initialized, but WonderPush is, returning its context", new NullPointerException("Stack"));
            }
            return WonderPush.getApplicationContext();
        }
    }

    static void changeUserId(String newUserId) {
        if (newUserId == null && getUserId() == null
                || newUserId != null && newUserId.equals(getUserId())) {
            // No userId change
            return;
        }
        WonderPush.logDebug("archiving storage for user " + getUserId());
        // Save current user preferences
        try {
            JSONObject currentUserArchive = new JSONObject();
            currentUserArchive.putOpt(ACCESS_TOKEN_PREF_NAME, getAccessToken());
            currentUserArchive.putOpt(SID_PREF_NAME, getSID());
            currentUserArchive.putOpt(INSTALLATION_ID_PREF_NAME, getInstallationId());
            currentUserArchive.putOpt(USER_ID_PREF_NAME, getUserId());
            currentUserArchive.putOpt(NOTIFICATION_ENABLED_PREF_NAME, getNotificationEnabled());
            currentUserArchive.putOpt(CHANNEL_PREFERENCES_PREF_NAME, getChannelPreferences());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CORE_PROPERTIES_NAME, getCachedInstallationCoreProperties());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME, getCachedInstallationCorePropertiesDate());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME, getCachedInstallationCorePropertiesAccessToken());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME, getCachedInstallationCustomPropertiesWritten());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME, getCachedInstallationCustomPropertiesWrittenDate());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME, getCachedInstallationCustomPropertiesUpdated());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME, getCachedInstallationCustomPropertiesUpdatedDate());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME, getCachedInstallationCustomPropertiesFirstDelayedWrite());
            currentUserArchive.putOpt(LAST_INTERACTION_DATE_PREF_NAME, getLastInteractionDate());
            currentUserArchive.putOpt(LAST_APPOPEN_DATE_PREF_NAME, getLastAppOpenDate());
            currentUserArchive.putOpt(LAST_APPOPEN_INFO_PREF_NAME, getLastAppOpenInfoJson());
            currentUserArchive.putOpt(LAST_APPCLOSE_DATE_PREF_NAME, getLastAppCloseDate());
            JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
            if (usersArchive == null) {
                usersArchive = new JSONObject();
            }
            usersArchive.put(getUserId() == null ? "" : getUserId(), currentUserArchive);
            putJSONObject(PER_USER_ARCHIVE_PREF_NAME, usersArchive);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to save current user preferences", ex);
        }
        // Load new user preferences
        WonderPush.logDebug("loading storage for user " + newUserId);
        JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
        if (usersArchive == null) usersArchive = new JSONObject();
        JSONObject newUserArchive = usersArchive.optJSONObject(newUserId == null ? "" : newUserId);
        if (newUserArchive == null) newUserArchive = new JSONObject();
        setAccessToken(JSONUtil.optString(newUserArchive, ACCESS_TOKEN_PREF_NAME));
        setSID(JSONUtil.optString(newUserArchive, SID_PREF_NAME));
        setInstallationId(JSONUtil.optString(newUserArchive, INSTALLATION_ID_PREF_NAME));
        setUserId(newUserId);
        setNotificationEnabled(newUserArchive.optBoolean(NOTIFICATION_ENABLED_PREF_NAME, true));
        setChannelPreferences(newUserArchive.optJSONObject(CHANNEL_PREFERENCES_PREF_NAME));
        setCachedInstallationCoreProperties(JSONUtil.optString(newUserArchive, CACHED_INSTALLATION_CORE_PROPERTIES_NAME));
        setCachedInstallationCorePropertiesDate(newUserArchive.optLong(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME));
        setCachedInstallationCorePropertiesAccessToken(JSONUtil.optString(newUserArchive, CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME));
        setCachedInstallationCustomPropertiesWritten(newUserArchive.optJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME));
        setCachedInstallationCustomPropertiesWrittenDate(newUserArchive.optLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME));
        setCachedInstallationCustomPropertiesUpdated(newUserArchive.optJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME));
        setCachedInstallationCustomPropertiesUpdatedDate(newUserArchive.optLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME));
        setCachedInstallationCustomPropertiesFirstDelayedWrite(newUserArchive.optLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME));
        setLastInteractionDate(newUserArchive.optLong(LAST_INTERACTION_DATE_PREF_NAME));
        setLastAppOpenDate(newUserArchive.optLong(LAST_APPOPEN_DATE_PREF_NAME));
        setLastAppOpenInfoJson(newUserArchive.optJSONObject(LAST_APPOPEN_INFO_PREF_NAME));
        setLastAppCloseDate(newUserArchive.optLong(LAST_APPCLOSE_DATE_PREF_NAME));
    }

    static JSONObject dumpState() {
        JSONObject rtn = new JSONObject();
        SharedPreferences prefs = getSharedPreferences();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Object value = JSONUtil.parseAllJSONStrings(entry.getValue());
            try {
                rtn.put(entry.getKey(), value);
            } catch (JSONException ex) {
                Log.e(WonderPush.TAG, "Failed to add key " + entry.getKey() + " to state dump for value: " + entry.getValue(), ex);
            }
        }
        return rtn;
    }

    /**
     * Gets the WonderPush shared preferences for that application.
     */
    static SharedPreferences getSharedPreferences() {
        if (null == getApplicationContext())
            return null;
        SharedPreferences rtn = getApplicationContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        if (null == rtn) {
            Log.e(WonderPush.TAG, "Could not get shared preferences", new NullPointerException("Stack"));
        }
        return rtn;
    }

    private static boolean has(String key) {
        SharedPreferences prefs = getSharedPreferences();
        if (prefs == null) {
            return false;
        }
        return prefs.contains(key);
    }

    private static void remove(String key) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(key);
        editor.apply();
    }

    private static String getString(String key) {
        return getString(key, null);
    }

    private static String getString(String key, String defaultValue) {
        SharedPreferences prefs = getSharedPreferences();
        if (prefs == null) {
            return defaultValue;
        }
        return prefs.getString(key, defaultValue);
    }

    private static void putString(String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        if (null == value) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
        editor.apply();
    }

    private static JSONObject getJSONObject(String key) {
        String json = getString(key);
        if (json != null) {
            try {
                return new JSONObject(json);
            } catch (JSONException e) {
                Log.w(WonderPush.TAG, "Failed to decode json from preferences", e);
            }
        }
        return null;
    }

    private static void putJSONObject(String key, JSONObject value) {
        putString(key, value == null ? null : value.toString());
    }

    @SuppressWarnings("unused")
    private static int getInt(String key) {
        return getInt(key, 0);
    }

    private static int getInt(String key, int defaultValue) {
        SharedPreferences prefs = getSharedPreferences();
        if (prefs == null) {
            return defaultValue;
        }
        return prefs.getInt(key, defaultValue);
    }

    private static void putInt(String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private static long getLong(String key, long defaultValue) {
        SharedPreferences prefs = getSharedPreferences();
        if (prefs == null) {
            return defaultValue;
        }
        return prefs.getLong(key, defaultValue);
    }

    private static void putLong(String key, long value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(key, value);
        editor.apply();
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences prefs = getSharedPreferences();
        if (prefs == null) {
            return defaultValue;
        }
        return prefs.getBoolean(key, defaultValue);
    }

    private static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Get the user id stored in the user's shared preferences.
     */
    static String getUserId() {
        return getString(USER_ID_PREF_NAME);
    }

    /**
     * Set the user id stored in the user's shared preferences.
     *
     * @param userId
     *            The user id to be stored
     */
    static void setUserId(String userId) {
        putString(USER_ID_PREF_NAME, userId);
    }

    static List<String> listKnownUserIds() {
        List<String> rtn = new ArrayList<>();
        JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
        if (usersArchive == null) usersArchive = new JSONObject();
        Iterator<String> it = usersArchive.keys();
        while (it.hasNext()) {
            String userId = it.next();
            if (userId != null && userId.length() == 0) userId = null;
            rtn.add(userId);
        }
        if (!rtn.contains(getUserId())) {
            rtn.add(getUserId());
        }
        return rtn;
    }

    /**
     * Remove old access token, installation id and SID.
     * For instance when the user id changes, the device will get a new identity, so we must clear the old one.
     * When we face an invalid access token or SID error, we must also clear the stored information.
     */
    static void invalidateCredentials() {
        setAccessToken(null);
        setInstallationId(null);
        setSID(null);
    }

    /**
     * Get the access token associated to a given user's shared preferences.
     */
    static String getAccessTokenForUserId(String userId) {
        if (userId == null && getUserId() == null
                || userId != null && userId.equals(getUserId())) {
            return getAccessToken();
        } else {
            JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
            if (usersArchive == null) usersArchive = new JSONObject();
            JSONObject userArchive = usersArchive.optJSONObject(userId == null ? "" : userId);
            if (userArchive == null) userArchive = new JSONObject();
            return JSONUtil.optString(userArchive, ACCESS_TOKEN_PREF_NAME);
        }
    }

    /**
     * Get the access token stored in the user's shared preferences.
     */
    static String getAccessToken() {
        return getString(ACCESS_TOKEN_PREF_NAME);
    }

    /**
     * Set the access token stored in the user's shared preferences.
     *
     * @param accessToken
     *            The access token to be stored
     */
    static void setAccessToken(String accessToken) {
        WonderPush.logDebug("Setting accessToken = " + accessToken);
        putString(ACCESS_TOKEN_PREF_NAME, accessToken);
    }

    /**
     * Get the installation id stored in the user's shared preferences.
     */
    static String getInstallationId() {
        return getString(INSTALLATION_ID_PREF_NAME);
    }

    /**
     * Set the installation id stored in the user's shared preferences.
     *
     * @param installationId
     *            The installation id to be stored
     */
    static void setInstallationId(String installationId) {
        WonderPush.logDebug("Setting installationId = " + installationId);
        putString(INSTALLATION_ID_PREF_NAME, installationId);
    }

    /**
     * Get the SID stored in the user's shared preferences.
     */
    static String getSID() {
        return getString(SID_PREF_NAME);
    }

    /**
     * Set the SID stored in the user's shared preferences.
     *
     * @param sid
     *            The SID to be stored
     */
    static void setSID(String sid) {
        WonderPush.logDebug("Setting sid = " + sid);
        putString(SID_PREF_NAME, sid);
    }

    /**
     * Get the notification enabled stored in the user's shared preferences.
     */
    static boolean getNotificationEnabled() {
        return getBoolean(NOTIFICATION_ENABLED_PREF_NAME, true);
    }

    /**
     * Set the notification enabled status stored in the user's shared preferences.
     *
     * @param status
     *            The notification enabled status to be stored
     */
    static void setNotificationEnabled(boolean status) {
        putBoolean(NOTIFICATION_ENABLED_PREF_NAME, status);
    }

    /**
     * Get the notification channel preferences in the user's shared preferences.
     */
    static JSONObject getChannelPreferences() {
        return getJSONObject(CHANNEL_PREFERENCES_PREF_NAME);
    }

    /**
     * Set the notification channel preferences in the user's shared preferences.
     *
     * @param value
     *            The notification channel preferences to be stored
     */
    static void setChannelPreferences(JSONObject value) {
        putJSONObject(CHANNEL_PREFERENCES_PREF_NAME, value);
    }

    /**
     * Get the cached installation core properties stored in the user's shared preferences.
     */
    static String getCachedInstallationCoreProperties() {
        return getString(CACHED_INSTALLATION_CORE_PROPERTIES_NAME);
    }

    /**
     * Set the cached installation core properties stored in the user's shared preferences.
     *
     * @param cachedInstallationCoreProperties
     *            The cached installation core properties to be stored
     */
    static void setCachedInstallationCoreProperties(String cachedInstallationCoreProperties) {
        putString(CACHED_INSTALLATION_CORE_PROPERTIES_NAME, cachedInstallationCoreProperties);
    }

    /**
     * Get the cached installation core properties date stored in the user's shared preferences.
     */
    static long getCachedInstallationCorePropertiesDate() {
        return getLong(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME, 0);
    }

    /**
     * Set the cached installation core properties date stored in the user's shared preferences.
     *
     * @param cachedInstallationCorePropertiesDate
     *            The cached installation core properties date to be stored
     */
    static void setCachedInstallationCorePropertiesDate(long cachedInstallationCorePropertiesDate) {
        putLong(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME, cachedInstallationCorePropertiesDate);
    }

    /**
     * Get the cached installation core properties stored in the user's shared preferences.
     */
    static String getCachedInstallationCorePropertiesAccessToken() {
        return getString(CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME);
    }

    /**
     * Set the cached installation core properties access token stored in the user's shared preferences.
     *
     * @param cachedInstallationCorePropertiesAccessToken
     *            The cached installation core properties access token to be stored
     */
    static void setCachedInstallationCorePropertiesAccessToken(String cachedInstallationCorePropertiesAccessToken) {
        putString(CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME, cachedInstallationCorePropertiesAccessToken);
    }

    /**
     * Get the partial object representing the written installation code properties so far stored in the user's shared preferences.
     * This object is updated whenever grouped updates are performed.
     */
    static JSONObject getCachedInstallationCustomPropertiesWritten() {
        return getJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME);
    }

    /**
     * Set the partial object representing the written installation code properties so far stored in the user's shared preferences.
     *
     * @param cachedInstallationCustomPropertiesWritten
     *            The partial object representing the written installation code properties so far to be stored.
     */
    static void setCachedInstallationCustomPropertiesWritten(JSONObject cachedInstallationCustomPropertiesWritten) {
        putJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME, cachedInstallationCustomPropertiesWritten);
    }

    /**
     * Get the date of the last write to installation custom properties that has been performed.
     */
    static long getCachedInstallationCustomPropertiesWrittenDate() {
        return getLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME, 0);
    }

    /**
     * Set the date of the last write to installation custom properties that has been performed.
     * @param cachedInstallationCustomPropertiesWrittenDate
     *            The date of the last write to installation custom properties that has been performed to store.
     */
    static void setCachedInstallationCustomPropertiesWrittenDate(long cachedInstallationCustomPropertiesWrittenDate) {
        putLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME, cachedInstallationCustomPropertiesWrittenDate);
    }

    /**
     * Get the partial object representing the updated installation code properties stored in the user's shared preferences.
     * This object is updated whenever updates are demanded, whether delayed for grouping or already written.
     */
    static JSONObject getCachedInstallationCustomPropertiesUpdated() {
        return getJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME);
    }

    /**
     * Set the partial object representing the updated installation code properties stored in the user's shared preferences.
     *
     * @param cachedInstallationCustomPropertiesUpdated
     *            The partial object representing the updated installation code properties to be stored.
     */
    static void setCachedInstallationCustomPropertiesUpdated(JSONObject cachedInstallationCustomPropertiesUpdated) {
        putJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME, cachedInstallationCustomPropertiesUpdated);
    }

    /**
     * Get the date of the last update to {@link #getCachedInstallationCustomPropertiesUpdated()}.
     * This is the date of the last non-noop write to installation custom properties that has been delayed for grouping.
     */
    static long getCachedInstallationCustomPropertiesUpdatedDate() {
        return getLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME, 0);
    }

    /**
     * Set the date of the last update to {@link #getCachedInstallationCustomPropertiesUpdated()}.
     * This is the date of the last non-noop write to installation custom properties that has been delayed for grouping.
     * @param cachedInstallationCustomPropertiesUpdatedDate
     *            The date of the last update to {@link #getCachedInstallationCustomPropertiesUpdated()}.
     */
    static void setCachedInstallationCustomPropertiesUpdatedDate(long cachedInstallationCustomPropertiesUpdatedDate) {
        putLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME, cachedInstallationCustomPropertiesUpdatedDate);
    }

    /**
     * The date of the first non-noop write to installation custom properties that has been delayed for grouping.
     */
    static long getCachedInstallationCustomPropertiesFirstDelayedWrite() {
        return getLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME, 0);
    }

    /**
     * Set the date of the first non-noop write to installation custom properties that has been delayed for grouping.
     * @param cachedInstallationCustomPropertiesFirstDelayedWrite
     *            The date of the first non-noop write to installation custom properties that has been delayed for grouping to store.
     */
    static void setCachedInstallationCustomPropertiesFirstDelayedWrite(long cachedInstallationCustomPropertiesFirstDelayedWrite) {
        putLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME, cachedInstallationCustomPropertiesFirstDelayedWrite);
    }

    /**
     * Get the saved state of installation custom sync for all users.
     */
    static JSONObject getInstallationCustomSyncStatePerUserId() {
        return getJSONObject(INSTALLATION_CUSTOM_SYNC_STATE_PER_USER_ID_PREF_NAME);
    }

    /**
     * Set the saved state of installation custom sync for all users.
     *
     * @param installationCustomSyncStatePerUserId
     *            The saved state of installation custom sync for all users to be stored.
     */
    static void setInstallationCustomSyncStatePerUserId(JSONObject installationCustomSyncStatePerUserId) {
        putJSONObject(INSTALLATION_CUSTOM_SYNC_STATE_PER_USER_ID_PREF_NAME, installationCustomSyncStatePerUserId);
    }


    /**
     * Get the registration id stored in the user's shared preferences.
     */
    static String getGCMRegistrationId() {
        return getString(GCM_REGISTRATION_ID_PREF_NAME);
    }

    /**
     * Set the registration stored in the user's shared preferences.
     *
     * @param registrationId
     *            The registration id to be stored
     */
    static void setGCMRegistrationId(String registrationId) {
        putString(GCM_REGISTRATION_ID_PREF_NAME, registrationId);
    }

    /**
     * Get the cached registration id date stored in the user's shared preferences.
     */
    static long getCachedGCMRegistrationIdDate() {
        return getLong(CACHED_GCM_REGISTRATION_ID_PREF_DATE_NAME, 0);
    }

    /**
     * Set the cached registration id date as stored in the user's shared preferences.
     *
     * @param cachedGCMRegistrationIdDate
     *            The cached registration id date to be stored
     */
    static void setCachedGCMRegistrationIdDate(long cachedGCMRegistrationIdDate) {
        putLong(CACHED_GCM_REGISTRATION_ID_PREF_DATE_NAME, cachedGCMRegistrationIdDate);
    }

    /**
     * Get the user id the registration id was associated with in the user's shared preferences.
     */
    static String getCachedGCMRegistrationIdAssociatedUserId() {
        return getString(CACHED_GCM_REGISTRATION_ID_ASSOCIATED_TO_USER_ID_PREF_NAME);
    }

    /**
     * Set the user id the registration was associated with in the user's shared preferences.
     *
     * @param userId
     *            The associated user id to be stored
     */
    static void setCachedGCMRegistrationIdAssociatedUserId(String userId) {
        putString(CACHED_GCM_REGISTRATION_ID_ASSOCIATED_TO_USER_ID_PREF_NAME, userId);
    }

    /**
     * Get the cached registration id access token stored in the user's shared preferences.
     */
    static String getCachedGCMRegistrationIdAccessToken() {
        return getString(CACHED_GCM_REGISTRATION_ID_PREF_ACCESS_TOKEN_NAME);
    }

    /**
     * Set the cached registration id access token as stored in the user's shared preferences.
     *
     * @param cachedGCMRegistrationIdAccessToken
     *            The cached registration id access token to be stored
     */
    static void setCachedGCMRegistrationIdAccessToken(String cachedGCMRegistrationIdAccessToken) {
        putString(CACHED_GCM_REGISTRATION_ID_PREF_ACCESS_TOKEN_NAME, cachedGCMRegistrationIdAccessToken);
    }

    /**
     * Get the application version stored in the user's shared preferences.
     */
    static int getGCMRegistrationAppVersion() {
        return getInt(GCM_REGISTRATION_APP_VERSION_PREF_NAME, Integer.MIN_VALUE);
    }

    /**
     * Set the application version stored in the user's shared preferences.
     *
     * @param appVersion
     *            The application version to be stored
     */
    static void setGCMRegistrationAppVersion(int appVersion) {
        putInt(GCM_REGISTRATION_APP_VERSION_PREF_NAME, appVersion);
    }

    /**
     * Get the registration sender ids stored in the user's shared preferences.
     */
    static String getGCMRegistrationSenderIds() {
        return getString(GCM_REGISTRATION_SENDER_IDS_PREF_NAME);
    }

    /**
     * Set the registration sender ids stored in the user's shared preferences.
     *
     * @param senderIds
     *            The registration sender ids to be stored
     */
    static void setGCMRegistrationSenderIds(String senderIds) {
        putString(GCM_REGISTRATION_SENDER_IDS_PREF_NAME, senderIds);
    }

    /**
     * Get the last received notification information stored in the user's shared preferences.
     */
    static JSONObject getLastReceivedNotificationInfoJson() {
        return getJSONObject(LAST_RECEIVED_NOTIFICATION_INFO_JSON_PREF_NAME);
    }

    /**
     * Set the last received notification information stored in the user's shared preferences.
     *
     * @param info
     *            The last received notification information to be stored
     */
    static void setLastReceivedNotificationInfoJson(JSONObject info) {
        putJSONObject(LAST_RECEIVED_NOTIFICATION_INFO_JSON_PREF_NAME, info);
    }

    /**
     * Get the last opened notification information stored in the user's shared preferences.
     */
    static JSONObject getLastOpenedNotificationInfoJson() {
        return getJSONObject(LAST_OPENED_NOTIFICATION_INFO_JSON_PREF_NAME);
    }

    /**
     * Set the last opened notification information stored in the user's shared preferences.
     *
     * @param info
     *            The last opened notification information to be stored
     */
    static void setLastOpenedNotificationInfoJson(JSONObject info) {
        putJSONObject(LAST_OPENED_NOTIFICATION_INFO_JSON_PREF_NAME, info);
    }

    /**
     * Get the last interaction date timestamp in milliseconds stored in the user's shared preferences.
     */
    static long getLastInteractionDate() {
        return getLong(LAST_INTERACTION_DATE_PREF_NAME, 0);
    }

    /**
     * Set the last interaction date timestamp in milliseconds stored in the user's shared preferences.
     * @param date
     *            The last interaction date to be stored
     */
    static void setLastInteractionDate(long date) {
        putLong(LAST_INTERACTION_DATE_PREF_NAME, date);
    }

    /**
     * Get the last app-open date timestamp in milliseconds stored in the user's shared preferences.
     */
    static long getLastAppOpenDate() {
        return getLong(LAST_APPOPEN_DATE_PREF_NAME, 0);
    }

    /**
     * Set the last app-open date timestamp in milliseconds stored in the user's shared preferences.
     * @param date
     *            The last app-open date to be stored
     */
    static void setLastAppOpenDate(long date) {
        putLong(LAST_APPOPEN_DATE_PREF_NAME, date);
    }

    /**
     * Get the last app-open information stored in the user's shared preferences.
     */
    static JSONObject getLastAppOpenInfoJson() {
        return getJSONObject(LAST_APPOPEN_INFO_PREF_NAME);
    }

    /**
     * Set the last app-open information in milliseconds stored in the user's shared preferences.
     * @param info
     *            The last app-open information to be stored
     */
    static void setLastAppOpenInfoJson(JSONObject info) {
        putJSONObject(LAST_APPOPEN_INFO_PREF_NAME, info);
    }

    /**
     * Get the last app-close date timestamp in milliseconds stored in the user's shared preferences.
     */
    static long getLastAppCloseDate() {
        return getLong(LAST_APPCLOSE_DATE_PREF_NAME, 0);
    }

    /**
     * Set the last app-close date timestamp in milliseconds stored in the user's shared preferences.
     * @param date
     *            The last app-close date to be stored
     */
    static void setLastAppCloseDate(long date) {
        putLong(LAST_APPCLOSE_DATE_PREF_NAME, date);
    }

    /**
     * Get the last known device date to WonderPush time offset in milliseconds stored in the user's shared preferences.
     */
    static long getDeviceDateSyncOffset() {
        return getLong(DEVICE_DATE_SYNC_OFFSET_PREF_NAME, 0);
    }

    /**
     * Set the last known device date to WonderPush time offset in milliseconds stored in the user's shared preferences.
     * @param offset
     *            The last known device date to WonderPush time offset to be stored
     */
    static void setDeviceDateSyncOffset(long offset) {
        putLong(DEVICE_DATE_SYNC_OFFSET_PREF_NAME, offset);
    }

    /**
     * Get the last known device date to WonderPush time uncertainty in milliseconds stored in the user's shared preferences.
     */
    static long getDeviceDateSyncUncertainty() {
        return getLong(DEVICE_DATE_SYNC_UNCERTAINTY_PREF_NAME, Long.MAX_VALUE);
    }

    /**
     * Set the last known device date to WonderPush time uncertainty in milliseconds stored in the user's shared preferences.
     * @param uncertainty
     *            The last known device date to WonderPush time uncertainty to be stored
     */
    static void setDeviceDateSyncUncertainty(long uncertainty) {
        putLong(DEVICE_DATE_SYNC_UNCERTAINTY_PREF_NAME, uncertainty);
    }

    /**
     * Retrieves the next notification id to use in NotificationManager for showing a tag-less notifications.
     */
    static int getNextTaglessNotificationManagerId() {
        int id = getInt(LAST_TAGLESS_NOTIFICATION_MANAGER_ID_PREF_NAME, 0);
        ++id;
        putInt(LAST_TAGLESS_NOTIFICATION_MANAGER_ID_PREF_NAME, id);
        return id;
    }

    /**
     * Retrieves whether to override logging.
     */
    static Boolean getOverrideSetLogging() {
        if (!has(OVERRIDE_SET_LOGGING_PREF_NAME)) return null;
        return getBoolean(OVERRIDE_SET_LOGGING_PREF_NAME, false);
    }

    /**
     * Sets whether to override logging.
     */
    static void setOverrideSetLogging(Boolean value) {
        if (value == null) {
            remove(OVERRIDE_SET_LOGGING_PREF_NAME);
        } else {
            putBoolean(OVERRIDE_SET_LOGGING_PREF_NAME, value);
        }
    }

    /**
     * Retrieves whether to override notification receipts.
     */
    static Boolean getOverrideNotificationReceipt() {
        if (!has(OVERRIDE_NOTIFICATION_RECEIPT_PREF_NAME)) return null;
        return getBoolean(OVERRIDE_NOTIFICATION_RECEIPT_PREF_NAME, false);
    }

    /**
     * Sets whether to override notification receipts.
     */
    static void setOverrideNotificationReceipt(Boolean value) {
        if (value == null) {
            remove(OVERRIDE_NOTIFICATION_RECEIPT_PREF_NAME);
        } else {
            putBoolean(OVERRIDE_NOTIFICATION_RECEIPT_PREF_NAME, value);
        }
    }

}
