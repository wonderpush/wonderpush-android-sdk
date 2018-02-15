package com.wonderpush.sdk;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    protected static void updateInstallationCoreProperties(Context context) {
        JSONObject properties = new JSONObject();
        try {
            JSONObject application = new JSONObject();
            application.put("version", getApplicationVersion());
            application.put("sdkVersion", getSDKVersion());
            properties.put("application", application);

            JSONObject device = new JSONObject();
            device.put("id", WonderPush.getDeviceId());
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

            JSONObject capabilities = new JSONObject();
            capabilities.put("bluetooth", getBluetoothSupported(context));
            capabilities.put("bluetoothLe", getBluetoothLESupported(context));
            capabilities.put("nfc", getNFCSupported(context));
            capabilities.put("ir", getIRSupported(context));
            capabilities.put("telephony", getTelephonySupported(context));
            capabilities.put("telephonyGsm", getTelephonyGSMSupported(context));
            capabilities.put("telephonyCdma", getTelephonyCDMASupported(context));
            capabilities.put("wifi", getWifiSupported(context));
            capabilities.put("wifiDirect", getWifiDirectSupported(context));
            capabilities.put("gps", getGPSSupported(context));
            capabilities.put("networkLocation", getNetworkLocationSupported(context));
            capabilities.put("camera", getCameraSupported(context));
            capabilities.put("frontCamera", getFrontCameraSupported(context));
            capabilities.put("microphone", getMicrophoneSupported(context));
            capabilities.put("sensorAccelerometer", getSensorAccelerometerSupported(context));
            capabilities.put("sensorBarometer", getSensorBarometerSupported(context));
            capabilities.put("sensorCompass", getSensorCompassSupported(context));
            capabilities.put("sensorGyroscope", getSensorGyroscopeSupported(context));
            capabilities.put("sensorLight", getSensorLightSupported(context));
            capabilities.put("sensorProximity", getSensorProximitySupported(context));
            capabilities.put("sensorStepCounter", getSensorStepCounterSupported(context));
            capabilities.put("sensorStepDetector", getSensorStepDetectorSupported(context));
            capabilities.put("sip", getSIPSupported(context));
            capabilities.put("sipVoip", getSIPVOIPSupported(context));
            capabilities.put("touchscreen", getTouchscreenSupported(context));
            capabilities.put("touchscreenTwoFingers", getTouchscreenTwoFingersSupported(context));
            capabilities.put("touchscreenDistinct", getTouchscreenDistinctSupported(context));
            capabilities.put("touchscreenFullHand", getTouchscreenFullHandSupported(context));
            capabilities.put("usbAccessory", getUSBAccessorySupported(context));
            capabilities.put("usbHost", getUSBHostSupported(context));
            device.put("capabilities", capabilities);

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

    protected static String getScreenSize(Context context) {
        return getScreenWidth(context) + "x" + getScreenHeight(context);
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

    protected static boolean getBluetoothSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected static boolean getBluetoothLESupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        }
        return false;
    }

    protected static boolean getNFCSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected static boolean getIRSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR);
        }
        return false;
    }

    protected static boolean getTelephonySupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    protected static boolean getTelephonyGSMSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM);
    }

    protected static boolean getTelephonyCDMASupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA);
    }

    protected static boolean getWifiSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    protected static boolean getWifiDirectSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
    }

    protected static boolean getGPSSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    protected static boolean getNetworkLocationSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK);
    }

    protected static boolean getCameraSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    protected static boolean getFrontCameraSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    protected static boolean getMicrophoneSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    protected static boolean getSensorAccelerometerSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
    }

    protected static boolean getSensorBarometerSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
    }

    protected static boolean getSensorCompassSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
    }

    protected static boolean getSensorGyroscopeSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
    }

    protected static boolean getSensorLightSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);
    }

    protected static boolean getSensorProximitySupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected static boolean getSensorStepCounterSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected static boolean getSensorStepDetectorSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
        }
        return false;
    }

    protected static boolean getSIPSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP);
    }

    protected static boolean getSIPVOIPSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP_VOIP);
    }

    protected static boolean getTouchscreenSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    protected static boolean getTouchscreenTwoFingersSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);
    }

    protected static boolean getTouchscreenDistinctSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    }

    protected static boolean getTouchscreenFullHandSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND);
    }

    protected static boolean getUSBAccessorySupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY);
    }

    protected static boolean getUSBHostSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
    }

}
