package com.wonderpush.sdk;

import android.content.Context;
import android.content.SharedPreferences;

class WonderPushConfiguration {

    private static final String PREF_FILE = "wonderpush";

    private static final String ACCESS_TOKEN_PREF_NAME = "__wonderpush_access_token";
    private static final String SID_PREF_NAME = "__wonderpush_sid";
    private static final String INSTALLATION_ID_PREF_NAME = "__installation_id";
    private static final String USER_ID_PREF_NAME = "__user_id";

    private static final String GCM_REGISTRATION_ID_PREF_NAME = "__wonderpush_gcm_registration_id";
    private static final String GCM_REGISTRATION_APP_VERSION_PREF_NAME = "__wonderpush_gcm_registration_app_version";
    private static final String GCM_REGISTRATION_SENDER_IDS_PREF_NAME = "__wonderpush_gcm_registration_sender_ids";

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

    protected static void putString(String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        if (null == value) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
        editor.commit();
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

    protected static void putInt(String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.commit();
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

}
