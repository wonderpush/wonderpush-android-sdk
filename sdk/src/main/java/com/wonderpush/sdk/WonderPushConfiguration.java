package com.wonderpush.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

public class WonderPushConfiguration {

    private static final String PREF_FILE = "wonderpush";

    private static final String PER_USER_ARCHIVE_PREF_NAME = "__per_user_archive";

    private static final String ACCESS_TOKEN_PREF_NAME = "__wonderpush_access_token";
    private static final String SID_PREF_NAME = "__wonderpush_sid";
    private static final String DEVICE_ID_PREF_NAME = "__device_id";
    private static final String INSTALLATION_ID_PREF_NAME = "__installation_id";
    private static final String USER_ID_PREF_NAME = "__user_id";
    private static final String USER_CONSENT_PREF_NAME = "__user_consent";

    private static final String STORED_TRACKED_EVENTS_PREF_NAME = "__wonderpush_stored_tracked_events";

    private static final String NOTIFICATION_ENABLED_PREF_NAME = "__wonderpush_notification_enabled";
    private static final String CACHED_OS_ARENOTIFICATIONSENABLED_NAME = "__cached_os_areNotificationsEnabled";
    private static final String CACHED_OS_ARENOTIFICATIONSENABLED_DATE_NAME = "__cached_os_areNotificationsEnabled_date";
    private static final String CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_NAME = "__cached_disabled_notification_channel_ids";
    private static final String CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_DATE_NAME = "__cached_disabled_notification_channel_ids_date";
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
    private static final String GCM_REGISTRATION_SENDER_IDS_PREF_NAME = "__wonderpush_gcm_registration_sender_ids";
    private static final String GCM_REGISTRATION_SERVICE_PREF_NAME = "__wonderpush_gcm_registration_service";

    private static final String LAST_RECEIVED_NOTIFICATION_INFO_JSON_PREF_NAME = "__last_received_notification_info_json";
    private static final String LAST_OPENED_NOTIFICATION_INFO_JSON_PREF_NAME = "__last_opened_notification_info_json";

    private static final String LAST_INTERACTION_DATE_PREF_NAME = "__last_interaction_date";
    private static final String LAST_APPOPEN_DATE_PREF_NAME = "__last_appopen_date";
    private static final String LAST_APPOPEN_INFO_PREF_NAME = "__last_appopen_info_json";
    private static final String LAST_APP_OPEN_SENT_DATE_PREF_NAME = "__last_app_open_sent_date";

    private static final String COUNTRY_PREF_NAME = "__country";
    private static final String CURRENCY_PREF_NAME = "__currency";
    private static final String LOCALE_PREF_NAME = "__locale";
    private static final String TIME_ZONE_PREF_NAME = "__time_zone";

    private static final String DEVICE_DATE_SYNC_OFFSET_PREF_NAME = "__device_date_sync_offset";
    private static final String DEVICE_DATE_SYNC_UNCERTAINTY_PREF_NAME = "__device_date_sync_uncertainty";

    private static final String LAST_TAGLESS_NOTIFICATION_MANAGER_ID_PREF_NAME = "__last_tagless_notification_manager_id";

    private static final String OVERRIDE_SET_LOGGING_PREF_NAME = "__override_set_logging";
    private static final String OVERRIDE_NOTIFICATION_RECEIPT_PREF_NAME = "__override_notification_receipt";

    private static Context sContext;

    public static final int DEFAULT_MAXIMUM_COLLAPSED_LAST_BUILTIN_TRACKED_EVENTS_COUNT = 100;
    public static final int DEFAULT_MAXIMUM_COLLAPSED_LAST_CUSTOM_TRACKED_EVENTS_COUNT = 1000;
    public static final int DEFAULT_MAXIMUM_COLLAPSED_OTHER_TRACKED_EVENTS_COUNT = 1000;
    public static final int DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_COUNT = 10000;
    public static final long DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_AGE_MS = 90L * 24 * 60 * 60 * 1000;
    private static int maximumCollapsedLastBuiltinTrackedEventsCount = DEFAULT_MAXIMUM_COLLAPSED_LAST_BUILTIN_TRACKED_EVENTS_COUNT;
    private static int maximumCollapsedLastCustomTrackedEventsCount = DEFAULT_MAXIMUM_COLLAPSED_LAST_CUSTOM_TRACKED_EVENTS_COUNT;
    private static int maximumCollapsedOtherTrackedEventsCount = DEFAULT_MAXIMUM_COLLAPSED_OTHER_TRACKED_EVENTS_COUNT;
    private static int maximumUncollapsedTrackedEventsCount = DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_COUNT;
    private static long maximumUncollapsedTrackedEventsAgeMs = DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_AGE_MS;
    private static WeakReference<List<JSONObject>> cachedTrackedEvents = new WeakReference<>(null);

