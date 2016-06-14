package com.wonderpush.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

class WonderPushConfiguration {

    private static final String PREF_FILE = "wonderpush";

    private static final String PER_USER_ARCHIVE_PREF_NAME = "__per_user_archive";

    private static final String ACCESS_TOKEN_PREF_NAME = "__wonderpush_access_token";
    private static final String SID_PREF_NAME = "__wonderpush_sid";
    private static final String INSTALLATION_ID_PREF_NAME = "__installation_id";
    private static final String USER_ID_PREF_NAME = "__user_id";

    private static final String NOTIFICATION_ENABLED_PREF_NAME = "__wonderpush_notification_enabled";

    private static final String CACHED_INSTALLATION_CORE_PROPERTIES_NAME = "__cached_installation_core_properties";
    private static final String CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME = "__cached_installation_core_properties_date";
    private static final String CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME = "__cached_installation_core_properties_access_token";

    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME = "__cached_installation_custom_properties_written";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME = "__cached_installation_custom_properties_written_date";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME = "__cached_installation_custom_properties_updated";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME = "__cached_installation_custom_properties_updated_date";
    private static final String CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME = "__cached_installation_custom_properties_first_delayed_write_date";

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

    private static Context sContext;

    protected static void initialize(Context context) {
        sContext = context.getApplicationContext();
    }

    protected static boolean isInitialized() {
        return sContext != null;
    }

    protected static Context getApplicationContext() {
        return sContext;
    }

    protected static void changeUserId(String newUserId) {
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
            WonderPush.logError("Failed to save current user preferences", ex);
        }
        // Load new user preferences
        WonderPush.logDebug("loading storage for user " + newUserId);
        JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
        if (usersArchive == null) usersArchive = new JSONObject();
        JSONObject newUserArchive = usersArchive.optJSONObject(newUserId == null ? "" : newUserId);
        if (newUserArchive == null) newUserArchive = new JSONObject();
        setAccessToken(newUserArchive.optString(ACCESS_TOKEN_PREF_NAME, null));
        setSID(newUserArchive.optString(SID_PREF_NAME, null));
        setInstallationId(newUserArchive.optString(INSTALLATION_ID_PREF_NAME, null));
        setUserId(newUserId);
        setNotificationEnabled(newUserArchive.optBoolean(NOTIFICATION_ENABLED_PREF_NAME, true));
        setCachedInstallationCoreProperties(newUserArchive.optString(CACHED_INSTALLATION_CORE_PROPERTIES_NAME, null));
        setCachedInstallationCorePropertiesDate(newUserArchive.optLong(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME));
        setCachedInstallationCorePropertiesAccessToken(newUserArchive.optString(CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME, null));
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

