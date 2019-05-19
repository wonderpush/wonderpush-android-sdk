package com.wonderpush.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

class InstallationManager {

    static final String TAG = WonderPush.TAG;

    /**
     * How long to wait for no other call to {@link #putInstallationCustomProperties(JSONObject)}
     * before writing changes to the server.
     */
    protected static final long CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY = 5 * 1000;

    /**
     * How long to wait for another call to {@link #putInstallationCustomProperties(JSONObject)} at maximum,
     * if there are no pause of {@link #CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY} time between calls.
     */
    protected static final long CACHED_INSTALLATION_CUSTOM_PROPERTIES_MAX_DELAY = 20 * 1000;

    public static JSONObject getInstallationCustomProperties() {
        try {
            return JSONSyncInstallationCustom.forCurrentUser().getSdkState();
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to read installation custom properties", ex);
            return new JSONObject();
        }
    }

    public static synchronized void putInstallationCustomProperties(JSONObject customProperties) {
        try {
            JSONSyncInstallationCustom.forCurrentUser().put(customProperties);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to put installation custom properties " + customProperties, ex);
        }
    }

    public static void setProperty(String field, Object value) {
        if (field == null) return;
        value = JSONUtil.wrap(value);
        try {
            JSONObject diff = new JSONObject();
            diff.put(field, value);
            putInstallationCustomProperties(diff);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to setProperty(" + field + ", " + value + ")", ex);
        }
    }

    public static void unsetProperty(String field) {
        if (field == null) return;
        try {
            JSONObject diff = new JSONObject();
            diff.put(field, JSONObject.NULL);
            putInstallationCustomProperties(diff);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to unsetProperty(" + field + ")", ex);
        }
    }

    public static void addProperty(String field, Object value) {
        value = JSONUtil.wrap(value);
        if (field == null || value == null || value == JSONObject.NULL) return;
        // The contract is to actually append new values only, not shuffle or deduplicate everything,
        // hence the array and the set.
        List<Object> values = new ArrayList<>(getPropertyValues(field));
        Set<Object> set = new HashSet<>(values);
        JSONArray inputs = value instanceof JSONArray ? (JSONArray) value : new JSONArray().put(value);
        for (int i = 0, e = inputs.length(); i < e; ++i) {
            try {
                Object input = inputs.get(i);
                if (input == null || input == JSONObject.NULL) continue;
                if (set.contains(input)) continue;
                values.add(input);
                set.add(input);
            } catch (JSONException ex) {
                Log.e(WonderPush.TAG, "Unexpected exception in addProperty", ex);
            }
        }
        setProperty(field, values);
    }

    public static void removeProperty(String field, Object value) {
        value = JSONUtil.wrap(value);
        if (field == null || value == null) return; // Note: We accept removing JSONObject.NULL
        // The contract is to actually remove every listed values (all duplicated appearances), not shuffle or deduplicate everything else
        List<Object> values = getPropertyValues(field);
        JSONArray inputs = value instanceof JSONArray ? (JSONArray) value : new JSONArray().put(value);
        Set<Object> set = new HashSet<>(JSONUtil.JSONArrayToList(inputs, Object.class));
        if (set.isEmpty()) return;
        JSONArray newValues = new JSONArray();
        for (Object item : values) {
            if (item == null) continue;
            if (set.contains(item)) continue;
            newValues.put(item);
        }
        setProperty(field, newValues);
    }

    public static Object getPropertyValue(String field) {
        if (field == null) return JSONObject.NULL;
        JSONObject properties = getInstallationCustomProperties();
        Object value = properties.opt(field);
        while (value instanceof JSONArray) { // Note, the documentation says *never* a JSONArray, so we use a while instead of an if to sure of that
            value = ((JSONArray) value).length() > 0 ? ((JSONArray) value).opt(0) : null;
        }
        if (value == null) value = JSONObject.NULL;
        return value;
    }

    public static List<Object> getPropertyValues(String field) {
        if (field == null) return Collections.emptyList();
        JSONObject properties = getInstallationCustomProperties();
        Object value = properties.opt(field);
        if (value == null || value == JSONObject.NULL) {
            return Collections.emptyList();
        } else if (value instanceof JSONArray) {
            return JSONUtil.JSONArrayToList((JSONArray) value, Object.class);
        } else {
            return Collections.singletonList(value);
        }
    }