    public static void initialize(Context context) {
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
            currentUserArchive.putOpt(CACHED_OS_ARENOTIFICATIONSENABLED_NAME, getCachedOsAreNotificationsEnabled());
            currentUserArchive.putOpt(CACHED_OS_ARENOTIFICATIONSENABLED_DATE_NAME, getCachedOsAreNotificationsEnabledDate());
            currentUserArchive.putOpt(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_NAME, new JSONArray(getCachedDisabledNotificationChannelIds()));
            currentUserArchive.putOpt(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_DATE_NAME, getCachedDisabledNotificationChannelIdsDate());
            currentUserArchive.putOpt(CHANNEL_PREFERENCES_PREF_NAME, getChannelPreferences());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME, getCachedInstallationCustomPropertiesWritten());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME, getCachedInstallationCustomPropertiesWrittenDate());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME, getCachedInstallationCustomPropertiesUpdated());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME, getCachedInstallationCustomPropertiesUpdatedDate());
            currentUserArchive.putOpt(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME, getCachedInstallationCustomPropertiesFirstDelayedWrite());
            currentUserArchive.putOpt(LAST_INTERACTION_DATE_PREF_NAME, getLastInteractionDate());
            currentUserArchive.putOpt(LAST_APPOPEN_DATE_PREF_NAME, getLastAppOpenDate());
            currentUserArchive.putOpt(LAST_APPOPEN_INFO_PREF_NAME, getLastAppOpenInfoJson());
            currentUserArchive.putOpt(LAST_APP_OPEN_SENT_DATE_PREF_NAME, getLastAppOpenSentDate());
            currentUserArchive.putOpt(COUNTRY_PREF_NAME, getCountry());
            currentUserArchive.putOpt(CURRENCY_PREF_NAME, getCurrency());
            currentUserArchive.putOpt(LOCALE_PREF_NAME, getLocale());
            currentUserArchive.putOpt(TIME_ZONE_PREF_NAME, getTimeZone());
            currentUserArchive.putOpt(STORED_TRACKED_EVENTS_PREF_NAME, getTrackedEvents());
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
        setCachedOsAreNotificationsEnabled(newUserArchive.optBoolean(CACHED_OS_ARENOTIFICATIONSENABLED_NAME, true));
        setCachedOsAreNotificationsEnabledDate(newUserArchive.optLong(CACHED_OS_ARENOTIFICATIONSENABLED_DATE_NAME));
        setCachedDisabledNotificationChannelIds(JSONArrayToSetString(newUserArchive.optJSONArray(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_NAME)));
        setCachedDisabledNotificationChannelIdsDate(newUserArchive.optLong(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_DATE_NAME));
        setChannelPreferences(newUserArchive.optJSONObject(CHANNEL_PREFERENCES_PREF_NAME));
        setCachedInstallationCustomPropertiesWritten(newUserArchive.optJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME));
        setCachedInstallationCustomPropertiesWrittenDate(newUserArchive.optLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME));
        setCachedInstallationCustomPropertiesUpdated(newUserArchive.optJSONObject(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME));
        setCachedInstallationCustomPropertiesUpdatedDate(newUserArchive.optLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME));
        setCachedInstallationCustomPropertiesFirstDelayedWrite(newUserArchive.optLong(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME));
        setLastInteractionDate(newUserArchive.optLong(LAST_INTERACTION_DATE_PREF_NAME));
        setLastAppOpenDate(newUserArchive.optLong(LAST_APPOPEN_DATE_PREF_NAME));
        setLastAppOpenInfoJson(newUserArchive.optJSONObject(LAST_APPOPEN_INFO_PREF_NAME));
        setLastAppOpenSentDate(newUserArchive.optLong(LAST_APP_OPEN_SENT_DATE_PREF_NAME));
        setCountry(JSONUtil.optString(newUserArchive, COUNTRY_PREF_NAME));
        setCurrency(JSONUtil.optString(newUserArchive, CURRENCY_PREF_NAME));
        setLocale(JSONUtil.optString(newUserArchive, LOCALE_PREF_NAME));
        setTimeZone(JSONUtil.optString(newUserArchive, TIME_ZONE_PREF_NAME));
        try {
            String trackedEventsJson = JSONUtil.optString(newUserArchive, STORED_TRACKED_EVENTS_PREF_NAME);
            JSONArray trackedEvents = null;
            if (trackedEventsJson != null) {
                trackedEvents = new JSONArray(trackedEventsJson);
            }
            setTrackedEvents(getTrackedEventsFromStoredJSONArray(trackedEvents));
        } catch (JSONException e) {
            setTrackedEvents(null);
        }
    }

    static void clearForUserId(String userId) {
        if (userId == null) userId = "";
        // Clean user archive
        JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
        if (usersArchive != null) {
            usersArchive.remove(userId);
            putJSONObject(PER_USER_ARCHIVE_PREF_NAME, usersArchive);
        }
        // Note: We do not touch INSTALLATION_CUSTOM_SYNC_STATE_PER_USER_ID_PREF_NAME ourself
        // If we're working on the current user, clear the properties
        if (userId.equals(getUserId()) || getUserId() == null && userId.equals("")) {
            SharedPreferences prefs = getSharedPreferences();
            if (prefs != null) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(ACCESS_TOKEN_PREF_NAME);
                editor.remove(SID_PREF_NAME);
                editor.remove(INSTALLATION_ID_PREF_NAME);
                editor.remove(USER_ID_PREF_NAME);
                editor.remove(NOTIFICATION_ENABLED_PREF_NAME);
                editor.remove(CACHED_OS_ARENOTIFICATIONSENABLED_NAME);
                editor.remove(CACHED_OS_ARENOTIFICATIONSENABLED_DATE_NAME);
                editor.remove(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_NAME);
                editor.remove(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_DATE_NAME);
                editor.remove(CHANNEL_PREFERENCES_PREF_NAME);
                editor.remove(CACHED_INSTALLATION_CORE_PROPERTIES_NAME);
                editor.remove(CACHED_INSTALLATION_CORE_PROPERTIES_DATE_NAME);
                editor.remove(CACHED_INSTALLATION_CORE_PROPERTIES_ACCESS_TOKEN_NAME);
                editor.remove(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_PREF_NAME);
                editor.remove(CACHED_INSTALLATION_CUSTOM_PROPERTIES_WRITTEN_DATE_PREF_NAME);
                editor.remove(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_PREF_NAME);
                editor.remove(CACHED_INSTALLATION_CUSTOM_PROPERTIES_UPDATED_DATE_PREF_NAME);
                editor.remove(CACHED_INSTALLATION_CUSTOM_PROPERTIES_FIRST_DELAYED_WRITE_DATE_PREF_NAME);
                editor.remove(LAST_INTERACTION_DATE_PREF_NAME);
                editor.remove(LAST_APPOPEN_DATE_PREF_NAME);
                editor.remove(LAST_APPOPEN_INFO_PREF_NAME);
                editor.remove(LAST_APP_OPEN_SENT_DATE_PREF_NAME);
                editor.remove(STORED_TRACKED_EVENTS_PREF_NAME);
                editor.apply();
            }
        }
    }

    static void clearStorage(boolean keepUserConsent, boolean keepDeviceId) {
        SharedPreferences prefs = getSharedPreferences();
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (keepUserConsent && USER_CONSENT_PREF_NAME.equals(key)) continue;
            if (keepDeviceId && DEVICE_ID_PREF_NAME.equals(key)) continue;
            editor.remove(key);
        }
        editor.apply();
    }

    static JSONObject dumpState() {
        JSONObject rtn = new JSONObject();
        SharedPreferences prefs = getSharedPreferences();
        if (prefs == null) return rtn;
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

    private static JSONArray getJSONArray(String key) {
        String json = getString(key);
        if (json != null) {
            try {
                return new JSONArray(json);
            } catch (JSONException e) {
                Log.w(WonderPush.TAG, "Failed to decode json from preferences", e);
            }
        }
        return null;
    }

    private static void putJSONArray(String key, JSONArray value) {
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

    private static Set<String> JSONArrayToSetString(JSONArray values) {
        TreeSet<String> rtn = new TreeSet<>();
        if (values != null) {
            for (int i = 0, e = values.length(); i < e; ++i) {
                try {
                    Object value = values.get(i);
                    if (value instanceof String) {
                        rtn.add((String) value);
                    }
                } catch (JSONException ex) {
                    Log.e(WonderPush.TAG, "Unexpected error while reading cached disabled notification channels", ex);
                }
            }
        }
        return rtn;
    }

    /**
     * Get the user consent stored in the user's shared preferences.
     */
    static boolean getUserConsent() {
        return getBoolean(USER_CONSENT_PREF_NAME, false);
    }

    /**
     * Set the user consent stored in the user's shared preferences.
     *
     * @param userConsent
     *            The user consent to be stored
     */
    static void setUserConsent(boolean userConsent) {
        putBoolean(USER_CONSENT_PREF_NAME, userConsent);
    }

    /**
     * Get the user id stored in the user's shared preferences.
     */
    public static String getUserId() {
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
    static void invalidateCredentials(String userId) {
        if (userId == null && getUserId() == null
                || userId != null && userId.equals(getUserId())) {
            setAccessToken(null);
            setInstallationId(null);
            setSID(null);
        } else {
            JSONObject usersArchive = getJSONObject(PER_USER_ARCHIVE_PREF_NAME);
            if (usersArchive == null) usersArchive = new JSONObject();
            JSONObject userArchive = usersArchive.optJSONObject(userId == null ? "" : userId);
            if (userArchive == null) userArchive = new JSONObject();
            userArchive.remove(ACCESS_TOKEN_PREF_NAME);
            userArchive.remove(INSTALLATION_ID_PREF_NAME);
            userArchive.remove(SID_PREF_NAME);
            try {
                usersArchive.put(userId == null ? "" : userId, userArchive);
                putJSONObject(PER_USER_ARCHIVE_PREF_NAME, usersArchive);
            } catch (JSONException e) {
                WonderPush.logError("Failed to invalidateCredentials for " + userId, e);
            }
        }
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
    public static String getAccessToken() {
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
     * Get the device id stored in the user's shared preferences.
     */
    static String getDeviceId() {
        return getString(DEVICE_ID_PREF_NAME);
    }

    /**
     * Set the device id stored in the user's shared preferences.
     *
     * @param deviceId
     *            The device id to be stored
     */
    static void setDeviceId(String deviceId) {
        WonderPush.logDebug("Setting deviceId = " + deviceId);
        putString(DEVICE_ID_PREF_NAME, deviceId);
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
     * Get the cached state of NotificationManager.areNotificationsEnabled().
     */
    static boolean getCachedOsAreNotificationsEnabled() {
        return getBoolean(CACHED_OS_ARENOTIFICATIONSENABLED_NAME, true);
    }

    /**
     * Set the cached state of NotificationManager.areNotificationsEnabled().
     *
     * @param status
     *            The cached state of NotificationManager.areNotificationsEnabled() to be stored.
     */
    static void setCachedOsAreNotificationsEnabled(boolean status) {
        putBoolean(CACHED_OS_ARENOTIFICATIONSENABLED_NAME, status);
    }

    /**
     * Get the date of the last update to {@link #getCachedOsAreNotificationsEnabled()}.
     */
    static long getCachedOsAreNotificationsEnabledDate() {
        return getLong(CACHED_OS_ARENOTIFICATIONSENABLED_DATE_NAME, 0);
    }

    /**
     * Set the date of the last update to {@link #getCachedOsAreNotificationsEnabled()}.
     * @param cachedOsAreNotificationsEnabledDate
     *            The date of the last update to {@link #getCachedOsAreNotificationsEnabled()}.
     */
    static void setCachedOsAreNotificationsEnabledDate(long cachedOsAreNotificationsEnabledDate) {
        putLong(CACHED_OS_ARENOTIFICATIONSENABLED_DATE_NAME, cachedOsAreNotificationsEnabledDate);
    }

    /**
     * Get the cached list of disabled notification channels.
     */
    static Set<String> getCachedDisabledNotificationChannelIds() {
        return JSONArrayToSetString(getJSONArray(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_NAME));
    }

    /**
     * Set the cached list of disabled notification channels.
     *
     * @param disabledNotificationsChannels
     *            The cached list of disabled notification channels to be stored.
     */
    static void setCachedDisabledNotificationChannelIds(Set<String> disabledNotificationsChannels) {
        putJSONArray(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_NAME, new JSONArray(disabledNotificationsChannels));
    }

    /**
     * Get the date of the last update to {@link #getCachedDisabledNotificationChannelIds()}.
     */
    static long getCachedDisabledNotificationChannelIdsDate() {
        return getLong(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_DATE_NAME, 0);
    }

    /**
     * Set the date of the last update to {@link #getCachedDisabledNotificationChannelIds()}.
     * @param cachedDisabledNotificationsChannelsDate
     *            The date of the last update to {@link #getCachedDisabledNotificationChannelIds()}.
     */
    static void setCachedDisabledNotificationChannelIdsDate(long cachedDisabledNotificationsChannelsDate) {
        putLong(CACHED_DISABLED_NOTIFICATION_CHANNEL_IDS_DATE_NAME, cachedDisabledNotificationsChannelsDate);
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
    public static String getGCMRegistrationId() {
        return getString(GCM_REGISTRATION_ID_PREF_NAME);
    }

    /**
     * Set the registration stored in the user's shared preferences.
     *
     * @param registrationId
     *            The registration id to be stored
     */
    public static void setGCMRegistrationId(String registrationId) {
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
    public static void setCachedGCMRegistrationIdDate(long cachedGCMRegistrationIdDate) {
        putLong(CACHED_GCM_REGISTRATION_ID_PREF_DATE_NAME, cachedGCMRegistrationIdDate);
    }

    /**
     * Get the user id the registration id was associated with in the user's shared preferences.
     */
    public static String getCachedGCMRegistrationIdAssociatedUserId() {
        return getString(CACHED_GCM_REGISTRATION_ID_ASSOCIATED_TO_USER_ID_PREF_NAME);
    }

    /**
     * Set the user id the registration was associated with in the user's shared preferences.
     *
     * @param userId
     *            The associated user id to be stored
     */
    public static void setCachedGCMRegistrationIdAssociatedUserId(String userId) {
        putString(CACHED_GCM_REGISTRATION_ID_ASSOCIATED_TO_USER_ID_PREF_NAME, userId);
    }

    /**
     * Get the cached registration id access token stored in the user's shared preferences.
     */
    public static String getCachedGCMRegistrationIdAccessToken() {
        return getString(CACHED_GCM_REGISTRATION_ID_PREF_ACCESS_TOKEN_NAME);
    }

    /**
     * Set the cached registration id access token as stored in the user's shared preferences.
     *
     * @param cachedGCMRegistrationIdAccessToken
     *            The cached registration id access token to be stored
     */
    public static void setCachedGCMRegistrationIdAccessToken(String cachedGCMRegistrationIdAccessToken) {
        putString(CACHED_GCM_REGISTRATION_ID_PREF_ACCESS_TOKEN_NAME, cachedGCMRegistrationIdAccessToken);
    }

    /**
     * Get the registration sender ids stored in the user's shared preferences.
     */
    public static String getGCMRegistrationSenderIds() {
        return getString(GCM_REGISTRATION_SENDER_IDS_PREF_NAME);
    }

    /**
     * Set the registration sender ids stored in the user's shared preferences.
     *
     * @param senderIds
     *            The registration sender ids to be stored
     */
    public static void setGCMRegistrationSenderIds(String senderIds) {
        putString(GCM_REGISTRATION_SENDER_IDS_PREF_NAME, senderIds);
    }

    /**
     * Get the push service stored in the user's shared preferences.
     */
    static String getGCMRegistrationService() {
        return getString(GCM_REGISTRATION_SERVICE_PREF_NAME);
    }

    /**
     * Set the push service stored in the user's shared preferences.
     *
     * @param service
     *            The push servuce to be stored
     */
    static void setGCMRegistrationService(String service) {
        putString(GCM_REGISTRATION_SERVICE_PREF_NAME, service);
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
    public static long getLastAppOpenDate() {
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
     * Get the last time we queued an @APP_OPEN event to the request vault.
     */
    static long getLastAppOpenSentDate() {
        return getLong(LAST_APP_OPEN_SENT_DATE_PREF_NAME, 0);
    }

    /**
     * Set the last time we queued an @APP_OPEN event to the request vault.
     * @param date
     *            The date
     */
    static void setLastAppOpenSentDate(long date) {
        putLong(LAST_APP_OPEN_SENT_DATE_PREF_NAME, date);
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

    static String getCountry() {
        return getString(COUNTRY_PREF_NAME);
    }

    static void setCountry(String value) {
        putString(COUNTRY_PREF_NAME, value);
    }

    static String getCurrency() {
        return getString(CURRENCY_PREF_NAME);
    }

    static void setCurrency(String value) {
        putString(CURRENCY_PREF_NAME, value);
    }

    static String getLocale() {
        return getString(LOCALE_PREF_NAME);
    }

    static void setLocale(String value) {
        putString(LOCALE_PREF_NAME, value);
    }

    static String getTimeZone() {
        return getString(TIME_ZONE_PREF_NAME);
    }

    static void setTimeZone(String value) {
        putString(TIME_ZONE_PREF_NAME, value);
    }

    static @Nullable Occurrences rememberTrackedEvent(JSONObject eventData) {
        // Note: It is assumed that the given event is more recent than any other already stored events

        String type = JSONUtil.getString(eventData, "type");
        if (type == null) return null;
        Occurrences occurrences = new Occurrences();
        long allTime = 0L;

        String campaignId = JSONUtil.optString(eventData, "campaignId");
        String collapsing = JSONUtil.optString(eventData, "collapsing");

        List<JSONObject> oldTrackedEvents = getTrackedEvents();
        List<JSONObject> uncollapsedEvents = new ArrayList<>(oldTrackedEvents.size() + 1); // collapsing == null
        List<JSONObject> collapsedLastBuiltinEvents = new ArrayList<>(); // collapsing.equals("last") && type.startsWith("@")
        List<JSONObject> collapsedLastCustomEvents = new ArrayList<>(); // collapsing.equals("last") && !type.startsWith("@")
        List<JSONObject> collapsedOtherEvents = new ArrayList<>(); // collapsing != null && !collapsing.equals("last") // ie. collapsing.equals("campaign"), as of this writing

        long now = TimeSync.getTime();
        long getMaximumUncollapsedTrackedEventsAgeMs = getMaximumUncollapsedTrackedEventsAgeMs();
        for (JSONObject oldTrackedEvent : oldTrackedEvents) {
            String oldTrackedEventCollapsing = JSONUtil.optString(oldTrackedEvent, "collapsing");
            String oldTrackedEventType = oldTrackedEvent.optString("type");
            // Filter out the collapsing=last event of the same type as the new event we want to add
            if ((collapsing == null || "last".equals(collapsing)) && "last".equals(oldTrackedEventCollapsing) && type.equals(oldTrackedEventType)) {
                JSONObject occs = oldTrackedEvent.optJSONObject("occurrences");
                long occsAllTime = occs != null ? occs.optLong("allTime", 1L) : 1;
                allTime = Math.max(1, occsAllTime);
                continue;
            }
            // Filter out the collapsing=campaign event of the same type and campaign as the new event we want to add
            if (campaignId != null && "campaign".equals(collapsing) && "campaign".equals(oldTrackedEventCollapsing) && type.equals(oldTrackedEventType) && campaignId.equals(oldTrackedEvent.optString("campaignId"))) {
                JSONObject occs = oldTrackedEvent.optJSONObject("occurrences");
                long occsAllTime = occs != null ? occs.optLong("allTime", 1L) : 1;
                allTime = Math.max(1, occsAllTime);
                continue;
            }
            // Filter out old uncollapsed events
            if (oldTrackedEventCollapsing == null && now - oldTrackedEvent.optLong("actionDate", now) >= getMaximumUncollapsedTrackedEventsAgeMs) {
                continue;
            }
            // TODO We may want to filter out old collapsing=campaign (or any non-null value other than "last") events too
            // Store the event in the proper, per-collapsing list
            if (oldTrackedEventCollapsing == null) {
                uncollapsedEvents.add(oldTrackedEvent);
            } else if ("last".equals(oldTrackedEventCollapsing)) {
                if (oldTrackedEventType.startsWith("@")) {
                    collapsedLastBuiltinEvents.add(oldTrackedEvent);
                } else {
                    collapsedLastCustomEvents.add(oldTrackedEvent);
                }
            } else {
                collapsedOtherEvents.add(oldTrackedEvent);
            }
        }

        // Add the new event, uncollapsed
        JSONObject collapsedEventData = null;
        JSONObject uncollapsedEventData = null;
        if (collapsing == null) {
            try {
                uncollapsedEventData = new JSONObject(eventData.toString());
                uncollapsedEvents.add(uncollapsedEventData);
            } catch (JSONException e) {
                Log.e(WonderPush.TAG, "Could not store uncollapsed tracked event", e);
            }
        }

        // Add the new event with collapsing
        // We default to collapsing=last, but we otherwise keep any existing collapsing
        try {
            allTime += 1;
            collapsedEventData = new JSONObject(eventData.toString());
            if (collapsing == null) {
                collapsedEventData.put("collapsing", "last");
                collapsing = "last";
            }
            if ("last".equals(collapsing)) {
                if (type.startsWith("@")) {
                    collapsedLastBuiltinEvents.add(collapsedEventData);
                } else {
                    collapsedLastCustomEvents.add(collapsedEventData);
                }
            } else {
                collapsedOtherEvents.add(collapsedEventData);
            }
        } catch (JSONException e) {
            Log.e(WonderPush.TAG, "Could not store collapsed tracked event", e);
        }

        // Sort events by date
        Comparator<? super JSONObject> eventActionDateComparator = (o1, o2) -> {
            long delta = o1.optLong("actionDate", -1) - o2.optLong("actionDate", -1);
            if (delta < 0) return -1;
            if (delta > 0) return 1;
            return 0;
        };
        WonderPushCompatibilityHelper.sort(uncollapsedEvents, eventActionDateComparator);
        WonderPushCompatibilityHelper.sort(collapsedLastBuiltinEvents, eventActionDateComparator);
        WonderPushCompatibilityHelper.sort(collapsedLastCustomEvents, eventActionDateComparator);
        WonderPushCompatibilityHelper.sort(collapsedOtherEvents, eventActionDateComparator);

        // Impose a limit on the maximum number of tracked events
        uncollapsedEvents = removeExcessEventsFromStart(uncollapsedEvents, getMaximumUncollapsedTrackedEventsCount());
        collapsedLastBuiltinEvents = removeExcessEventsFromStart(collapsedLastBuiltinEvents, getMaximumCollapsedLastBuiltinTrackedEventsCount());
        collapsedLastCustomEvents = removeExcessEventsFromStart(collapsedLastCustomEvents, getMaximumCollapsedLastCustomTrackedEventsCount());
        collapsedOtherEvents = removeExcessEventsFromStart(collapsedOtherEvents, getMaximumCollapsedOtherTrackedEventsCount());

        long last1days=0L, last3days=0L, last7days=0L, last15days=0L, last30days=0L, last60days=0L, last90days=0L;

        // Reconstruct the whole list
        List<JSONObject> storeTrackedEvents = new ArrayList<>(collapsedLastBuiltinEvents.size() + collapsedLastCustomEvents.size() + collapsedOtherEvents.size() + uncollapsedEvents.size());
        storeTrackedEvents.addAll(collapsedLastBuiltinEvents);
        storeTrackedEvents.addAll(collapsedLastCustomEvents);
        storeTrackedEvents.addAll(collapsedOtherEvents);
        storeTrackedEvents.addAll(uncollapsedEvents);
        long uncollapsedCount = 0L;
        for (JSONObject trackedEvent : uncollapsedEvents) {
            String trackedEventType = trackedEvent.optString("type");
            if (type.equals(trackedEventType)) {
                ++uncollapsedCount;
                long actionDate = trackedEvent.optLong("actionDate", now);
                long numberOfDaysSinceNow = (long)Math.floor((double)(now - actionDate) / 86400000d);
                if (numberOfDaysSinceNow <= 1) ++last1days;
                if (numberOfDaysSinceNow <= 3) ++last3days;
                if (numberOfDaysSinceNow <= 7) ++last7days;
                if (numberOfDaysSinceNow <= 15) ++last15days;
                if (numberOfDaysSinceNow <= 30) ++last30days;
                if (numberOfDaysSinceNow <= 60) ++last60days;
                if (numberOfDaysSinceNow <= 90) ++last90days;
            }

        }

        occurrences.allTime = Math.max(allTime, uncollapsedCount);
        occurrences.last1days = last1days;
        occurrences.last3days = last3days;
        occurrences.last7days = last7days;
        occurrences.last15days = last15days;
        occurrences.last30days = last30days;
        occurrences.last60days = last60days;
        occurrences.last90days = last90days;

        try {
            if (collapsedEventData != null) {
                collapsedEventData.put("occurrences", occurrences.toJSON());
            }
            if (uncollapsedEventData != null) {
                uncollapsedEventData.put("occurrences", occurrences.toJSON());
            }
        } catch (JSONException e) {
            Log.w(WonderPush.TAG, "Could not store occurrences", e);
        }

        // Store the new list
        setTrackedEvents(storeTrackedEvents);
        return occurrences;
    }

    private static <T> List<T> removeExcessEventsFromStart(List<T> list, int max) {
        int excessEvents = list.size() - max;
        return list.subList(Math.max(0, excessEvents), list.size());
    }

    static int getMaximumCollapsedLastBuiltinTrackedEventsCount() {
        return maximumCollapsedLastBuiltinTrackedEventsCount;
    }

    static void setMaximumCollapsedLastBuiltinTrackedEventsCount(int value) {
        maximumCollapsedLastBuiltinTrackedEventsCount = value;
    }

    static int getMaximumCollapsedLastCustomTrackedEventsCount() {
        return maximumCollapsedLastCustomTrackedEventsCount;
    }

    static void setMaximumCollapsedLastCustomTrackedEventsCount(int value) {
        maximumCollapsedLastCustomTrackedEventsCount = value;
    }

    static int getMaximumCollapsedOtherTrackedEventsCount() {
        return maximumCollapsedOtherTrackedEventsCount;
    }

    static void setMaximumCollapsedOtherTrackedEventsCount(int value) {
        maximumCollapsedOtherTrackedEventsCount = value;
    }

    static int getMaximumUncollapsedTrackedEventsCount() {
        return maximumUncollapsedTrackedEventsCount;
    }

    static void setMaximumUncollapsedTrackedEventsCount(int value) {
        maximumUncollapsedTrackedEventsCount = value;
    }

    static long getMaximumUncollapsedTrackedEventsAgeMs() {
        return maximumUncollapsedTrackedEventsAgeMs;
    }

    static void setMaximumUncollapsedTrackedEventsAgeMs(long value) {
        maximumUncollapsedTrackedEventsAgeMs = value;
    }

    static void setTrackedEvents(List<JSONObject> trackedEvents) {
        JSONArray storedTrackedEvents = trackedEvents == null ? null : new JSONArray(trackedEvents);
        putJSONArray(STORED_TRACKED_EVENTS_PREF_NAME, storedTrackedEvents);
    }

    public static List<JSONObject> getTrackedEvents() {
        List<JSONObject> result = cachedTrackedEvents.get();
        if (result == null) {
            result = getTrackedEventsFromStoredJSONArray(getJSONArray(STORED_TRACKED_EVENTS_PREF_NAME));
        }
        return result;
    }

    static List<JSONObject> getTrackedEventsFromStoredJSONArray(JSONArray storedTrackedEvents) {
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; storedTrackedEvents != null && i < storedTrackedEvents.length(); i++) {
            JSONObject event = storedTrackedEvents.optJSONObject(i);
            if (event == null) continue;
            if (!event.has("creationDate") && event.has("actionDate")) {
                try {
                    event.putOpt("creationDate", event.opt("actionDate"));
                } catch (JSONException ex) {
                    Log.w(WonderPush.TAG, "Unexpected exception while copying actionDate into creationDate", ex);
                }
            }
            result.add(event);
        }
        return result;
    }

    public static class Occurrences {
        public Long allTime;
        public Long last1days;
        public Long last3days;
        public Long last7days;
        public Long last15days;
        public Long last30days;
        public Long last60days;
        public Long last90days;
        public JSONObject toJSON() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("allTime", allTime);
            result.put("last1days", last1days);
            result.put("last3days", last3days);
            result.put("last7days", last7days);
            result.put("last15days", last15days);
            result.put("last30days", last30days);
            result.put("last60days", last60days);
            result.put("last90days", last90days);
            return result;
        }
    }

}