    /**
     * Gets the WonderPush shared preferences for that application.
     */
    protected static SharedPreferences getSharedPreferences() {
        if (null == getApplicationContext())
            return null;
        return getApplicationContext().getSharedPreferences(PREF_FILE, 0);
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
    protected static String getUserId() {
        return getString(USER_ID_PREF_NAME);
    }

    /**
     * Set the user id stored in the user's shared preferences.
     *
     * @param userId
     *            The user id to be stored
     */
    protected static void setUserId(String userId) {
        putString(USER_ID_PREF_NAME, userId);
    }

    /**
     * Remove old access token, installation id and SID.
     * For instance when the user id changes, the device will get a new identity, so we must clear the old one.
     * When we face an invalid access token or SID error, we must also clear the stored information.
     */
    protected static void invalidateCredentials() {
        setAccessToken(null);
        setInstallationId(null);
        setSID(null);
    }

    /**
     * Get the access token associated to a given user's shared preferences.
     */
    protected static String getAccessTokenForUserId(String userId) {
        if (userId == null && getUserId() == null
                || userId != null && userId.equals(getUserId())) {
            return getAccessToken();
        } else {
            JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
            if (usersArchive == null) usersArchive = new JSONObject();
            JSONObject userArchive = usersArchive.optJSONObject(userId == null ? "" : userId);
            if (userArchive == null) userArchive = new JSONObject();
            return userArchive.optString(ACCESS_TOKEN_PREF_NAME, null);
        }
    }

    /**
     * Get the access token stored in the user's shared preferences.
     */
    protected static String getAccessToken() {
        return getString(ACCESS_TOKEN_PREF_NAME);
    }

    /**
     * Set the access token stored in the user's shared preferences.
     *
     * @param accessToken
     *            The access token to be stored
     */
    protected static void setAccessToken(String accessToken) {
        putString(ACCESS_TOKEN_PREF_NAME, accessToken);
    }

    /**
     * Get the installation id stored in the user's shared preferences.
     */
    protected static String getInstallationId() {
        return getString(INSTALLATION_ID_PREF_NAME);
    }

    /**
     * Set the installation id stored in the user's shared preferences.
     *
     * @param installationId
     *            The installation id to be stored
     */
    protected static void setInstallationId(String installationId) {
        putString(INSTALLATION_ID_PREF_NAME, installationId);
    }

    /**
     * Get the SID stored in the user's shared preferences.
     */
    protected static String getSID() {
        return getString(SID_PREF_NAME);
    }

    /**
     * Set the SID stored in the user's shared preferences.
     *
     * @param sid
     *            The SID to be stored
     */
    protected static void setSID(String sid) {
        putString(SID_PREF_NAME, sid);
    }

    /**
     * Get the notification enabled stored in the user's shared preferences.
     */
    protected static boolean getNotificationEnabled() {
        return getBoolean(NOTIFICATION_ENABLED_PREF_NAME, true);
    }

    /**
     * Set the notification enabled status stored in the user's shared preferences.
     *
     * @param status
     *            The notification enabled status to be stored
     */
    protected static void setNotificationEnabled(boolean status) {
        putBoolean(NOTIFICATION_ENABLED_PREF_NAME, status);
    }

    /**
     * Get the cached installation core properties stored in the user's shared preferences.
     */
    protected static String getCachedInstallationCoreProperties() {
        return getString(CACHED_INSTALLATION_CORE_PROPERTIES_NAME);
    }

    /**
     * Set the cached installation core properties stored in the user's shared preferences.
     *
     * @param cachedInstallationCoreProperties
     *            The cached installation core properties to be stored
     */
    protected static void setCachedInstallationCoreProperties(String cachedInstallationCoreProperties) {
        putString(CACHED_INSTALLATION_CORE_PROPERTIES_NAME, cachedInstallationCoreProperties);
    }

    /**
     * Get the cached installation core properties date stored in the user's shared preferences.
     */
    protected static long getCachedInstallationCorePropertiesDate() {
        return getLong(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME, 0);
    }

    /**
     * Set the cached installation core properties date stored in the user's shared preferences.
     *
     * @param cachedInstallationCorePropertiesDate
     *            The cached installation core properties date to be stored
     */
    protected static void setCachedInstallationCorePropertiesDate(long cachedInstallationCorePropertiesDate) {
        putLong(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME, cachedInstallationCorePropertiesDate);
    }

    /**
     * Get the cached installation core properties stored in the user's shared preferences.
     */
    protected static String getCachedInstallationCorePropertiesAccessToken() {
        return getString(CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME);
    }

    /**
     * Set the cached installation core properties access token stored in the user's shared preferences.
     *
     * @param cachedInstallationCorePropertiesAccessToken
     *            The cached installation core properties access token to be stored
     */
    protected static void setCachedInstallationCorePropertiesAccessToken(String cachedInstallationCorePropertiesAccessToken) {
        putString(CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME, cachedInstallationCorePropertiesAccessToken);
    }

    /**
     * Get the partial object representing the written installation code properties so far stored in the user's shared preferences.
     * This object is updated whenever grouped updates are performed.
     */
    protected static JSONObject getCachedInstallationCustomPropertiesWritten() {
        return getJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME);
    }