    public static void addTag(String... tag) {
        Set<String> tags = new TreeSet<>(getTags()); // use a sorted implementation to avoid useless diffs
        for (String aTag : tag) {
            if (aTag != null && !aTag.isEmpty()) {
                tags.add(aTag);
            } else {
                Log.w(TAG, "Dropping invalid tag " + aTag);
            }
        }
        try {
            JSONObject diff = new JSONObject();
            diff.putOpt("tags", new JSONArray(tags));
            putInstallationCustomProperties(diff);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to addTag", ex);
        }
    }

    public static void removeTag(String... tag) {
        Set<String> tags = new TreeSet<>(getTags()); // use a sorted implementation to avoid useless diffs
        tags.removeAll(Arrays.asList(tag));
        try {
            JSONObject diff = new JSONObject();
            diff.putOpt("tags", new JSONArray(tags));
            putInstallationCustomProperties(diff);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to addTag", ex);
        }
    }

    public static void removeAllTags() {
        try {
            JSONObject diff = new JSONObject();
            diff.putOpt("tags", JSONObject.NULL);
            putInstallationCustomProperties(diff);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to removeAllTags", ex);
        }
    }

    public static Set<String> getTags() {
        JSONObject custom = getInstallationCustomProperties();
        JSONArray tags = custom.optJSONArray("tags");
        if (tags == null) {
            tags = new JSONArray();
            // Recover from a potential scalar string value
            String val = JSONUtil.optString(custom, "tags");
            if (val != null) {
                tags.put(val);
            }
        }
        TreeSet<String> rtn = new TreeSet<>(); // use a sorted implementation to avoid useless diffs later on
        for (int i = 0, l = tags.length(); i < l; ++i) {
            try {
                Object val = tags.get(i);
                if (val instanceof String && !((String) val).isEmpty()) {
                    rtn.add((String) val);
                }
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to get tags at position " + i + " from " + tags, ex);
            }
        }
        return rtn;
    }

    public static boolean hasTag(String tag) {
        if (tag == null) return false;
        return getTags().contains(tag);
    }

    static void updateInstallation(JSONObject properties, boolean overwrite) {
        if (!WonderPush.hasUserConsent()) {
            WonderPush.logError("Not tracking updating installation without user consent. properties=" + properties + ", overwrite=" + overwrite);
            return;
        }

        String propertyEndpoint = "/installation";
        RequestParams parameters = new RequestParams();
        parameters.put("body", properties.toString());
        parameters.put("overwrite", overwrite ? "true" : "false");
        WonderPush.postEventually(propertyEndpoint, parameters);
    }

