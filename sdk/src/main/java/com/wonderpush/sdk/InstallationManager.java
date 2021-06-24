package com.wonderpush.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

public class InstallationManager {

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
            JSONObject sync = JSONSyncInstallation.forCurrentUser().getSdkState();
            JSONObject custom = sync == null ? null : sync.optJSONObject("custom");
            if (custom == null) return new JSONObject();
            Iterator<String> it = custom.keys();
            while (it.hasNext()) {
                String key = it.next();
                if (key.indexOf('_') < 0) {
                    it.remove();
                }
            }
            return custom;
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to read installation custom properties", ex);
            return new JSONObject();
        }
    }

    public static synchronized void putInstallationCustomProperties(JSONObject customProperties) {
        try {
            customProperties = JSONUtil.deepCopy(customProperties);
            Iterator<String> it = customProperties.keys();
            while (it.hasNext()) {
                String key = it.next();
                if (key.indexOf('_') < 0) {
                    Log.w(WonderPush.TAG, "Dropping installation property with no prefix: " + key);
                    it.remove();
                }
            }
            JSONObject diff = new JSONObject();
            diff.put("custom", customProperties);
            JSONSyncInstallation.forCurrentUser().put(diff);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to put installation custom properties " + customProperties, ex);
        }
    }

    public static synchronized void setProperty(String field, Object value) {
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

    public static synchronized void unsetProperty(String field) {
        if (field == null) return;
        try {
            JSONObject diff = new JSONObject();
            diff.put(field, JSONObject.NULL);
            putInstallationCustomProperties(diff);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to unsetProperty(" + field + ")", ex);
        }
    }

    public static synchronized void addProperty(String field, Object value) {
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

    public static synchronized void removeProperty(String field, Object value) {
        value = JSONUtil.wrap(value);
        if (field == null || value == null) return; // Note: We accept removing JSONObject.NULL
        // The contract is to actually remove every listed values (all duplicated appearances), not shuffle or deduplicate everything else
        List<Object> values = getPropertyValues(field);
        JSONArray inputs = null;
        try {
            inputs = new JSONArray((value instanceof JSONArray ? (JSONArray) value : new JSONArray().put(value)).toString());
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Unexpected exception in removeProperty", ex);
            return;
        }
        Set<Object> set = new HashSet<>(JSONUtil.JSONArrayToList(inputs, Object.class, true));
        if (set.isEmpty()) return;
        JSONArray newValues = new JSONArray();
        for (Object item : values) {
            if (item == null) continue;
            if (set.contains(item)) continue;
            newValues.put(item);
        }
        setProperty(field, newValues);
    }

    public static synchronized Object getPropertyValue(String field) {
        if (field == null) return JSONObject.NULL;
        JSONObject properties = getInstallationCustomProperties();
        Object value = properties.opt(field);
        while (value instanceof JSONArray) { // Note, the documentation says *never* a JSONArray, so we use a while instead of an if to sure of that
            value = ((JSONArray) value).length() > 0 ? ((JSONArray) value).opt(0) : null;
        }
        if (value == null) value = JSONObject.NULL;
        return value;
    }

    public static synchronized List<Object> getPropertyValues(String field) {
        if (field == null) return Collections.emptyList();
        JSONObject properties = getInstallationCustomProperties();
        Object value = properties.opt(field);
        if (value == null || value == JSONObject.NULL) {
            return Collections.emptyList();
        } else if (value instanceof JSONArray) {
            return JSONUtil.JSONArrayToList((JSONArray) value, Object.class, true);
        } else {
            return Collections.singletonList(value);
        }
    }

    public static synchronized void addTag(String... tag) {
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
            JSONObject custom = new JSONObject();
            diff.put("custom", custom);
            custom.putOpt("tags", new JSONArray(tags));
            JSONSyncInstallation.forCurrentUser().put(diff);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to addTag", ex);
        }
    }

    public static synchronized void removeTag(String... tag) {
        Set<String> tags = new TreeSet<>(getTags()); // use a sorted implementation to avoid useless diffs
        tags.removeAll(Arrays.asList(tag));
        try {
            JSONObject diff = new JSONObject();
            JSONObject custom = new JSONObject();
            diff.put("custom", custom);
            custom.putOpt("tags", new JSONArray(tags));
            JSONSyncInstallation.forCurrentUser().put(diff);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to addTag", ex);
        }
    }

    public static synchronized void removeAllTags() {
        try {
            JSONObject diff = new JSONObject();
            JSONObject custom = new JSONObject();
            diff.put("custom", custom);
            custom.putOpt("tags", JSONObject.NULL);
            JSONSyncInstallation.forCurrentUser().put(diff);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to removeAllTags", ex);
        }
    }

    public static synchronized Set<String> getTags() {
        JSONObject custom;
        try {
            JSONObject sync = JSONSyncInstallation.forCurrentUser().getSdkState();
            custom = sync != null ? sync.optJSONObject("custom") : null;
            if (custom == null) custom = new JSONObject();
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to read installation custom properties", ex);
            custom = new JSONObject();
        }
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

    public static synchronized boolean hasTag(String tag) {
        if (tag == null) return false;
        return getTags().contains(tag);
    }

    protected static void updateInstallationCoreProperties(final Context context) {
        WonderPush.safeDefer(new Runnable() {
            @Override
            public void run() {
                JSONObject diff = new JSONObject();
                try {
                    JSONObject application = new JSONObject();
                    application.put("version", getApplicationVersion());
                    application.put("sdkVersion", getSDKVersion());
                    application.put("integrator", WonderPush.getIntegrator() == null ? JSONObject.NULL : WonderPush.getIntegrator());
                    diff.put("application", application);

                    JSONObject device = new JSONObject();
                    device.put("id", WonderPush.getDeviceId());
                    device.put("platform", "Android");
                    device.put("osVersion", getOsVersion());
                    device.put("brand", getDeviceBrand());
                    device.put("model", getDeviceModel());
                    device.put("name", getDeviceName());
                    device.put("screenWidth", getScreenWidth());
                    device.put("screenHeight", getScreenHeight());
                    device.put("screenDensity", getScreenDensity());

                    JSONObject configuration = new JSONObject();
                    configuration.put("timeZone", getUserTimezone());
                    configuration.put("timeOffset", getUserTimeOffset());
                    configuration.put("carrier", getCarrierName());
                    configuration.put("locale", getLocaleString());
                    configuration.put("country", getLocaleCountry());
                    configuration.put("currency", getLocaleCurrency());
                    device.put("configuration", configuration);

                    diff.put("device", device);
                    JSONSyncInstallation.forCurrentUser().put(diff);

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
        return WonderPush.getTimeZone();
    }

    protected static long getUserTimeOffset() {
        String timeZone = WonderPush.getTimeZone();
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        return tz.getOffset(TimeSync.getTime());
    }

    protected static String getCarrierName() {
        TelephonyManager telephonyManager = ((TelephonyManager) WonderPush.getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE));
        return telephonyManager.getNetworkOperatorName();
    }

    protected static String getLocaleString() {
        return WonderPush.getLocale();
    }

    protected static String getLocaleCountry() {
        return WonderPush.getCountry();
    }

    protected static String getLocaleCurrency() {
        return WonderPush.getCurrency();
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

    protected static int getScreenDensity() {
        return Resources.getSystem().getConfiguration().densityDpi;
    }

    protected static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    protected static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

}