    /**
     * Set the partial object representing the written installation code properties so far stored in the user's shared preferences.
     *
     * @param cachedInstallationCustomPropertiesWritten
     *            The partial object representing the written installation code properties so far to be stored.
     */
    protected static void setCachedInstallationCustomPropertiesWritten(JSONObject cachedInstallationCustomPropertiesWritten) {
        putJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME, cachedInstallationCustomPropertiesWritten);
    }

    /**
     * Get the date of the last write to installation custom properties that has been performed.
     */
    protected static long getCachedInstallationCustomPropertiesWrittenDate() {
        return getLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME, 0);
    }

    /**
     * Set the date of the last write to installation custom properties that has been performed.
     * @param cachedInstallationCustomPropertiesWrittenDate
     *            The date of the last write to installation custom properties that has been performed to store.
     */
    protected static void setCachedInstallationCustomPropertiesWrittenDate(long cachedInstallationCustomPropertiesWrittenDate) {
        putLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME, cachedInstallationCustomPropertiesWrittenDate);
    }

    /**
     * Get the partial object representing the updated installation code properties stored in the user's shared preferences.
     * This object is updated whenever updates are demanded, whether delayed for grouping or already written.
     */
    protected static JSONObject getCachedInstallationCustomPropertiesUpdated() {
        return getJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME);
    }

    /**
     * Set the partial object representing the updated installation code properties stored in the user's shared preferences.
     *
     * @param cachedInstallationCustomPropertiesUpdated
     *            The partial object representing the updated installation code properties to be stored.
     */
    protected static void setCachedInstallationCustomPropertiesUpdated(JSONObject cachedInstallationCustomPropertiesUpdated) {
        putJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME, cachedInstallationCustomPropertiesUpdated);
    }

    /**
     * Get the date of the last update to {@link #getCachedInstallationCustomPropertiesUpdated()}.
     * This is the date of the last non-noop write to installation custom properties that has been delayed for grouping.
     */
    protected static long getCachedInstallationCustomPropertiesUpdatedDate() {
        return getLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME, 0);
    }

    /**
     * Set the date of the last update to {@link #getCachedInstallationCustomPropertiesUpdated()}.
     * This is the date of the last non-noop write to installation custom properties that has been delayed for grouping.
     * @param cachedInstallationCustomPropertiesUpdatedDate
     *            The date of the last update to {@link #getCachedInstallationCustomPropertiesUpdated()}.
     */
    protected static void setCachedInstallationCustomPropertiesUpdatedDate(long cachedInstallationCustomPropertiesUpdatedDate) {
        putLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME, cachedInstallationCustomPropertiesUpdatedDate);
    }

    /**
     * The date of the first non-noop write to installation custom properties that has been delayed for grouping.
     */
    protected static long getCachedInstallationCustomPropertiesFirstDelayedWrite() {
        return getLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME, 0);
    }

    /**
     * Set the date of the first non-noop write to installation custom properties that has been delayed for grouping.
     * @param cachedInstallationCustomPropertiesFirstDelayedWrite
     *            The date of the first non-noop write to installation custom properties that has been delayed for grouping to store.
     */
    protected static void setCachedInstallationCustomPropertiesFirstDelayedWrite(long cachedInstallationCustomPropertiesFirstDelayedWrite) {
        putLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME, cachedInstallationCustomPropertiesFirstDelayedWrite);
    }

    /**
     * Get the registration id stored in the user's shared preferences.
     */
    protected static String getGCMRegistrationId() {
        return getString(GCM_REGISTRATION_ID_PREF_NAME);
    }

    /**
     * Set the registration stored in the user's shared preferences.
     *
     * @param registrationId
     *            The registration id to be stored
     */
    protected static void setGCMRegistrationId(String registrationId) {
        putString(GCM_REGISTRATION_ID_PREF_NAME, registrationId);
    }

    /**
     * Get the cached registration id date stored in the user's shared preferences.
     */
    protected static long getCachedGCMRegistrationIdDate() {
        return getLong(CACHED_GCM_REGISTRATION_ID_PREF_DATE_NAME, 0);
    }

    /**
     * Set the cached registration id date as stored in the user's shared preferences.
     *
     * @param cachedGCMRegistrationIdDate
     *            The cached registration id date to be stored
     */
    protected static void setCachedGCMRegistrationIdDate(long cachedGCMRegistrationIdDate) {
        putLong(CACHED_GCM_REGISTRATION_ID_PREF_DATE_NAME, cachedGCMRegistrationIdDate);
    }

    /**
     * Get the user id the registration id was associated with in the user's shared preferences.
     */
    protected static String getCachedGCMRegistrationIdAssociatedUserId() {
        return getString(CACHED_GCM_REGISTRATION_ID_ASSOCIATED_TO_USER_ID_PREF_NAME);
    }

    /**
     * Set the user id the registration was associated with in the user's shared preferences.
     *
     * @param userId
     *            The associated user id to be stored
     */
    protected static void setCachedGCMRegistrationIdAssociatedUserId(String userId) {
        putString(CACHED_GCM_REGISTRATION_ID_ASSOCIATED_TO_USER_ID_PREF_NAME, userId);
    }

    /**
     * Get the cached registration id access token stored in the user's shared preferences.
     */
    protected static String getCachedGCMRegistrationIdAccessToken() {
        return getString(CACHED_GCM_REGISTRATION_ID_PREF_ACCESS_TOKEN_NAME);
    }

    /**
     * Set the cached registration id access token as stored in the user's shared preferences.
     *
     * @param cachedGCMRegistrationIdAccessToken
     *            The cached registration id access token to be stored
     */
    protected static void setCachedGCMRegistrationIdAccessToken(String cachedGCMRegistrationIdAccessToken) {
        putString(CACHED_GCM_REGISTRATION_ID_PREF_ACCESS_TOKEN_NAME, cachedGCMRegistrationIdAccessToken);
    }

    /**
     * Get the application version stored in the user's shared preferences.
     */
    protected static int getGCMRegistrationAppVersion() {
        return getInt(GCM_REGISTRATION_APP_VERSION_PREF_NAME, Integer.MIN_VALUE);
    }

    /**
     * Set the application version stored in the user's shared preferences.
     *
     * @param appVersion
     *            The application version to be stored
     */
    protected static void setGCMRegistrationAppVersion(int appVersion) {
        putInt(GCM_REGISTRATION_APP_VERSION_PREF_NAME, appVersion);
    }

    /**
     * Get the registration sender ids stored in the user's shared preferences.
     */
    protected static String getGCMRegistrationSenderIds() {
        return getString(GCM_REGISTRATION_SENDER_IDS_PREF_NAME);
    }

    /**
     * Set the registration sender ids stored in the user's shared preferences.
     *
     * @param senderIds
     *            The registration sender ids to be stored
     */
    protected static void setGCMRegistrationSenderIds(String senderIds) {
        putString(GCM_REGISTRATION_SENDER_IDS_PREF_NAME, senderIds);
    }

    /**
     * Get the last received notification information stored in the user's shared preferences.
     */
    protected static JSONObject getLastReceivedNotificationInfoJson() {
        return getJSONObject(LAST_RECEIVED_NOTIFICATION_INFO_JSON_PREF_NAME);
    }

    /**
     * Set the last received notification information stored in the user's shared preferences.
     *
     * @param info
     *            The last received notification information to be stored
     */
    protected static void setLastReceivedNotificationInfoJson(JSONObject info) {
        putJSONObject(LAST_RECEIVED_NOTIFICATION_INFO_JSON_PREF_NAME, info);
    }

    /**
     * Get the last opened notification information stored in the user's shared preferences.
     */
    protected static JSONObject getLastOpenedNotificationInfoJson() {
        return getJSONObject(LAST_OPENED_NOTIFICATION_INFO_JSON_PREF_NAME);
    }

    /**
     * Set the last opened notification information stored in the user's shared preferences.
     *
     * @param info
     *            The last opened notification information to be stored
     */
    protected static void setLastOpenedNotificationInfoJson(JSONObject info) {
        putJSONObject(LAST_OPENED_NOTIFICATION_INFO_JSON_PREF_NAME, info);
    }

    /**
     * Get the last interaction date timestamp in milliseconds stored in the user's shared preferences.
     */
    protected static long getLastInteractionDate() {
        return getLong(LAST_INTERACTION_DATE_PREF_NAME, 0);
    }

    /**
     * Set the last interaction date timestamp in milliseconds stored in the user's shared preferences.
     * @param date
     *            The last interaction date to be stored
     */
    protected static void setLastInteractionDate(long date) {
        putLong(LAST_INTERACTION_DATE_PREF_NAME, date);
    }

    /**
     * Get the last app-open date timestamp in milliseconds stored in the user's shared preferences.
     */
    protected static long getLastAppOpenDate() {
        return getLong(LAST_APPOPEN_DATE_PREF_NAME, 0);
    }

    /**
     * Set the last app-open date timestamp in milliseconds stored in the user's shared preferences.
     * @param date
     *            The last app-open date to be stored
     */
    protected static void setLastAppOpenDate(long date) {
        putLong(LAST_APPOPEN_DATE_PREF_NAME, date);
    }

    /**
     * Get the last app-open information stored in the user's shared preferences.
     */
    protected static JSONObject getLastAppOpenInfoJson() {
        return getJSONObject(LAST_APPOPEN_INFO_PREF_NAME);
    }

    /**
     * Set the last app-open information in milliseconds stored in the user's shared preferences.
     * @param info
     *            The last app-open information to be stored
     */
    protected static void setLastAppOpenInfoJson(JSONObject info) {
        putJSONObject(LAST_APPOPEN_INFO_PREF_NAME, info);
    }

    /**
     * Get the last app-close date timestamp in milliseconds stored in the user's shared preferences.
     */
    protected static long getLastAppCloseDate() {
        return getLong(LAST_APPCLOSE_DATE_PREF_NAME, 0);
    }

    /**
     * Set the last app-close date timestamp in milliseconds stored in the user's shared preferences.
     * @param date
     *            The last app-close date to be stored
     */
    protected static void setLastAppCloseDate(long date) {
        putLong(LAST_APPCLOSE_DATE_PREF_NAME, date);
    }

    /**
     * Get the last known device date to WonderPush time offset in milliseconds stored in the user's shared preferences.
     */
    protected static long getDeviceDateSyncOffset() {
        return getLong(DEVICE_DATE_SYNC_OFFSET_PREF_NAME, 0);
    }

    /**
     * Set the last known device date to WonderPush time offset in milliseconds stored in the user's shared preferences.
     * @param offset
     *            The last known device date to WonderPush time offset to be stored
     */
    protected static void setDeviceDateSyncOffset(long offset) {
        putLong(DEVICE_DATE_SYNC_OFFSET_PREF_NAME, offset);
    }

    /**
     * Get the last known device date to WonderPush time uncertainty in milliseconds stored in the user's shared preferences.
     */
    protected static long getDeviceDateSyncUncertainty() {
        return getLong(DEVICE_DATE_SYNC_UNCERTAINTY_PREF_NAME, Long.MAX_VALUE);
    }

    /**
     * Set the last known device date to WonderPush time uncertainty in milliseconds stored in the user's shared preferences.
     * @param uncertainty
     *            The last known device date to WonderPush time uncertainty to be stored
     */
    protected static void setDeviceDateSyncUncertainty(long uncertainty) {
        putLong(DEVICE_DATE_SYNC_UNCERTAINTY_PREF_NAME, uncertainty);
    }

    /**
     * Retrieves the next notification id to use in NotificationManager for showing a tag-less notifications.
     */
    protected static int getNextTaglessNotificationManagerId() {
        int id = getInt(LAST_TAGLESS_NOTIFICATION_MANAGER_ID_PREF_NAME, 0);
        ++id;
        putInt(LAST_TAGLESS_NOTIFICATION_MANAGER_ID_PREF_NAME, id);
        return id;
    }
}
