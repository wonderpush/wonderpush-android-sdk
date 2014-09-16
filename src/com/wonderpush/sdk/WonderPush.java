package com.wonderpush.sdk;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.OpenUDID.OpenUDID_manager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.wonderpush.sdk.WonderPushDialogBuilder.Button;
import com.wonderpush.sdk.WonderPushDialogBuilder.Button.Action;

/**
 * Main class of the WonderPush SDK.
 *
 * <p>You would normally only interact with this class, which has all static members.</p>
 *
 * <p>
 *   Make sure you properly installed the WonderPush SDK, as described in
 *   <a href="../../../packages.html">the guide</a>.
 * </p>
 *
 * <p>You must call {@link #initialize(Activity, String, String, String)} before using any other function.</p>
 *
 * <p>
 *   Troubleshooting tip:
 *   As the SDK should not interfere with your application other than when a notification is to be shown,
 *   make sure to monitor your logs for the <tt>WonderPush</tt> tag during development,
 *   if things did not went as smoothly as they should have.
 * </p>
 */
public class WonderPush {

    static final String TAG = WonderPush.class.getSimpleName();

    private static Context sApplicationContext;
    private static String sClientId;
    private static String sClientSecret;
    private static Location sLocation;
    private static String sBaseURL;
    private static String sLang;
    private static boolean sIsInitialized = false;
    private static boolean sIsReachable = false;

    /**
     * The timeout for WebView requests
     */
    protected static final int WEBVIEW_REQUEST_TOTAL_TIMEOUT = 10000;
    protected static final int API_INT = 1; // reset SDK_VERSION when bumping this
    protected static final String API_VERSION = "v" + API_INT;
    protected static final String SDK_VERSION = "Android-" + API_INT + ".0.9.1"; // reset to .1.0.0 when bumping API_INT
    private static final String PREF_FILE = "wonderpush";
    protected static final int ERROR_INVALID_SID = 12017;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Local intent broadcasted when the WonderPush SDK has been initialized and network is reachable.
     */
    public static final String INTENT_INTIALIZED = "wonderpushInitialized";

    /**
     * Local intent broadcasted when a resource has been successfully preloaded.
     */
    protected static final String INTENT_RESOURCE_PRELOADED = "wonderpushResourcePreloaded";

    /**
     * The extra key for the path of a preloaded resource in a {@link #INTENT_RESOURCE_PRELOADED} intent.
     */
    protected static final String INTENT_RESOURCE_PRELOADED_EXTRA_PATH = "wonderpushResourcePreloadedPath";

    /**
     * Intent extra key for GCM notification data when the user clicks the notification.
     */
    protected static final String NOTIFICATION_EXTRA_KEY = "_wpNotification";

    static enum NotificationType {
        SIMPLE("simple"),
        DATA("data"),
        TEXT("text"),
        HTML("html"),
        MAP("map"),
        URL("url"),
        ;

        private String type;

        private NotificationType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }

        public static NotificationType fromString(String type) {
            if (type == null) {
                throw new NullPointerException();
            }
            for (NotificationType notificationType : NotificationType.values()) {
                if (type.equals(notificationType.toString())) {
                    return notificationType;
                }
            }
            throw new IllegalArgumentException("Constant \"" + type + "\" is not a known " + NotificationType.class.getSimpleName());
        }
    }

    private static final String PRODUCTION_API_URL = "https://api.wonderpush.com/" + API_VERSION;
    protected static boolean SHOW_DEBUG = true;
    protected static final int ERROR_INVALID_ACCESS_TOKEN = 11003;
    protected static final String DEFAULT_LANGUAGE_CODE = "en";
    protected static final String[] VALID_LANGUAGE_CODES = {
            "af", "ar", "be", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "en_GB", "en_US", "es", "es_ES", "es_MX",
            "et", "fa", "fi", "fr", "fr_FR", "fr_CA", "he", "hi", "hr", "hu", "id", "is", "it", "ja", "ko", "lt", "lv",
            "mk", "ms", "nb", "nl", "pa", "pl", "pt", "pt_PT", "pt_BR", "ro", "ru", "sk", "sl", "sq", "sr", "sv", "sw",
            "ta", "th", "tl", "tr", "uk", "vi", "zh", "zh_CN", "zh_TW", "zh_HK",
    };

    protected WonderPush() {
    }

    /**
     * Gets the current player id from the local cache.
     *
     * @return The current player id from the local cache.
     */
    protected static String getUserId() {
        return WonderPushRestClient.getUserId();
    }

    private static boolean checkPlayService(Activity activity) {
        try {
            Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
            int resultCode = GooglePlayServicesUtil
                    .isGooglePlayServicesAvailable(activity);
            if (resultCode != ConnectionResult.SUCCESS) {
                if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                    GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                } else {
                    Log.w(TAG, "This device does not support google play service no push notification will be received");
                }
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "The Google Play Services have not been added to the application");
            return false;
        }
    }

    /**
     * Helper method that will register a device for google cloud messages
     * notification and register the device token to WonderPush. This method is
     * called within {@link #initialize(Activity, String, String, String)}.
     *
     * @param activity
     *            The current activity.
     */
    protected static void registerForPushNotification(Activity activity) {
        if (checkPlayService(activity)) {
            WonderPushGcmClient.registerForPushNotification(activity);
        } else {
            Log.w(TAG, "Google Play Services not present. Check your setup.");
        }
    }

    protected static void logDebug(String debug) {
        if (WonderPush.SHOW_DEBUG) {
            Log.d(TAG, debug);
        }
    }

    private static void handleReceivedNotification(Activity activity, JSONObject data) {
        NotificationType type = null;
        try {
            type = NotificationType.fromString(data.optString("type"));
        } catch (NullPointerException e) {
        } catch (IllegalArgumentException e) {
        }
        if (type == null) {
            Log.w(TAG, "No built-in action for type " + data.optString("type"));
            return;
        }
        switch (type) {
            case SIMPLE:
            case DATA:
                // Nothing to do
                break;
            case URL:
                handleURLNotification(activity, data);
                break;
            case TEXT:
                handleDialogNotification(activity, data);
                break;
            case MAP:
                handleMapNotification(activity, data);
                break;
            case HTML:
                handleHTMLNotification(activity, data);
                break;
            default:
                Log.w(TAG, "TODO: built-in action for type " + type);
                break;
        }
    }

    /**
     * Method to call on your {@code onCreate()} method to handle the WonderPush notification.
     * Must be added to the {@link Activity} implementation of the class you gave to
     * {@link #onBroadcastReceived(Context, Intent, int, Class)}
     * (will be called within {@link WonderPush#initialize(Activity, String, String)}).
     * Example:
     *
     * <pre>
     * <code>
     * protected void onCreate(Bundle savedInstance) {
     *     WonderPush.onCreateMainActivity(this, getIntent());
     * }
     * </code>
     * </pre>
     *
     * @param activity
     *            The current activity.
     * @param intent
     *            The intent the activity received.
     *
     * @return <code>true</code> if handled, <code>false</code> otherwise.
     */
    private static boolean onCreateMainActivity(final Activity activity, Intent intent) {
        if (intent.hasExtra(WonderPush.NOTIFICATION_EXTRA_KEY)) {
            String notificationString = intent.getStringExtra(WonderPush.NOTIFICATION_EXTRA_KEY);
            logDebug("Handling notification at main activity creation: " + notificationString);
            try {
                final JSONObject notification = new JSONObject(notificationString);
                JSONObject trackData = new JSONObject();
                trackData.put("campaignId", notification.optString("c"));
                trackData.put("notificationId", notification.optString("n"));
                WonderPush.trackInternalEvent("@NOTIFICATION_OPENED", trackData);

                if (sIsInitialized) {
                    handleReceivedNotification(activity, notification);
                } else {
                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            handleReceivedNotification(activity, notification);
                            LocalBroadcastManager.getInstance(activity).unregisterReceiver(this);
                        }
                    };

                    IntentFilter filter = new IntentFilter(WonderPush.INTENT_INTIALIZED);
                    LocalBroadcastManager.getInstance(activity).registerReceiver(receiver, filter);
                }

                return true;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse notification JSON object", e);
            }

        }
        return false;
    }

    /**
     * Gets the clientId that was specified during the {@link WonderPush.initialize(Context, String, String} call.
     */
    protected static String getClientId() {
        return sClientId;
    }

    /**
     * Gets the clientSecret that was specified during the {@link WonderPush.initialize(Context, String, String} call.
     */
    protected static String getClientSecret() {
        return sClientSecret;
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
            BluetoothAdapter btDevice = BluetoothAdapter.getDefaultAdapter();
            return btDevice.getName();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Returns the UDID determined by OpenUDID.
     *
     * @return The UDID determined by OpenUDID or null if OpenUDID is not initialized.
     */
    protected static String getUDID() {
        if (OpenUDID_manager.isInitialized())
            return OpenUDID_manager.getOpenUDID();
        return null;
    }

    protected static void setNetworkAvailable(boolean state) {
        sIsReachable = state;
    }

    protected static boolean isNetworkAvailable() {
        return sIsReachable;
    }

    /**
     * Returns the base URL for the WonderPush API.
     * This is the URL used to prefix every API resource path.
     *
     * @return The base URL.
     */
    protected static String getBaseURL() {
        return sBaseURL;
    }

    /**
     * Returns the base URL for the WonderPush API with a <code>http:</code> scheme.
     *
     * @see #getBaseURL()
     *
     * @return The base URL.
     */
    protected static String getNonSecureBaseURL() {
        return sBaseURL.replaceFirst("https:", "http:");
    }

    /**
     * A GET request.
     *
     * @param resource
     *            The resource path, starting with /.
     * @param params
     *            AsyncHttpClient request parameters.
     * @param responseHandler
     *            An AsyncHttpClient response handler.
     */
    protected static void get(String resource, WonderPush.RequestParams params,
            WonderPush.ResponseHandler responseHandler) {
        WonderPushRestClient.get(resource, params, responseHandler);
    }

    /**
     * A POST request.
     *
     * @param resource
     *            The resource path, starting with /.
     * @param params
     *            AsyncHttpClient request parameters.
     * @param responseHandler
     *            An AsyncHttpClient response handler.
     */
    protected static void post(String resource, WonderPush.RequestParams params,
            WonderPush.ResponseHandler responseHandler) {
        WonderPushRestClient.post(resource, params, responseHandler);
    }

    /**
     * A POST request that is guaranteed to be executed when a network
     * connection is present, surviving application reboot. The responseHandler
     * will be called only if the network is present when the request is first run.
     *
     * @param resource
     * @param params
     *            The request parameters. Only serializable parameters are
     *            guaranteed to survive a network error or device reboot.
     * @param responseHandler
     *            An AsyncHttpClient response handler.
     */
    protected static void postEventually(String resource,
            WonderPush.RequestParams params,
            WonderPush.ResponseHandler responseHandler) {
        WonderPushRestClient.postEventually(resource, params, responseHandler);
    }

    /**
     * A PUT request.
     *
     * @param resource
     *            The resource path, starting with /.
     * @param params
     *            AsyncHttpClient request parameters.
     * @param responseHandler
     *            An AsyncHttpClient response handler.
     */
    protected static void put(String resource, WonderPush.RequestParams params,
            WonderPush.ResponseHandler responseHandler) {
        WonderPushRestClient.put(resource, params, responseHandler);
    }

    /**
     * A DELETE request.
     *
     * @param resource
     *            The resource path, starting with /.
     * @param params
     *            AsyncHttpClient request parameters.
     * @param responseHandler
     *            An AsyncHttpClient response handler.
     */
    protected static void delete(String resource,
            WonderPush.ResponseHandler responseHandler) {
        WonderPushRestClient.delete(resource, responseHandler);
    }

    /**
     * Returns the Location as set in {@link setLocation(Location)} or the best
     * last known location of the {@link LocationManager} or null if permission
     * was not given.
     */
    protected static Location getLocation() {
        if (null != sLocation)
            return sLocation;

        Context applicationContext = getApplicationContext();

        if (applicationContext == null)
            return null;

        LocationManager locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location best = null;
            for (String provider : locationManager.getAllProviders()) {
                Location location;
                try {
                    location = locationManager.getLastKnownLocation(provider);
                } catch (SecurityException ex) {
                    continue;
                }
                // If this location is null, discard
                if (null == location) {
                    continue;
                }

                // If broken accuracy, discard
                if (location.getAccuracy() < 0) {
                    continue;
                }

                // If we have no best yet, use this first location
                if (null == best) {
                    best = location;
                    continue;
                }

                // If this location is significantly older, discard
                long timeDelta = location.getTime() - best.getTime();
                if (timeDelta < -1000 * 60 * 2) {
                    continue;
                }

                // If we have no accuracy, discard
                if (0 == location.getAccuracy()) {
                    continue;
                }

                // If this location is less accurate, discard
                if (best.getAccuracy() < location.getAccuracy()) {
                    continue;
                }

                best = location;
            }

            return best;
        } catch (java.lang.SecurityException e) {
            // Missing permission;
            return null;
        }
    }

    /**
     * Gets the current language. If language was specified using {@link WonderPush#setLang(String)},
     * this value is returned. Otherwise it is guessed from the system.
     *
     * @return The locale in use.
     */
    protected static String getLang() {
        if (null != sLang)
            return sLang;

        Locale locale = Locale.getDefault();

        if (null == locale)
            return DEFAULT_LANGUAGE_CODE;

        String language = locale.getLanguage();
        String country = locale.getCountry();
        String localeString = String.format("%s_%s",
                language != null ? language.toLowerCase(Locale.ENGLISH) : "",
                country != null ? country.toUpperCase(Locale.ENGLISH) : "");

        // 1. if no language is specified, return the default language
        if (null == language)
            return DEFAULT_LANGUAGE_CODE;

        // 2. try to match the language or the entire locale string among the
        // list of available language codes
        String matchedLanguageCode = null;
        for (int i = 0 ; i < VALID_LANGUAGE_CODES.length ; i++) {
            if (VALID_LANGUAGE_CODES[i].equals(localeString)) {
                // return here as this is the most precise match we can get
                return localeString;
            }

            if (VALID_LANGUAGE_CODES[i].equals(language)) {
                // set the matched language code, and continue iterating as we
                // may match the localeString in a later iteration.
                matchedLanguageCode = language;
            }
        }

        if (null != matchedLanguageCode)
            return matchedLanguageCode;

        return DEFAULT_LANGUAGE_CODE;
    }

    protected static String getApplicationVersion() {
        PackageInfo pInfo;
        String versionName = null;
        try {
            pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (NameNotFoundException e) {
            logDebug("Could not retreive version name");
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
        TelephonyManager telephonyManager = ((TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE));
        return telephonyManager.getNetworkOperatorName();
    }

    protected static String getLocaleString() {
        return String.format("%s_%s", Locale.getDefault().getLanguage()
                .toLowerCase(Locale.ENGLISH), Locale.getDefault().getCountry()
                .toUpperCase(Locale.ENGLISH));
    }

    protected static String getLibraryVersion() {
        return SDK_VERSION;
    }

    protected static int getScreenDensity(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.densityDpi;
    }

    protected static String getScreenSize(Activity activity) {
        return getScreenWidth(activity) + "x" + getScreenHeight(activity);
    }

    protected static int getScreenWidth(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.widthPixels;
    }

    protected static int getScreenHeight(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.heightPixels;
    }

    protected static boolean getBluetoothSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected static boolean getBluetoothLESupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getNFCSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected static boolean getIRSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR);
        }
        return false;
    }

    protected static boolean getTelephonySupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    protected static boolean getTelephonyGSMSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM);
    }

    protected static boolean getTelephonyCDMASupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA);
    }

    protected static boolean getWifiSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected static boolean getWifiDirectSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
        }
        return false;
    }

    protected static boolean getGPSSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    protected static boolean getNetworkLocationSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK);
    }

    protected static boolean getCameraSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getFrontCameraSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        }
        return false;
    }

    protected static boolean getMicrophoneSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    protected static boolean getSensorAccelerometerSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSensorBarometerSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
        }
        return false;
    }

    protected static boolean getSensorCompassSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSensorGyroscopeSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        }
        return false;
    }

    protected static boolean getSensorLightSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);
    }

    protected static boolean getSensorProximitySupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected static boolean getSensorStepCounterSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected static boolean getSensorStepDetectorSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSIPSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSIPVOIPSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP_VOIP);
        }
        return false;
    }

    protected static boolean getTouchscreenSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    protected static boolean getTouchscreenTwoFingersSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);
    }

    protected static boolean getTouchscreenDistinctSupported(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getTouchscreenFullHandSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    protected static boolean getUSBAccessorySupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    protected static boolean getUSBHostSupported(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
        }
        return false;
    }

    protected static void updateInstallationCoreProperties(Activity activity) {
        JSONObject properties = new JSONObject();
        try {
            JSONObject application = new JSONObject();
            application.put("version", getApplicationVersion());
            application.put("sdkVersion", SDK_VERSION);
            properties.put("application", application);

            JSONObject device = new JSONObject();
            device.put("id", getUDID());
            device.put("platform", "Android");
            device.put("osVersion", getOsVersion());
            device.put("brand", getDeviceBrand());
            device.put("model", getDeviceModel());
            device.put("name", getDeviceName());
            device.put("screenWidth", getScreenWidth(activity));
            device.put("screenHeight", getScreenHeight(activity));
            device.put("screenDensity", getScreenDensity(activity));

            JSONObject configuration = new JSONObject();
            configuration.put("timeZone", getUserTimezone());
            configuration.put("carrier", getCarrierName());
            configuration.put("locale", getLocaleString());
            device.put("configuration", configuration);

            JSONObject capabilities = new JSONObject();
            capabilities.put("bluetooth", getBluetoothSupported(activity));
            capabilities.put("bluetoothLe", getBluetoothLESupported(activity));
            capabilities.put("nfc", getNFCSupported(activity));
            capabilities.put("ir", getIRSupported(activity));
            capabilities.put("telephony", getTelephonySupported(activity));
            capabilities.put("telephonyGsm", getTelephonyGSMSupported(activity));
            capabilities.put("telephonyCdma", getTelephonyCDMASupported(activity));
            capabilities.put("wifi", getWifiSupported(activity));
            capabilities.put("wifiDirect", getWifiDirectSupported(activity));
            capabilities.put("gps", getGPSSupported(activity));
            capabilities.put("networkLocation", getNetworkLocationSupported(activity));
            capabilities.put("camera", getCameraSupported(activity));
            capabilities.put("frontCamera", getFrontCameraSupported(activity));
            capabilities.put("microphone", getMicrophoneSupported(activity));
            capabilities.put("sensorAccelerometer", getSensorAccelerometerSupported(activity));
            capabilities.put("sensorBarometer", getSensorBarometerSupported(activity));
            capabilities.put("sensorCompass", getSensorCompassSupported(activity));
            capabilities.put("sensorGyroscope", getSensorGyroscopeSupported(activity));
            capabilities.put("sensorLight", getSensorLightSupported(activity));
            capabilities.put("sensorProximity", getSensorProximitySupported(activity));
            capabilities.put("sensorStepCounter", getSensorStepCounterSupported(activity));
            capabilities.put("sensorStepDetector", getSensorStepDetectorSupported(activity));
            capabilities.put("sip", getSIPSupported(activity));
            capabilities.put("sipVoip", getSIPVOIPSupported(activity));
            capabilities.put("touchscreen", getTouchscreenSupported(activity));
            capabilities.put("touchscreenTwoFingers", getTouchscreenTwoFingersSupported(activity));
            capabilities.put("touchscreenDistinct", getTouchscreenDistinctSupported(activity));
            capabilities.put("touchscreenFullHand", getTouchscreenFullHandSupported(activity));
            capabilities.put("usbAccessory", getUSBAccessorySupported(activity));
            capabilities.put("usbHost", getUSBHostSupported(activity));
            device.put("capabilities", capabilities);

            properties.put("device", device);
        } catch (JSONException e1) {
        }

        updateInstallation(properties, false, null);
    }

    /**
     * Update the custom properties attached to the current installation object stored by WonderPush.
     *
     * <p>
     *   In order to remove a value, don't forget to use the
     *   {@link <a href="http://d.android.com/reference/org/json/JSONObject.html#NULL">JSONObject.NULL</a>}
     *   object as value.
     * </p>
     *
     * @param customProperties
     *            The partial object containing only the properties to update.
     */
    public static void putInstallationCustomProperties(JSONObject customProperties) {
        JSONObject properties = new JSONObject();
        try {
            properties.put("custom", customProperties);
        } catch (JSONException e) {
        }
        updateInstallation(properties, false, null);
    }

    static void updateInstallation(JSONObject properties, boolean overwrite, ResponseHandler handler) {
        String propertyEndpoint = "/installation";
        RequestParams parameters = new RequestParams();
        parameters.put("body", properties.toString());
        parameters.put("overwrite", overwrite ? "true" : "false");
        postEventually(propertyEndpoint, parameters, handler);
    }

    /**
     * Send an event to be tracked to WonderPush.
     * @param type
     *            The event type, or name.
     *            Event types starting with an {@code @} character are reserved.
     */
    public static void trackEvent(String type) {
        trackEvent(type, null);
    }

    /**
     * Send an event to be tracked to WonderPush.
     * @param type
     *            The event type, or name.
     *            Event types starting with an {@code @} character are reserved.
     * @param customData
     *            A JSON object containing custom properties to be attached to the event.
     *            Prefer using a few custom properties over a plethora of event type variants.
     */
    public static void trackEvent(String type, JSONObject customData) {
        if (type == null || type.length() == 0 || type.charAt(0) == '@') {
            throw new IllegalArgumentException("Bad event type");
        }
        sendEvent(type, null, customData);
    }

    protected static void trackInternalEvent(String type, JSONObject eventData) {
        trackInternalEvent(type, eventData, null);
    }

    protected static void trackInternalEvent(String type, JSONObject eventData, JSONObject customData) {
        if (type.charAt(0) != '@') {
            throw new IllegalArgumentException("This method must only be called for internal events, starting with an '@'");
        }
        sendEvent(type, eventData, customData);
    }

    private static void sendEvent(String type, JSONObject eventData, JSONObject customData) {
        String eventEndpoint = "/events/";

        JSONObject event = new JSONObject();
        if (eventData != null && eventData.length() > 0) {
            @SuppressWarnings("unchecked")
            Iterator<String> keys = eventData.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = eventData.opt(key);
                try {
                    event.putOpt(key, value);
                } catch (JSONException ex) {
                    Log.e(TAG, "Error building event object body", ex);
                }
            }
        }
        try {
            event.put("type", type);
            if (customData != null && customData.length() > 0) {
                event.put("custom", customData);
            }
            // Fill some pieces of information at the time of tracking,
            // instead of using the automatically injected location at request time,
            // which can be wrong in case of network problems
            Location location = getLocation();
            if (location != null) {
                event.put("location", "" + location.getLatitude() + "," + location.getLongitude());
            }
            event.put("creationDate", System.currentTimeMillis());
        } catch (JSONException ex) {
            Log.e(TAG, "Error building event object body", ex);
        }

        RequestParams parameters = new RequestParams();
        parameters.put("body", event.toString());
        postEventually(eventEndpoint, parameters, null);
    }

    /**
     * Whether {@link #initialize(Activity, String, String, String)} has been called.
     * Different from having fetched an access token,
     * and hence from {@link #INTENT_INTIALIZED} being dispatched.
     * @return
     */
    static boolean isInitialized() {
        return sIsInitialized;
    }

    /**
     * Initialize WonderPush. Call this method before using WonderPush.
     * A good place to initialize WonderPush is in your main activity's
     * <a href="http://developer.android.com/reference/android/app/Activity.html#onCreate(android.os.Bundle)">
     * {@code onCreate(Bundle)}</a> method as follows:
     *
     * <pre><code>protected void onCreate(Bundle savedInstance) {
     *     WonderPush.initialize(this, "clientId", "clientSecret");
     * }</code></pre>
     *
     * @param context
     *            The main activity of your application.
     *            It must be the same activity that you declared in the {@code <meta-data>} tag
     *            under the WonderPush {@code <receiver>} tag in your {@code AndroidManifest.xml}.
     * @param clientId
     *            The clientId of your application.
     * @param clientSecret
     *            The clientSecret of your application.
     * @param userId
     *            The id of the user in your application, or {@code null}.
     */
    public static void initialize(final Activity context, final String clientId, String clientSecret, final String userId) {
        setNetworkAvailable(false);
        sApplicationContext = context.getApplicationContext();
        sClientId = clientId;
        sClientSecret = clientSecret;
        sBaseURL = PRODUCTION_API_URL;
        sIsInitialized = true;

        // Permission checks
        if (context.getPackageManager().checkPermission(android.Manifest.permission.INTERNET, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing INTERNET permission. Add <uses-permission android:name=\"android.permission.INTERNET\" /> under <manifest> in your AndroidManifest.xml");
        };
        if (context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED
                && context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION permission. Add <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /> under <manifest> in your AndroidManifest.xml (you can add either or both)");
        } else if (context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Only ACCESS_COARSE_LOCATION permission is granted. For more precision, you should strongly consider adding <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /> under <manifest> in your AndroidManifest.xml");
        }

        // Initialize OpenUDID
        OpenUDID_manager.sync(sApplicationContext);
        onCreateMainActivity(context, context.getIntent());

        // Wait for UDID to be ready and fetch anonymous token if needed.
        new Runnable() {
            @Override
            public void run() {
                if (OpenUDID_manager.isInitialized()) {
                    WonderPushRestClient.setUserId(userId);
                    boolean isFetchingToken = WonderPushRestClient.fetchAnonymousAccessTokenIfNeeded(new ResponseHandler() {
                        @Override
                        public void onFailure(Throwable e, Response errorResponse) {
                        }

                        @Override
                        public void onSuccess(Response response) {
                            registerForPushNotification(context);
                            Intent broadcast = new Intent(INTENT_INTIALIZED);
                            LocalBroadcastManager.getInstance(WonderPush.getApplicationContext()).sendBroadcast(broadcast);
                            updateInstallationCoreProperties(context);
                        }
                    });
                    if (!isFetchingToken) {
                        // even if we have an access token, we need to ensure
                        // connectivity
                        // state
                        WonderPush.get("/network/ping", null, new ResponseHandler() {
                            @Override
                            public void onFailure(Throwable e, Response errorResponse) {
                            }

                            @Override
                            public void onSuccess(Response response) {
                                registerForPushNotification(context);
                                Intent broadcast = new Intent(INTENT_INTIALIZED);
                                LocalBroadcastManager.getInstance(WonderPush.getApplicationContext())
                                        .sendBroadcast(broadcast);
                                updateInstallationCoreProperties(context);
                            }
                        });
                    }
                } else {
                    new Handler().postDelayed(this, 100);
                }
            }
        }.run();

        WonderPushRequestVault.initialize();
    }

    /**
     * Method to be called in your own Google Cloud Messaging
     * <a href="http://developer.android.com/reference/android/content/BroadcastReceiver.html"><tt>BroadcastReceiver</tt></a>
     * to handle WonderPush push notifications.
     *
     * <p>
     *   <b>Note:</b> There is no need to call this method if you use {@link WonderPushBroadcastReceiver},
     *   as advertised in <a href="../../../packages.html#installing-sdk--configuring-sdk">the guide</a>.
     * </p>
     *
     * <p>
     *   Implement your <a href="http://developer.android.com/reference/android/content/BroadcastReceiver.html#onReceive(android.content.Context, android.content.Intent)"><tt>BroadcastReceiver.onReceive(Context, Intent)</tt></a>
     *   method as follows:
     * </p>
     * <pre><code>public void onReceive(Context context, Intent intent) {
     *     if (WonderPush.onBroadcastReceived(context, intent, R.drawable.icon, YourMainActivity.class)) {
     *         return;
     *     }
     *     // Do your own handling here
     * }</code></pre>
     *
     * <p>
     *   For more information about Google Cloud Messaging visit:
     *   <a href="http://developer.android.com/google/gcm/index.html">http://developer.android.com/google/gcm/index.html</a>.
     * </p>
     *
     * @param context
     *            The current context.
     * @param intent
     *            The received intent.
     * @param iconResource
     *            The icon you want to show in the notification.
     * @param activityClass
     *            The activity class you want to start when the user touches the notification
     * @return {@code true} if handled, {@code false} otherwise.
     */
    public static boolean onBroadcastReceived(Context context, Intent intent, int iconResource, Class<? extends Activity> activityClass) {
        return WonderPushGcmClient.onBroadcastReceived(context, intent, iconResource, activityClass);
    }

    /**
     * Gets the application context that was captured during the
     * {@link WonderPush#initialize(Activity, String, String} call.
     */
    protected static Context getApplicationContext() {
        if (null == sApplicationContext)
            Log.e(TAG, "Application context is null, did you call WonderPush.initialize ?");
        return sApplicationContext;
    }

    /**
     * Gets the WonderPush shared preferences for that application.
     */
    protected static SharedPreferences getSharedPreferences() {
        if (null == getApplicationContext())
            return null;
        return getApplicationContext().getSharedPreferences(PREF_FILE, 0);
    }

    protected static SharedPreferences getSharedPreferences(Context c) {
        if (null == c)
            throw new IllegalArgumentException("Context must be set");

        return c.getSharedPreferences(PREF_FILE, 0);
    }

    protected static WonderPushDialogBuilder createDialogNotificationBase(final Activity activity, final JSONObject data) {
        WonderPushDialogBuilder builder = new WonderPushDialogBuilder(activity, data, new WonderPushDialogBuilder.OnChoice() {
            @Override
            public void onChoice(WonderPushDialogBuilder dialog, Button which) {
                handleDialogButtonAction(dialog, which);
            }
        });
        builder.setupTitleAndIcon();

        return builder;
    }

    protected static void createDefaultCloseButtonIfNeeded(WonderPushDialogBuilder builder) {
        List<WonderPushDialogBuilder.Button> buttons = builder.getButtons();
        if (buttons.isEmpty()) {
            Button defaultButton = new Button();
            defaultButton.label = builder.getActivity().getResources().getString(R.string.wonderpush_close);
            buttons.add(defaultButton);
        }
    }

    protected static void handleDialogButtonAction(WonderPushDialogBuilder dialog, WonderPushDialogBuilder.Button buttonClicked) {
        if (buttonClicked == null) {
            logDebug("User cancelled the dialog");
            return;
        }
        for (WonderPushDialogBuilder.Button.Action action : buttonClicked.actions) {
            switch (action.getType()) {
                case CLOSE:
                    // Noop
                    break;
                case MAP_OPEN:
                    handleMapOpenButtonAction(dialog, buttonClicked);
                    break;
                default:
                    Log.w(TAG, "TODO: build-in button action " + action.getType());
                    break;
            }
        }
    }

    protected static void handleDialogNotification(final Activity activity, final JSONObject data) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(activity, data);

        String text = data.optString("text");
        if (text == null) {
            Log.w(TAG, "Got no text to display for a plain notification");
        } else {
            builder.setMessage(text);
        }

        createDefaultCloseButtonIfNeeded(builder);
        builder.setupButtons();

        builder.show();
    }

    @SuppressLint("InflateParams")
    protected static void handleMapNotification(final Activity activity, final JSONObject data) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(activity, data);

        final JSONObject place;
        try {
            place = data.getJSONObject("map").getJSONObject("place");
        } catch (JSONException e) {
            Log.e(TAG, "Could not get the place from the map", e);
            return;
        }
        final JSONObject point = place.optJSONObject("point");
        final View dialogView = activity.getLayoutInflater().inflate(R.layout.wonderpush_notification_map_dialog, null, false);
        final TextView text = (TextView) dialogView.findViewById(R.id.wonderpush_notification_map_dialog_text);
        if (data.has("text")) {
            text.setVisibility(View.VISIBLE);
            text.setText(data.optString("text"));
            text.setMovementMethod(new ScrollingMovementMethod());
        }
        final ImageView map = (ImageView) dialogView.findViewById(R.id.wonderpush_notification_map_dialog_map);
        builder.setView(dialogView);

        List<Button> buttons = builder.getButtons();
        if (buttons.isEmpty()) {
            // Close button
            Button closeButton = new Button();
            closeButton.label = activity.getResources().getString(R.string.wonderpush_close);
            Action closeAction = new Action();
            closeAction.setType(Action.Type.CLOSE);
            closeButton.actions.add(closeAction);
            buttons.add(closeButton);
            // Open button
            Button openButton = new Button();
            openButton.label = activity.getResources().getString(R.string.wonderpush_open);
            Action openAction = new Action();
            openAction.setType(Action.Type.MAP_OPEN);
            openButton.actions.add(openAction);
            buttons.add(openButton);
        }
        builder.setupButtons();

        builder.show();

        new AsyncTask<Object, Object, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object... args) {
                try {
                    String loc = null;
                    if (point != null && point.has("lat") && point.has("lon")) {
                        loc = point.optDouble("lat", 0.0) + "," + point.optDouble("lon", 0.0);
                    } else {
                        loc = place.optString("name", place.optString("query"));
                    }
                    int screenWidth = getScreenWidth(activity);
                    int screenHeight = getScreenHeight(activity);
                    double ratio = screenWidth / (double)screenHeight;
                    int width = ratio >= 1 ? Math.min(640, screenWidth) : (int)Math.floor(ratio * Math.min(640, screenHeight));
                    int height = ratio <= 1 ? Math.min(640, screenHeight) : (int)Math.floor(Math.min(640, screenWidth) / ratio);
                    String size = width + "x" + height;
                    int scale = getScreenDensity(activity) >= 192 ? 2 : 1;
                    URL url = new URL("https://maps.google.com/maps/api/staticmap"
                            + "?center=" + loc
                            + "&zoom=" + place.optInt("zoom", 13)
                            + "&size=" + size
                            + "&sensors=false"
                            + "&markers=color:red%7C" + loc
                            + "&scale=" + scale
                            + "&language=" + getLang()
                    );
                    return BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Malformed map URL", e);
                } catch (IOException e) {
                    Log.e(TAG, "Could not load map image", e);
                }
                return null;
            }
            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (bmp == null) {
                    map.setVisibility(View.GONE);
                    text.setMaxLines(Integer.MAX_VALUE);
                } else {
                    map.setScaleType(ScaleType.CENTER_CROP);
                    map.setImageBitmap(bmp);
                }
            }
        }.execute();
    }

    protected static void handleMapOpenButtonAction(WonderPushDialogBuilder dialog, Button button) {
        Activity activity = dialog.getActivity();
        JSONObject data = dialog.getData();
        try {
            JSONObject place;
            try {
                place = data.getJSONObject("map").getJSONObject("place");
            } catch (JSONException e) {
                Log.e(TAG, "Could not get the place from the map", e);
                return;
            }
            JSONObject point = place.optJSONObject("point");

            Uri.Builder geo = new Uri.Builder();
            geo.scheme("geo");
            if (point != null && point.has("lat") && point.has("lon")) {
                if (place.has("name")) {
                    geo.authority("0,0");
                    geo.appendQueryParameter("q", point.getDouble("lat") + "," + point.getDouble("lon") + "(" + place.getString("name") + ")");
                } else {
                    geo.authority(point.getDouble("lat") + "," + point.getDouble("lon"));
                    if (place.has("zoom")) {
                        geo.appendQueryParameter("z", Integer.toString(place.getInt("zoom")));
                    }
                }
            } else if (place.has("query")) {
                geo.authority("0,0");
                geo.appendQueryParameter("q", place.getString("query"));
            }
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setData(geo.build());
            if (open.resolveActivity(activity.getPackageManager()) != null) {
                logDebug("Will open location " + open.getDataString());
                activity.startActivity(open);
            } else {
                Log.d(TAG, "No activity can open location " + open.getDataString());
                Log.d(TAG, "Falling back to regular URL");
                geo = new Uri.Builder();
                geo.scheme("http");
                geo.authority("maps.google.com");
                geo.path("maps");
                if (point.has("lat") && point.has("lon")) {
                    geo.appendQueryParameter("q", point.getDouble("lat") + "," + point.getDouble("lon"));
                    if (place.has("zoom")) {
                        geo.appendQueryParameter("z", Integer.toString(place.getInt("zoom")));
                    }
                } else if (place.has("query")) {
                    geo.appendQueryParameter("q", place.getString("query"));
                } else if (place.has("name")) {
                    geo.appendQueryParameter("q", place.getString("name"));
                }
                open = new Intent(Intent.ACTION_VIEW);
                open.setData(geo.build());
                if (open.resolveActivity(activity.getPackageManager()) != null) {
                    logDebug("Opening URL " + open.getDataString());
                    activity.startActivity(open);
                } else {
                    Log.d(TAG, "No activity can open URL " + open.getDataString());
                    Log.w(TAG, "Cannot open map!");
                    Toast.makeText(activity, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to open map", e);
            Toast.makeText(activity, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
        }
    }

    protected static WonderPushDialogBuilder createWebNotificationBasePre(final Activity activity, final JSONObject data, WonderPushView webView) {
        final WonderPushDialogBuilder builder = createDialogNotificationBase(activity, data);
        builder.setupButtons();

        JSONArray buttons = data.optJSONArray("buttons");
        webView.setShowCloseButton(buttons == null || buttons.length() < 1);

        webView.setStateListener(new WonderPushView.OnStateListener() {
            @Override
            public void onLoading() {
                // Handled by WonderPushView itself
            }
            @Override
            public void onLoaded() {
                // Handled by WonderPushView itself
            }
            @Override
            public void onError() {
                // Handled by WonderPushView itself
            }
            @Override
            public void onClose() {
                builder.dismiss();
            }
        });

        builder.setView(webView);

        return builder;
    }

    protected static void handleHTMLNotification(final Activity activity, final JSONObject data) {
        if (!data.has("message")) {
            Log.w(TAG, "Did not have any HTML content to display in the notification!");
            return;
        }

        // Build the dialog
        WonderPushView webView = new WonderPushView(activity);
        final WonderPushDialogBuilder builder = createWebNotificationBasePre(activity, data, webView);
        builder.setupButtons();

        // Set content
        webView.loadDataWithBaseURL(data.optString("baseUrl"), data.optString("message"), "text/html", "utf-8", null);

        // Show time!
        builder.show();
    }

    protected static void handleURLNotification(Activity activity, JSONObject data) {
        if (!data.has("url")) {
            Log.w(TAG, "Did not have any URL to display in the notification!");
            return;
        }

        WonderPushView webView = new WonderPushView(activity);
        final WonderPushDialogBuilder builder = createWebNotificationBasePre(activity, data, webView);
        builder.setupButtons();

        // Set content
        webView.setFullUrl(data.optString("url"));

        // Show time!
        builder.show();
    }

    /**
     * A class that handles the parameter to provide to either an api call or a view.
     */
    protected static class RequestParams extends com.loopj.android.http.RequestParams implements Parcelable {

        public static final String TAG = "RequestParams";

        public RequestParams(Parcel in) throws JSONException {
            JSONObject json = new JSONObject(in.readString());
            Iterator<?> it = json.keys();
            String key;
            while (it.hasNext()) {
                key = (String) it.next();
                this.put(key, json.optString(key));
            }
        }

        /**
         * Constructs a new empty <code>RequestParams</code> instance.
         */
        public RequestParams() {
            super();
        }

        /**
         * Constructs a new RequestParams instance containing the key/value
         * string params from the specified map.
         *
         * @param source
         *            The source key/value string map to add.
         */
        public RequestParams(Map<String, String> source) {
            super(source);
        }

        /**
         * Constructs a new RequestParams instance and populate it with multiple
         * initial key/value string param.
         *
         * @param keysAndValues
         *            A sequence of keys and values. Objects are automatically
         *            converted to Strings (including the value {@code null}).
         * @throws IllegalArgumentException
         *            If the number of arguments isn't even.
         */
        public RequestParams(Object... keysAndValues) {
            super(keysAndValues);
        }

        /**
         * Constructs a new RequestParams instance and populate it with a single
         * initial key/value string param.
         *
         * @param key
         *            The key name for the intial param.
         * @param value
         *            The value string for the initial param.
         */
        public RequestParams(String key, String value) {
            super(key, value);
        }

        /**
         * Return the names of all parameters.
         */
        public Set<String> getParamNames() {
            HashSet<String> result = new HashSet<String>();
            result.addAll(this.fileParams.keySet());
            result.addAll(this.urlParams.keySet());
            result.addAll(this.urlParamsWithArray.keySet());
            return result;
        }

        /**
         * Returns the value for the given param. If the given param is
         * encountered multiple times, the first occurrence is returned.
         *
         * @param paramName
         */
        public String getParamValue(String paramName) {

            if (this.urlParams.containsKey(paramName))
                return this.urlParams.get(paramName);

            if (this.urlParamsWithArray.containsKey(paramName)) {
                List<String> values = this.urlParamsWithArray.get(paramName);
                if (0 < values.size())
                    return values.get(0);
            }

            return null;
        }

        /**
         * Checks whether the provided key has been specified as parameter.
         *
         * @param key
         */
        public boolean has(String key) {
            return urlParams.containsKey(key)
                    || fileParams.containsKey(key)
                    || urlParamsWithArray.containsKey(key);
        }

        public String getURLEncodedString() {
            return getParamString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public JSONObject toJSONObject() {
            JSONObject result = new JSONObject();
            java.util.List<org.apache.http.message.BasicNameValuePair> params = getParamsList();
            for (org.apache.http.message.BasicNameValuePair parameter : params) {
                try {
                    result.put(parameter.getName(), parameter.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            destination.writeString(toJSONObject().toString());
        }

        public static final Parcelable.Creator<WonderPush.RequestParams> CREATOR = new Parcelable.Creator<WonderPush.RequestParams>() {
            public RequestParams createFromParcel(Parcel in) {
                try {
                    return new RequestParams(in);
                } catch (JSONException e) {
                    Log.e(TAG, "Error while unserializing JSON from a WonderPush.RequestParams", e);
                    return null;
                }
            }

            public RequestParams[] newArray(int size) {
                return new RequestParams[size];
            }
        };
    }

    /**
     * An HTTP response object
     */
    protected static class Response {

        JSONObject mJson;

        public Response(String responseContent) {
            try {
                mJson = new JSONObject(responseContent);
            } catch (JSONException e) {
                Log.e(TAG, "Invalid JSON in response content: " + responseContent, e);
            }
        }

        public Response(JSONObject responseJson) {
            mJson = responseJson;
        }

        public boolean isError() {
            return mJson.has("error");
        }

        public String getErrorMessage() {
            if (!isError())
                return null;

            return mJson.optJSONObject("error").optString("message");
        }

        public int getErrorStatus() {
            if (!isError())
                return 0;

            return mJson.optJSONObject("error").optInt("status");
        }

        public int getErrorCode() {
            if (!isError())
                return 0;

            return mJson.optJSONObject("error").optInt("code");
        }

        public JSONObject getJSONObject() {
            return mJson;
        }
    }

    /**
     * HTTP response handler.
     */
    protected static abstract class ResponseHandler {

        /**
         * Called when the request failed. Default implementation is empty.
         *
         * @param e
         * @param errorResponse
         */
        public abstract void onFailure(Throwable e, Response errorResponse);

        /**
         * Called on request success. Default implementation is empty.
         *
         * @param response
         */
        public abstract void onSuccess(Response response);

        /**
         * Called on request success. Default implementation calls {@link #onSuccess(Response)}.
         *
         * @param response
         */
        public void onSuccess(int statusCode, Response response) {
            onSuccess(response);
        }

    }

}