    protected static void updateInstallationCoreProperties(final Context context) {
        WonderPush.safeDefer(new Runnable() {
            @Override
            public void run() {
                JSONObject properties = new JSONObject();
                AtomicReference<String> federatedId = null;
                try {
                    federatedId = WonderPush.getFederatedIdAlreadyInBackground();
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while reading federatedId", ex);
                }
                try {
                    JSONObject application = new JSONObject();
                    application.put("version", getApplicationVersion());
                    application.put("sdkVersion", getSDKVersion());
                    properties.put("application", application);

                    JSONObject device = new JSONObject();
                    device.put("id", WonderPush.getDeviceId());
                    if (federatedId != null) {
                        String federatedIdStr = federatedId.get();
                        if (federatedIdStr == null) {
                            device.put("federatedId", JSONObject.NULL);
                        } else {
                            device.put("federatedId", "0:" + federatedIdStr);
                        }
                    }
                    device.put("platform", "Android");
                    device.put("osVersion", getOsVersion());
                    device.put("brand", getDeviceBrand());
                    device.put("model", getDeviceModel());
                    device.put("name", getDeviceName());
                    device.put("screenWidth", getScreenWidth(context));
                    device.put("screenHeight", getScreenHeight(context));
                    device.put("screenDensity", getScreenDensity(context));

                    JSONObject configuration = new JSONObject();
                    configuration.put("timeZone", getUserTimezone());
                    configuration.put("carrier", getCarrierName());
                    configuration.put("locale", getLocaleString());
                    configuration.put("country", getLocaleCountry());
                    configuration.put("currency", getLocaleCurrency());
                    device.put("configuration", configuration);

                    properties.put("device", device);

                    String cachedPropertiesString = WonderPushConfiguration.getCachedInstallationCoreProperties();
                    JSONObject cachedProperties = null;
                    if (cachedPropertiesString != null) {
                        try {
                            cachedProperties = new JSONObject(cachedPropertiesString);
                        } catch (JSONException ex) {
                            Log.e(TAG, "Unexpected error while parsing cached core properties", ex);
                            Log.e(TAG, "Input was: " + cachedPropertiesString);
                        }
                    }
                    String cachedPropertiesAccessToken = WonderPushConfiguration.getCachedInstallationCorePropertiesAccessToken();
                    if (!JSONUtil.equals(properties, cachedProperties)
                            || cachedPropertiesAccessToken == null && WonderPushConfiguration.getAccessToken() != null
                            || cachedPropertiesAccessToken != null && !cachedPropertiesAccessToken.equals(WonderPushConfiguration.getAccessToken())
                            ) {
                        WonderPushConfiguration.setCachedInstallationCorePropertiesDate(System.currentTimeMillis());
                        WonderPushConfiguration.setCachedInstallationCoreProperties(properties.toString());
                        WonderPushConfiguration.setCachedInstallationCorePropertiesAccessToken(WonderPushConfiguration.getAccessToken());
                        updateInstallation(properties, false);
                    }
                } catch (JSONException ex) {
                    Log.e(TAG, "Unexpected error while updating installation core properties", ex);
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while updating installation core properties", ex);
                }
            }
        }, 0);
    }

    protected static String getApplicationVersion() {
        String versionName = null;
        try {
            PackageInfo packageInfo = WonderPush.getApplicationContext().getPackageManager().getPackageInfo(WonderPush.getApplicationContext().getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            WonderPush.logDebug("Could not retreive version name");
        }
        return versionName;
    }

    protected static String getOsVersion() {
        return "" + android.os.Build.VERSION.SDK_INT;
    }

    protected static String getUserTimezone() {
        return TimeZone.getDefault().getID();
    }

    protected static String getCarrierName() {
        TelephonyManager telephonyManager = ((TelephonyManager) WonderPush.getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE));
        return telephonyManager.getNetworkOperatorName();
    }

    protected static String getLocaleString() {
        return String.format("%s_%s", Locale.getDefault().getLanguage()
                .toLowerCase(Locale.ENGLISH), Locale.getDefault().getCountry()
                .toUpperCase(Locale.ENGLISH));
    }

    protected static String getLocaleCountry() {
        String rtn = Locale.getDefault().getCountry();
        if ("".equals(rtn)) {
            rtn = null;
        } else {
            rtn = rtn.toUpperCase();
        }
        return rtn;
    }

    protected static String getLocaleCurrency() {
        try {
            Currency currency = Currency.getInstance(Locale.getDefault());
            if (currency == null) return null;
            String rtn = currency.getCurrencyCode();
            if ("".equals(rtn)) {
                rtn = null;
            } else {
                rtn = rtn.toUpperCase();
            }
            return rtn;
        } catch (Exception e) { // mostly for IllegalArgumentException
            return null;
        }
    }

    protected static String getSDKVersion() {
        return WonderPush.SDK_VERSION;
    }

    /**
     * Gets the model of this android device.
     */
    protected static String getDeviceModel() {
        return Build.MODEL;
    }

    protected static String getDeviceBrand() {
        return Build.MANUFACTURER;
    }

    /**
     * Returns the Bluetooth device name, if permissions are granted,
     * and provided the device actually has Bluetooth.
     */
    protected static String getDeviceName() {
        try {
            if (WonderPush.getApplicationContext().getPackageManager().checkPermission(android.Manifest.permission.BLUETOOTH, WonderPush.getApplicationContext().getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                BluetoothAdapter btDevice = BluetoothAdapter.getDefaultAdapter();
                return btDevice.getName();
            }
        } catch (Exception ex) {
            // Ignore
        }
        return null;
    }

    protected static int getScreenDensity(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.densityDpi;
    }

    protected static int getScreenWidth(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.widthPixels;
    }

    protected static int getScreenHeight(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.heightPixels;
    }

}
