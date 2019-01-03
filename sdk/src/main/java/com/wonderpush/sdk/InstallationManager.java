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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
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

    static void updateInstallation(JSONObject properties, boolean overwrite) {
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

    protected static int getApplicationVersionCode() {
        int versionCode = -1;
        try {
            PackageInfo packageInfo = WonderPush.getApplicationContext().getPackageManager().getPackageInfo(WonderPush.getApplicationContext().getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            WonderPush.logDebug("Could not retreive version code");
        }
        return versionCode;
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
