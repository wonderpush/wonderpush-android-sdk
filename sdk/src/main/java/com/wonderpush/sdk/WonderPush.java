package com.wonderpush.sdk;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.OpenUDID.OpenUDID_manager;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import cz.msebera.android.httpclient.message.BasicNameValuePair;

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
 * <p>You must call {@link #initialize(Context)} before using any other function.</p>
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
    protected static boolean SHOW_DEBUG = false;

    private static Context sApplicationContext;
    private static Application sApplication;
    private static ActivityLifecycleMonitor sActivityLifecycleCallbacks;
    private static boolean sActivityLifecycleCallbacksRegistered;
    private static WeakReference<Intent> sLastHandledIntentRef;
    private static WeakHashMap<Activity, Object> sTrackedActivities = new WeakHashMap<>();
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            sActivityLifecycleCallbacks = new ActivityLifecycleMonitor();
        }
    }

    private static Looper sLooper;
    private static Handler sDeferHandler;
    private static ScheduledExecutorService sScheduledExecutor;
    static {
        sDeferHandler = new Handler(Looper.getMainLooper()); // temporary value until our thread is started
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                sLooper = Looper.myLooper();
                sDeferHandler = new Handler(sLooper);
                Looper.loop();
            }
        }, "WonderPush").start();
        sScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private static String sClientId;
    private static String sClientSecret;
    private static Location sLocation;
    private static String sBaseURL;
    private static String sLang;
    private static boolean sIsInitialized = false;
    private static boolean sIsReady = false;
    private static boolean sIsReachable = false;

    private static boolean sBeforeInitializationUserIdSet = false;
    private static String sBeforeInitializationUserId;

    /**
     * The timeout for WebView requests
     */
    protected static final int WEBVIEW_REQUEST_TOTAL_TIMEOUT = 10000;
    protected static final int API_INT = 1; // reset SDK_VERSION when bumping this
    protected static final String API_VERSION = "v" + API_INT;
    protected static final String SDK_SHORT_VERSION = "2.1.1-SNAPSHOT"; // reset to .1.0.0 when bumping API_INT
    protected static final String SDK_VERSION = "Android-" + API_INT + "." + SDK_SHORT_VERSION;
    protected static final int ERROR_INVALID_SID = 12017;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static long startupDateToServerDateOffset = 0;
    private static long startupDateToServerDateUncertainty = Long.MAX_VALUE;
    private static long deviceDateToServerDateOffset = 0;
    private static long deviceDateToServerDateUncertainty = Long.MAX_VALUE;
    private static long startupDateToDeviceDateOffset = Long.MAX_VALUE;

    private static ScheduledFuture<Void> putInstallationCustomPropertiesDelayedTask;

    /**
     * How long in ms should two interactions should be separated in time,
     * to be considered as belonging to two different sessions.
     */
    private static final long DIFFERENT_SESSION_REGULAR_MIN_TIME_GAP = 30 * 60 * 1000;

    /**
     * How long in ms should have elapsed from last interaction,
     * to consider the opening of a notification as starting a new session.
     * This should be a lower threshold than {@link #DIFFERENT_SESSION_REGULAR_MIN_TIME_GAP},
     * as a notification creates a more urgent need to reopen the application.
     */
    private static final long DIFFERENT_SESSION_NOTIFICATION_MIN_TIME_GAP = 15 * 60 * 1000;

    /**
     * How long in ms to skip updating the installation core properties if they did not change.
     */
    private static final long CACHED_INSTALLATION_CORE_PROPERTIES_DURATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * How long to wait for no other call to {@link #putInstallationCustomProperties(JSONObject)}
     * before writing changes to the server.
     */
    private static final long CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY = 5 * 1000;

    /**
     * How long to wait for another call to {@link #putInstallationCustomProperties(JSONObject)} at maximum,
     * if there are no pause of {@link #CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY} time between calls.
     */
    private static final long CACHED_INSTALLATION_CUSTOM_PROPERTIES_MAX_DELAY = 20 * 1000;

    /**
     * How long in ms to skip updating the registration id if it did not change.
     */
    protected static final long CACHED_REGISTRATION_ID_DURATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * The metadata key name corresponding to the name of the WonderPushInitializer implementation.
     */
    private static final String METADATA_INITIALIZER_CLASS = "wonderpushInitializerClass";

    /**
     * The preference.subscriptionStatus value when notifications are enabled.
     */
    private static final String INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTIN = "optIn";

    /**
     * The preference.subscriptionStatus value when notifications are disabled.
     */
    private static final String INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTOUT = "optOut";

    /**
     * Local intent broadcasted when the WonderPush SDK has been initialized and network is reachable.
     */
    public static final String INTENT_INTIALIZED = "wonderpushInitialized";

    /**
     * Local intent broadcasted when a push notification created by the WonderPush SDK has been opened.
     */
    public static final String INTENT_NOTIFICATION_OPENED = "wonderpushNotificationOpened";

    /**
     * The extra key for the original received push notification intent in a {@link #INTENT_NOTIFICATION_OPENED} intent.
     */
    public static final String INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION = "wonderpushReceivedPushNotification";

    /**
     * The extra key for whether the user clicked the notification or it was automatically opened by the SDK
     * in a {@link #INTENT_NOTIFICATION_OPENED} intent.
     */
    public static final String INTENT_NOTIFICATION_OPENED_EXTRA_FROM_USER_INTERACTION = "wonderpushFromUserInteraction";

    /**
     * Local intent broadcasted when a push notification created by the WonderPush SDK is to be opened,
     * but no activity is to be started.
     * This let's you handle {@code data} notifications or any deep linking yourself.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN = "wonderpushNotificationWillOpen";

    /**
     * The scheme for the WonderPushService intents.
     */
    protected static final String INTENT_NOTIFICATION_WILL_OPEN_SCHEME = "wonderpush";

    /**
     * The authority for handling notification opens with deep links calling the WonderPushService.
     */
    protected static final String INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY = "notificationOpen";

    /**
     * The first path segment for opening the notification in the default way.
     */
    protected static final String INTENT_NOTIFICATION_WILL_OPEN_PATH_DEFAULT = "default";

    /**
     * The first path segment for broadcasting the "notification will open" event for a programmatic resolution.
     */
    protected static final String INTENT_NOTIFICATION_WILL_OPEN_PATH_BROADCAST = "broadcast";

    /**
     * The extra key for the original received push notification intent in a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION =
            INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION;

    /**
     * The extra key for whether the user clicked the notification or it was automatically opened by the SDK
     * in a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION =
            INTENT_NOTIFICATION_OPENED_EXTRA_FROM_USER_INTERACTION;

    /**
     * The extra key denoting whether to automatically display a rich notification message in a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     * You can set this property to {@code false} in your BroadcastReceiver.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN = "wonderpushAutomaticOpen";

    /**
     * The extra key denoting the received push notification type, for a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     * You can test this property against {@link #INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE_DATA} in your BroadcastReceiver.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE = "wonderpushNotificationType";

    /**
     * The value associated to data push notifications (aka silent notifications), corresponding to the extra key
     * {@link #INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE}.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE_DATA = "data";

    /**
     * Local intent broadcasted when a resource has been successfully preloaded.
     */
    protected static final String INTENT_RESOURCE_PRELOADED = "wonderpushResourcePreloaded";

    /**
     * The extra key for the path of a preloaded resource in a {@link #INTENT_RESOURCE_PRELOADED} intent.
     */
    protected static final String INTENT_RESOURCE_PRELOADED_EXTRA_PATH = "wonderpushResourcePreloadedPath";

    /**
     * Intent scheme for GCM notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_SCHEME = "wonderpush";

    /**
     * Intent authority for GCM notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_AUTHORITY = "notification";

    /**
     * Intent data type for GCM notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_TYPE = "application/vnd.wonderpush.notification";

    /**
     * Intent query parameter key for GCM notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_QUERY_PARAMETER = "body";

    /**
     * Intent action for notification button action `method`.
     */
    public static final String INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_ACTION = "com.wonderpush.action.method";

    /**
     * Intent scheme for notification button action `method`.
     */
    public static final String INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_SCHEME = "wonderpush";

    /**
     * Intent authority for notification button action `method`.
     */
    public static final String INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_AUTHORITY = "action.method";

    /**
     * Intent query parameter key for the notification button action `method` method name.
     */
    public static final String INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_METHOD = "com.wonderpush.action.method.extra_method";

    /**
     * Intent query parameter key for the notification button action `method` argument.
     */
    public static final String INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_ARG = "com.wonderpush.action.method.extra_arg";

    private static final String PRODUCTION_API_URL = "https://api.wonderpush.com/" + API_VERSION;
    protected static final int ERROR_INVALID_CREDENTIALS = 11000;
    protected static final int ERROR_INVALID_ACCESS_TOKEN = 11003;
    protected static final String DEFAULT_LANGUAGE_CODE = "en";
    protected static final String[] VALID_LANGUAGE_CODES = {
            "af", "ar", "be", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "en_GB", "en_US", "es", "es_ES", "es_MX",
            "et", "fa", "fi", "fr", "fr_FR", "fr_CA", "he", "hi", "hr", "hu", "id", "is", "it", "ja", "ko", "lt", "lv",
            "mk", "ms", "nb", "nl", "pa", "pl", "pt", "pt_PT", "pt_BR", "ro", "ru", "sk", "sl", "sq", "sr", "sv", "sw",
            "ta", "th", "tl", "tr", "uk", "vi", "zh", "zh_CN", "zh_TW", "zh_HK",
    };

    protected WonderPush() {
        throw new IllegalAccessError("You should not instantiate this class!");
    }

    private static boolean checkPlayService(Context context) {
        try {
            Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
            if (resultCode != ConnectionResult.SUCCESS) {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    if (context instanceof Activity) {
                        googleApiAvailability.getErrorDialog((Activity) context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                    } else {
                        googleApiAvailability.showErrorNotification(context, resultCode);
                    }
                } else {
                    Log.w(TAG, "This device does not support Google Play Services, push notification are not supported");
                }
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "The Google Play Services have not been added to the application", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while checking the Google Play Services", e);
        }
        return false;
    }

    /**
     * Helper method that will register a device for google cloud messages
     * notification and register the device token to WonderPush. This method is
     * called within {@link #initialize(Context, String, String)}.
     *
     * @param context
     *            The current {@link Activity} (preferred) or {@link Application} context.
     */
    protected static void registerForPushNotification(Context context) {
        if (checkPlayService(context)) {
            WonderPushGcmClient.registerForPushNotification(context);
        } else {
            Log.w(TAG, "Google Play Services not present. Check your setup. If on an emulator, use a Google APIs system image.");
        }
    }

    /**
     * Whether to enable debug logging.
     *
     * You should not do this in production builds.
     *
     * @param enable {@code true} to enable debug logs.
     */
    public static void setLogging(boolean enable) {
        WonderPush.SHOW_DEBUG = enable;
    }

    protected static void logDebug(String debug) {
        if (WonderPush.SHOW_DEBUG) {
            Log.d(TAG, debug);
        }
    }

    protected static void logDebug(String debug, Throwable tr) {
        if (WonderPush.SHOW_DEBUG) {
            Log.d(TAG, debug, tr);
        }
    }

    protected static void logError(String msg) {
        if (WonderPush.SHOW_DEBUG) {
            Log.e(TAG, msg);
        }
    }

    protected static void logError(String msg, Throwable tr) {
        if (WonderPush.SHOW_DEBUG) {
            Log.e(TAG, msg, tr);
        }
    }

    private static void handleReceivedNotification(Context context, NotificationModel notif) {
        try {
            for (ActionModel action : notif.getActions()) {
                try {
                    if (action == null || action.getType() == null) {
                        // Skip unrecognized action types
                        continue;
                    }
                    switch (action.getType()) {
                        case CLOSE:
                            // Noop
                            break;
                        case LINK:
                            handleLinkAction(context, action);
                            break;
                        case RATING:
                            handleRatingAction(context, action);
                            break;
                        case TRACK_EVENT:
                            handleTrackEventAction(action);
                            break;
                        case UPDATE_INSTALLATION:
                            handleUpdateInstallationAction(action);
                            break;
                        case METHOD:
                            handleMethodAction(action);
                            break;
                        default:
                            Log.w(TAG, "Unhandled opened notification action \"" + action.getType() + "\"");
                            break;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while handling opened notification action " + action, ex);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling opened notification actions", ex);
        }

        try {
            switch (notif.getType()) {
                case SIMPLE:
                case DATA:
                    // Nothing to do
                    break;
                case URL:
                    handleURLNotification(context, (NotificationUrlModel) notif);
                    break;
                case TEXT:
                    handleTextNotification(context, (NotificationTextModel) notif);
                    break;
                case MAP:
                    handleMapNotification(context, (NotificationMapModel) notif);
                    break;
                case HTML:
                    handleHTMLNotification(context, (NotificationHtmlModel) notif);
                    break;
                default:
                    Log.e(TAG, "Missing built-in action for type " + notif.getType());
                    break;
            }
        } catch (ClassCastException ex) {
            Log.e(TAG, "Wrong notification class for type " + notif.getType(), ex);
        }
    }

    protected static boolean containsExplicitNotification(Intent intent) {
        return  intent != null
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
                && (sLastHandledIntentRef == null || intent != sLastHandledIntentRef.get())
                && INTENT_NOTIFICATION_TYPE.equals(intent.getType())
                && intent.getData() != null
                && INTENT_NOTIFICATION_SCHEME.equals(intent.getData().getScheme())
                && INTENT_NOTIFICATION_AUTHORITY.equals(intent.getData().getAuthority())
                ;
    }

    protected static boolean containsWillOpenNotification(Intent intent) {
        return  intent != null
                // action may or may not be INTENT_NOTIFICATION_WILL_OPEN
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
                && (sLastHandledIntentRef == null || intent != sLastHandledIntentRef.get())
                && intent.hasExtra(INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION)
                ;
    }

    protected static boolean containsWillOpenNotificationAutomaticallyOpenable(Intent intent) {
        return  containsWillOpenNotification(intent)
                && intent.hasExtra(INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN) // makes it default to false if removed
                && intent.getBooleanExtra(INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN, false)
                ;
    }

    /**
     * Method to call on your {@code onNewIntent()} and {@code onCreate()} methods to handle the WonderPush notification.
     *
     * <p>Starting from API 14, there is no need to call this method, but it won't hurt if you do.</p>
     *
     * <p>
     *   This method is automatically called from within {@link WonderPush#initialize(Context)},
     *   so calling this method after calling {@link WonderPush#initialize(Context)} is useless.
     * </p>
     *
     * <p>Example:</p>
     * <pre>
     * <code>
     * &#64;Override
     * protected void onCreate(Bundle savedInstance) {
     *     // In case you call WonderPush.initialize() from your custom Application class,
     *     // and you target API < 14, you can either call WonderPush.initialize() once again here
     *     // or call this method instead.
     *     WonderPush.showPotentialNotification(this, getIntent());
     * }
     *
     * &#64;Override
     * protected void onNewIntent(Intent intent) {
     *     WonderPush.showPotentialNotification(this, intent);
     * }
     * </code>
     * </pre>
     *
     * @param activity
     *            The current {@link Activity}.
     *            Just give {@code this}.
     * @param intent
     *            The intent the activity received.
     *            Just give the {@code intent} you received in parameter, or give {@code getIntent()}.
     *
     * @return <code>true</code> if handled, <code>false</code> otherwise.
     */
    public static boolean showPotentialNotification(final Activity activity, Intent intent) {
        try {
            if (containsExplicitNotification(intent) || containsWillOpenNotificationAutomaticallyOpenable(intent)) {
                final NotificationModel notif = NotificationModel.fromLocalIntent(intent);
                if (notif == null) {
                    Log.e(TAG, "Failed to extract notification object");
                    return false;
                }
                if (containsExplicitNotification(intent)) {
                    intent.setDataAndType(null, null);
                } else if (containsWillOpenNotification(intent)) {
                    // Keep it
                    //intent.removeExtra(INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION);
                }
                sLastHandledIntentRef = new WeakReference<>(intent);
                logDebug("Handling opened notification: " + notif.getInputJSONString());
                try {
                    JSONObject trackData = new JSONObject();
                    trackData.put("campaignId", notif.getCampaignId());
                    trackData.put("notificationId", notif.getNotificationId());
                    trackData.put("actionDate", getTime());
                    WonderPush.trackInternalEvent("@NOTIFICATION_OPENED", trackData);

                    WonderPushConfiguration.setLastOpenedNotificationInfoJson(trackData);

                    // Notify the application that the notification has been opened
                    Intent notificationOpenedIntent = new Intent(INTENT_NOTIFICATION_OPENED);
                    boolean fromUserInteraction = intent.getBooleanExtra("fromUserInteraction", true);
                    notificationOpenedIntent.putExtra(INTENT_NOTIFICATION_OPENED_EXTRA_FROM_USER_INTERACTION, fromUserInteraction);
                    Intent receivedPushNotificationIntent = intent.getParcelableExtra("receivedPushNotificationIntent");
                    notificationOpenedIntent.putExtra(INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION, receivedPushNotificationIntent);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationOpenedIntent);

                    if (sIsInitialized) {
                        handleReceivedNotification(activity, notif);
                    } else {
                        BroadcastReceiver receiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                try {
                                    handleReceivedNotification(context, notif);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Unexpected error on deferred handling of received notification", ex);
                                }
                                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
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
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while showing potential notification", e);
        }
        return false;
    }

    /**
     * Gets the clientId that was specified during the {@link #initialize(Context, String, String)} call.
     */
    protected static String getClientId() {
        return sClientId;
    }

    /**
     * Gets the clientSecret that was specified during the {@link #initialize(Context, String, String)} call.
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

    protected static boolean isUDIDReady() {
        return OpenUDID_manager.isInitialized();
    }

    /**
     * Returns the UDID determined by OpenUDID.
     *
     * @return The UDID determined by OpenUDID or null if OpenUDID is not initialized.
     */
    protected static String getUDID() {
        if (!isUDIDReady()) {
            Log.w(TAG, "Reading UDID before it is ready!");
            return null;
        }
        return OpenUDID_manager.getOpenUDID();
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
     * @param responseHandler
     *            An AsyncHttpClient response handler.
     */
    protected static void delete(String resource,
            WonderPush.ResponseHandler responseHandler) {
        WonderPushRestClient.delete(resource, responseHandler);
    }

    /**
     * Returns the last known location of the {@link LocationManager}
     * or null if permission was not given.
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

                // If no, broken or poor accuracy, discard
                if (location.getAccuracy() <= 0 || location.getAccuracy() >= 10000) {
                    continue;
                }

                // Skip locations older than 5 minutes
                if (location.getTime() < System.currentTimeMillis() - 5 * 60 * 1000) {
                    continue;
                }

                // If we have no best yet, use this first location
                if (null == best) {
                    best = location;
                    continue;
                }

                // If this location is more than 2 minutes older than the current best, discard
                if (location.getTime() < best.getTime() - 2 * 60 * 1000) {
                    continue;
                }

                // If this location is less precise (ie. has a *larger* accuracy radius), discard
                if (location.getAccuracy() > best.getAccuracy()) {
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
     * Gets the current language, guessed from the system.
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
        for (String languageCode : VALID_LANGUAGE_CODES) {
            if (languageCode.equals(localeString)) {
                // return here as this is the most precise match we can get
                return localeString;
            }

            if (languageCode.equals(language)) {
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
        String versionName = null;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            logDebug("Could not retreive version name");
        }
        return versionName;
    }

    protected static int getApplicationVersionCode() {
        int versionCode = -1;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            logDebug("Could not retreive version code");
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
        TelephonyManager telephonyManager = ((TelephonyManager) getApplicationContext()
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

    protected static String getLibraryVersion() {
        return SDK_VERSION;
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getNFCSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
        }
        return false;
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

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected static boolean getWifiDirectSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
        }
        return false;
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getFrontCameraSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        }
        return false;
    }

    protected static boolean getMicrophoneSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    protected static boolean getSensorAccelerometerSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSensorBarometerSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
        }
        return false;
    }

    protected static boolean getSensorCompassSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSensorGyroscopeSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        }
        return false;
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSIPSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getSIPVOIPSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SIP_VOIP);
        }
        return false;
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected static boolean getTouchscreenFullHandSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    protected static boolean getUSBAccessorySupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    protected static boolean getUSBHostSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
        }
        return false;
    }

    protected static void updateInstallationCoreProperties(Context context) {
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

            String propertiesString = properties.toString();
            String cachedPropertiesString = WonderPushConfiguration.getCachedInstallationCoreProperties();
            long cachedPropertiesDate = WonderPushConfiguration.getCachedInstallationCorePropertiesDate();
            if (System.currentTimeMillis() - cachedPropertiesDate > CACHED_INSTALLATION_CORE_PROPERTIES_DURATION
                    || !propertiesString.equals(cachedPropertiesString)) {
                WonderPushConfiguration.setCachedInstallationCorePropertiesDate(System.currentTimeMillis());
                WonderPushConfiguration.setCachedInstallationCoreProperties(propertiesString);
                updateInstallation(properties, false, null);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Unexpected error while updating installation core properties", ex);
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while updating installation core properties", ex);
        }
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
    public static synchronized void putInstallationCustomProperties(JSONObject customProperties) {
        try {
            JSONObject updatedRef = WonderPushConfiguration.getCachedInstallationCustomPropertiesUpdated();
            if (updatedRef == null) updatedRef = new JSONObject();
            JSONObject updated = WonderPushConfiguration.getCachedInstallationCustomPropertiesUpdated();
            if (updated == null) updated = new JSONObject();
            try {
                JSONUtil.merge(updated, customProperties);
            } catch (JSONException ex) {
                WonderPush.logError("Unexpected error while merging custom properties", ex);
            }
            if (!JSONUtil.equals(updatedRef, updated)) {
                if (putInstallationCustomPropertiesDelayedTask != null) {
                    putInstallationCustomPropertiesDelayedTask.cancel(false);
                }
                long nowRT = SystemClock.elapsedRealtime();
                long now = System.currentTimeMillis();
                long firstWrite = WonderPushConfiguration.getCachedInstallationCustomPropertiesFirstDelayedWrite();
                if (firstWrite == 0) {
                    WonderPushConfiguration.setCachedInstallationCustomPropertiesFirstDelayedWrite(nowRT);
                    firstWrite = nowRT;
                }
                WonderPushConfiguration.setCachedInstallationCustomPropertiesUpdated(updated);
                WonderPushConfiguration.setCachedInstallationCustomPropertiesUpdatedDate(now);
                putInstallationCustomPropertiesDelayedTask = sScheduledExecutor.schedule(
                        new Callable<Void>() {
                            @Override
                            public Void call() {
                                try {
                                    putInstallationCustomProperties_inner();
                                } catch (Exception ex) {
                                    Log.e(TAG, "Unexpected error on scheduled task", ex);
                                }
                                return null;
                            }
                        },
                        Math.min(CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY,
                                firstWrite + CACHED_INSTALLATION_CUSTOM_PROPERTIES_MAX_DELAY - nowRT),
                        TimeUnit.MILLISECONDS);
            }
            onInteraction();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while putting installation custom properties", e);
        }
    }

    private static synchronized void putInstallationCustomProperties_inner() {
        JSONObject written = WonderPushConfiguration.getCachedInstallationCustomPropertiesWritten();
        JSONObject updated = WonderPushConfiguration.getCachedInstallationCustomPropertiesUpdated();
        JSONObject customProperties;
        try {
            customProperties = JSONUtil.diff(written, updated);
        } catch (JSONException ex) {
            WonderPush.logError("Unexpected error while calculating custom properties diff, using whole value", ex);
            customProperties = updated;
        }
        if (customProperties != null && customProperties.length() > 0) {
            try {
                JSONObject properties = new JSONObject();
                try {
                    properties.put("custom", customProperties);
                } catch (JSONException e) {
                    Log.e(TAG, "Unexpected error while updating installation core properties", e);
                }
                updateInstallation(properties, false, null);
                long now = System.currentTimeMillis();
                WonderPushConfiguration.setCachedInstallationCustomPropertiesWritten(updated);
                WonderPushConfiguration.setCachedInstallationCustomPropertiesWrittenDate(now);
            } catch (Exception ex) {
                WonderPush.logError("Unexpected error while putting custom properties", ex);
            }
        }
        WonderPushConfiguration.setCachedInstallationCustomPropertiesFirstDelayedWrite(0);
    }

    static void updateInstallation(JSONObject properties, boolean overwrite, ResponseHandler handler) {
        String propertyEndpoint = "/installation";
        RequestParams parameters = new RequestParams();
        parameters.put("body", properties.toString());
        parameters.put("overwrite", overwrite ? "true" : "false");
        postEventually(propertyEndpoint, parameters, handler);
    }

    /**
     * Get the current timestamp in milliseconds, UTC.
     * @return A timestamp in milliseconds
     */
    protected static long getTime() {
        // Initialization
        if (deviceDateToServerDateUncertainty == Long.MAX_VALUE) {
            deviceDateToServerDateUncertainty = WonderPushConfiguration.getDeviceDateSyncUncertainty();
            deviceDateToServerDateOffset = WonderPushConfiguration.getDeviceDateSyncOffset();
        }
        long currentTimeMillis = System.currentTimeMillis();
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long startupToDeviceOffset = currentTimeMillis - elapsedRealtime;
        if (startupDateToDeviceDateOffset == Long.MAX_VALUE) {
            startupDateToDeviceDateOffset = startupToDeviceOffset;
        }

        // Check device date consistency with startup date
        if (Math.abs(startupToDeviceOffset - startupDateToDeviceDateOffset) > 1000) {
            // System time has jumped (by at least 1 second), or has drifted with regards to elapsedRealtime.
            // Apply the offset difference to resynchronize the "device" sync offset onto the new system date.
            deviceDateToServerDateOffset -= startupToDeviceOffset - startupDateToDeviceDateOffset;
            WonderPushConfiguration.setDeviceDateSyncOffset(deviceDateToServerDateOffset);
            startupDateToDeviceDateOffset = startupToDeviceOffset;
        }

        if (startupDateToServerDateUncertainty <= deviceDateToServerDateUncertainty
                // Don't use the startup date if it has not been synced, use and trust last device date sync
                && startupDateToServerDateUncertainty != Long.MAX_VALUE) {
            return elapsedRealtime + startupDateToServerDateOffset;
        } else {
            return currentTimeMillis + deviceDateToServerDateOffset;
        }
    }

    /**
     * Synchronize time with the WonderPush servers.
     * @param elapsedRealtimeSend
     *            The time at which the request was sent.
     * @param elapsedRealtimeReceive
     *            The time at which the response was received.
     * @param serverDate
     *            The time at which the server received the request, as read in the response.
     * @param serverTook
     *            The time the server took to process the request, as read in the response.
     */
    protected static void syncTimeWithServer(long elapsedRealtimeSend, long elapsedRealtimeReceive, long serverDate, long serverTook) {
        if (serverDate == 0) {
            return;
        }

        // We have two synchronization sources:
        // - The "startup" sync, bound to the process lifecycle, using SystemClock.elapsedRealtime()
        //   This time source cannot be messed up with.
        //   It is only valid until the device reboots, at which time a new time origin is set.
        // - The "device" sync, bound to the system clock, using System.currentTimeMillis()
        //   This time source is affected each time the user changes the date and time,
        //   but it is not affected by timezone or daylight saving changes.
        // The "startup" sync must be saved into a "device" sync in order to persist between runs of the process.
        // The "startup" sync should only be stored in memory, and no attempt to count reboot should be taken.

        // Initialization
        if (deviceDateToServerDateUncertainty == Long.MAX_VALUE) {
            deviceDateToServerDateUncertainty = WonderPushConfiguration.getDeviceDateSyncUncertainty();
            deviceDateToServerDateOffset = WonderPushConfiguration.getDeviceDateSyncOffset();
        }
        long startupToDeviceOffset = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        if (startupDateToDeviceDateOffset == Long.MAX_VALUE) {
            startupDateToDeviceDateOffset = startupToDeviceOffset;
        }

        long uncertainty = (elapsedRealtimeReceive - elapsedRealtimeSend - serverTook) / 2;
        long offset = serverDate + serverTook / 2 - (elapsedRealtimeSend + elapsedRealtimeReceive) / 2;

        // We must improve the quality of the "startup" sync. We can trust elaspedRealtime() based measures.
        if (
                // Case 1. Lower uncertainty
                uncertainty < startupDateToServerDateUncertainty
                // Case 2. Additional check for exceptional server-side time gaps
                //         Calculate whether the two offsets agree within the total uncertainty limit
                || Math.abs(offset - startupDateToServerDateOffset)
                        > uncertainty+startupDateToServerDateUncertainty
                        // note the RHS overflows with the Long.MAX_VALUE initialization, but case 1 handles that
        ) {
            // Case 1. Take the new, more accurate synchronization
            // Case 2. Forget the old synchronization, time have changed too much
            startupDateToServerDateOffset = offset;
            startupDateToServerDateUncertainty = uncertainty;
        }

        // We must detect whether the "device" sync is still valid, otherwise we must update it.
        if (
                // Case 1. Lower uncertainty
                startupDateToServerDateUncertainty < deviceDateToServerDateUncertainty
                // Case 2. Local clock was updated, or the two time sources have drifted from each other
                || Math.abs(startupToDeviceOffset - startupDateToDeviceDateOffset) > startupDateToServerDateUncertainty
                // Case 3. Time gap between the "startup" and "device" sync
                || Math.abs(deviceDateToServerDateOffset - (startupDateToServerDateOffset - startupDateToDeviceDateOffset))
                        > deviceDateToServerDateUncertainty + startupDateToServerDateUncertainty
                        // note the RHS overflows with the Long.MAX_VALUE initialization, but case 1 handles that
        ) {
            deviceDateToServerDateOffset = startupDateToServerDateOffset - startupDateToDeviceDateOffset;
            deviceDateToServerDateUncertainty = startupDateToServerDateUncertainty;
            WonderPushConfiguration.setDeviceDateSyncOffset(deviceDateToServerDateOffset);
            WonderPushConfiguration.setDeviceDateSyncUncertainty(deviceDateToServerDateUncertainty);
        }
    }

    /**
     * Send an event to be tracked to WonderPush.
     * @param type
     *            The event type, or name.
     *            Event types starting with an {@code @} character are reserved.
     */
    public static void trackEvent(String type) {
        try {
            trackEvent(type, null);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while tracking user event of type \"" + type + "\"", e);
        }
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
        try {
            if (type == null || type.length() == 0 || type.charAt(0) == '@') {
                throw new IllegalArgumentException("Bad event type");
            }
            sendEvent(type, null, customData);
            onInteraction();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while tracking user event of type \"" + type + "\"", e);
        }
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
                    WonderPush.logError("Error building event object body", ex);
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
            if (!event.has("actionDate")) {
                event.put("actionDate", getTime());
            }
        } catch (JSONException ex) {
            WonderPush.logError("Error building event object body", ex);
        }

        RequestParams parameters = new RequestParams();
        parameters.put("body", event.toString());
        postEventually(eventEndpoint, parameters, null);
    }

    protected static void onInteraction() {
        long lastInteractionDate = WonderPushConfiguration.getLastInteractionDate();
        long lastAppOpenDate = WonderPushConfiguration.getLastAppOpenDate();
        long lastAppCloseDate = WonderPushConfiguration.getLastAppCloseDate();
        JSONObject lastReceivedNotificationInfo = WonderPushConfiguration.getLastReceivedNotificationInfoJson();
        if (lastReceivedNotificationInfo == null) lastReceivedNotificationInfo = new JSONObject();
        long lastReceivedNotificationDate = lastReceivedNotificationInfo.optLong("actionDate", Long.MAX_VALUE);
        JSONObject lastOpenedNotificationInfo = WonderPushConfiguration.getLastOpenedNotificationInfoJson();
        if (lastOpenedNotificationInfo == null) lastOpenedNotificationInfo = new JSONObject();
        long lastOpenedNotificationDate = lastOpenedNotificationInfo.optLong("actionDate", Long.MAX_VALUE);
        long now = getTime();

        if (
                now - lastInteractionDate >= DIFFERENT_SESSION_REGULAR_MIN_TIME_GAP
                || (
                        lastReceivedNotificationDate > lastInteractionDate
                        && now - lastInteractionDate >= DIFFERENT_SESSION_NOTIFICATION_MIN_TIME_GAP
                )
        ) {
            // We will track a new app open event

            // We must first close the possibly still-open previous session
            if (lastAppCloseDate < lastAppOpenDate) {
                JSONObject closeInfo = WonderPushConfiguration.getLastAppOpenInfoJson();
                if (closeInfo == null) {
                    closeInfo = new JSONObject();
                }
                long appCloseDate = lastInteractionDate;
                try {
                    closeInfo.put("actionDate", appCloseDate);
                    closeInfo.put("openedTime", appCloseDate - lastAppOpenDate);
                } catch (JSONException e) {
                    logDebug("Failed to fill @APP_CLOSE information", e);
                }
                trackInternalEvent("@APP_CLOSE", closeInfo);
                WonderPushConfiguration.setLastAppCloseDate(appCloseDate);
            }

            // Track the new app open event
            JSONObject openInfo = new JSONObject();
            // Add the elapsed time between the last received notification
            if (lastReceivedNotificationDate <= now) {
                try {
                    openInfo.put("lastReceivedNotificationTime", now - lastReceivedNotificationDate);
                } catch (JSONException e) {
                    logDebug("Failed to fill @APP_OPEN previous notification information", e);
                }
            }
            // Add the information of the clicked notification
            if (now - lastOpenedNotificationDate < 10 * 1000) { // allow a few seconds between click on the notification and the call to this method
                try {
                    openInfo.putOpt("notificationId", lastOpenedNotificationInfo.opt("notificationId"));
                    openInfo.putOpt("campaignId", lastOpenedNotificationInfo.opt("campaignId"));
                } catch (JSONException e) {
                    logDebug("Failed to fill @APP_OPEN opened notification information", e);
                }
            }
            trackInternalEvent("@APP_OPEN", openInfo);
            WonderPushConfiguration.setLastAppOpenDate(now);
            WonderPushConfiguration.setLastAppOpenInfoJson(openInfo);
        }

        WonderPushConfiguration.setLastInteractionDate(now);
    }

    protected static void monitorActivitiesLifecycle() {
        if (!sActivityLifecycleCallbacksRegistered && sActivityLifecycleCallbacks != null && sApplication != null) {
            WonderPushCompatibilityHelper.ApplicationRegisterActivityLifecycleCallbacks(sApplication, sActivityLifecycleCallbacks);
            sActivityLifecycleCallbacksRegistered = true;
        }
    }

    protected static Activity getCurrentActivity() {
        Activity candidate = null;
        if (sActivityLifecycleCallbacksRegistered
                && sActivityLifecycleCallbacks.hasResumedActivities()) {
            candidate = sActivityLifecycleCallbacks.getLastResumedActivity();
        }
        if (candidate == null) {
            for (Activity activity : sTrackedActivities.keySet()) {
                if (activity.hasWindowFocus() && !activity.isFinishing()) {
                    candidate = activity;
                    break;
                }
            }
        }
        return candidate;
    }

    /**
     * Whether {@link #initialize(Context, String, String)} has been called.
     * Different from having fetched an access token,
     * and hence from {@link #INTENT_INTIALIZED} being dispatched.
     * @return {@code true} if the SDK is initialized, {@code false} otherwise.
     */
    static boolean isInitialized() {
        return sIsInitialized;
    }

    /**
     * Whether the SDK is ready to operate and
     * the {@link #INTENT_INTIALIZED} intent has been dispatched.
     *
     * The SDK is ready when it is initialized and has fetched an access token.
     * @return {@code true} if the SDK is ready, {@code false} otherwise.
     */
    public static boolean isReady() {
        return sIsReady;
    }

    /**
     * Initialize WonderPush.<br />
     * <b>Call this method before using WonderPush.</b>
     *
     * <p>
     *   A good place to initialize WonderPush is in your main activity's
     *   <a href="http://developer.android.com/reference/android/app/Activity.html#onCreate(android.os.Bundle)">
     *   {@code onCreate(Bundle)}</a> method as follows:
     * </p>
     * <pre><code>protected void onCreate(Bundle savedInstance) {
     *    WonderPush.initialize(this);
     *}</code></pre>
     *
     * <p>
     *   This function will instantiate the {@link WonderPushInitializer} implementation you provided in your
     *   {@code AndroidManifest.xml}.<br />
     *   <i>Please look at that interface documentation for detailed instruction.</i>
     * </p>
     *
     * @param context
     *            The main {@link Activity} of your application, or failing that, the {@link Application} context.
     *            It must be the same activity that you declared in the {@code <meta-data>} tag
     *            under the WonderPush {@code <receiver>} tag in your {@code AndroidManifest.xml}.
     */
    public static void initialize(final Context context) {
        try {
            ensureInitialized(context);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while initializing the SDK", e);
        }
    }

    /**
     * Initialize WonderPush from your {@link WonderPushInitializer} implementation.
     *
     * <p>
     *   Prefer calling the simpler {@link WonderPush#initialize(Context)} function directly, as it will
     *   instantiate your {@link WonderPushInitializer} implementation which will in turn call this function.
     *   This way you concentrate the retrieval of your credentials from secure storage in a single location.
     * </p>
     *
     * @param context
     *            The main {@link Activity} of your application, or failing that, the {@link Application} context.
     *            It must be the same activity that you declared in the {@code <meta-data>} tag
     *            under the WonderPush {@code <receiver>} tag in your {@code AndroidManifest.xml}.
     * @param clientId
     *            The clientId of your application.
     * @param clientSecret
     *            The clientSecret of your application.
     */
    public static void initialize(final Context context, final String clientId, String clientSecret) {
        try {
            if (!sIsInitialized || (
                    clientId != null && clientSecret != null && (!clientId.equals(sClientId) || !clientSecret.equals(sClientSecret))
            )) {

                sIsInitialized = false;
                setNetworkAvailable(false);

                sApplicationContext = context.getApplicationContext();
                sClientId = clientId;
                sClientSecret = clientSecret;
                sBaseURL = PRODUCTION_API_URL;

                WonderPushConfiguration.initialize(getApplicationContext());
                if (sBeforeInitializationUserIdSet) {
                    setUserId(sBeforeInitializationUserId);
                }

                WonderPushRequestVault.initialize();

                // Initialize OpenUDID
                OpenUDID_manager.sync(getApplicationContext());

                sIsInitialized = true;

                // Permission checks
                if (context.getPackageManager().checkPermission(android.Manifest.permission.INTERNET, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Missing INTERNET permission. Add <uses-permission android:name=\"android.permission.INTERNET\" /> under <manifest> in your AndroidManifest.xml");
                }
                if (context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED
                        && context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Missing ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION permission. Add <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /> under <manifest> in your AndroidManifest.xml (you can add either or both)");
                } else if (context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Only ACCESS_COARSE_LOCATION permission is granted. For more precision, you should strongly consider adding <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /> under <manifest> in your AndroidManifest.xml");
                }

                // Wait for UDID to be ready and fetch anonymous token if needed.
                WonderPush.safeDefer(new Runnable() {
                    @Override
                    public void run() {
                        if (isUDIDReady()) {
                            boolean isFetchingToken = WonderPushRestClient.fetchAnonymousAccessTokenIfNeeded(new ResponseHandler() {
                                @Override
                                public void onFailure(Throwable e, Response errorResponse) {
                                }

                                @Override
                                public void onSuccess(Response response) {
                                    updateInstallationCoreProperties(context);
                                    registerForPushNotification(context);
                                    if (WonderPushConfiguration.getCachedInstallationCustomPropertiesFirstDelayedWrite() != 0) {
                                        putInstallationCustomProperties_inner();
                                    }
                                    sIsReady = true;
                                    Intent broadcast = new Intent(INTENT_INTIALIZED);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);
                                }
                            });
                            if (!isFetchingToken) {
                                updateInstallationCoreProperties(context);
                                registerForPushNotification(context);
                                if (WonderPushConfiguration.getCachedInstallationCustomPropertiesFirstDelayedWrite() != 0) {
                                    putInstallationCustomProperties_inner();
                                }
                                sIsReady = true;
                                Intent broadcast = new Intent(INTENT_INTIALIZED);
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);
                            }
                        } else {
                            WonderPush.safeDefer(this, 100);
                        }
                    }
                }, 0);

            }

            initializeForApplication(context);
            initializeForActivity(context);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while initializing the SDK", e);
        }
    }

    protected static void initializeForApplication(Context context) {
        if (sApplication != null || !(context instanceof Application)) {
            return;
        }
        sApplication = (Application) context;
        monitorActivitiesLifecycle();
    }

    protected static void initializeForActivity(Context context) {
        if (!(context instanceof Activity)) {
            return;
        }
        Activity activity = (Activity) context;

        sTrackedActivities.put(activity, null);

        showPotentialNotification(activity, activity.getIntent());
        onInteraction(); // keep after onCreateMainActivity() as a possible received notification's information is needed
    }

    /**
     * Instantiate the {@link WonderPushInitializer} interface configured in the {@code AndroidManifest.xml},
     * and calls it if the SDK is not initialized yet.
     * @param context
     * @return {@code true} if no error happened, {@code false} otherwise
     */
    protected static boolean ensureInitialized(Context context) {
        if (!isInitialized()) {

            String initializerClassName = null;
            try {

                ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                initializerClassName = bundle.getString(METADATA_INITIALIZER_CLASS);
                if (initializerClassName == null) {
                    Log.e(TAG, "Failed to load initializer class. Did you add: <meta-data android:name=\"" + METADATA_INITIALIZER_CLASS + "\" android:value=\"com.package.YourWonderPushInitializerImpl\"/> under <application> in your AndroidManifest.xml");
                }

                Class<? extends WonderPushInitializer> initializerClass = Class.forName(initializerClassName).asSubclass(WonderPushInitializer.class);
                WonderPushInitializer initializer = initializerClass.newInstance();

                initializer.initialize(context);

            } catch (NameNotFoundException e) {
                Log.e(TAG, "Failed to load initializer class", e);
            } catch (NullPointerException e) {
                Log.e(TAG, "Failed to load initializer class", e);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Failed to load initializer class. Check your <meta-data android:name=\"" + METADATA_INITIALIZER_CLASS + "\" android:value=\"com.package.YourWonderPushInitializerImpl\"/> entry under <application> in your AndroidManifest.xml", e);
            } catch (InstantiationException e) {
                Log.e(TAG, "Failed to intantiate the initializer class " + initializerClassName + ". Make sure it has a public default constructor with no argument.", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Failed to intantiate the initializer class " + initializerClassName + ". Make sure it has a public default constructor with no argument.", e);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while ensuring SDK initialization", e);
            }

        } else {

            // No need to get clientId/clientSecret once again
            // we only need to re-run the Activity-related initialization
            initialize(context, null, null);

        }

        return isInitialized();
    }

    /**
     * Sets the user id, used to identify a single identity across multiple devices,
     * and to correctly identify multiple users on a single device.
     *
     * <p>If not called, the last used user id it assumed. Defaulting to {@code null} if none is known.</p>
     *
     * <p>Prefer calling this method just before calling {@link #initialize(Context)}, rather than just after.</p>
     *
     * <p>
     *   Upon changing userId, the access token is wiped, so avoid unnecessary calls, like calling with {@code null}
     *   just before calling with a user id.
     * </p>
     *
     * @param userId
     *            The user id, unique to your application.
     *            Use {@code null} for anonymous users.<br />
     *            You are strongly encouraged to use your own unique internal identifier.
     */
    public static void setUserId(String userId) {
        try {
            if ("".equals(userId)) userId = null;

            // Do nothing if not initialized
            if (!isInitialized()) {
                sBeforeInitializationUserIdSet = true;
                sBeforeInitializationUserId = userId;
                return;
            }
            sBeforeInitializationUserIdSet = false;
            sBeforeInitializationUserId = null;

            String oldUserId = WonderPushConfiguration.getUserId();
            if (userId == null && oldUserId == null
                    || userId != null && userId.equals(oldUserId)) {
                // User id is the same as before, nothing needs to be done
            } else {
                // The user id changed, we must reset the access token
                WonderPushConfiguration.invalidateCredentials();
                WonderPushConfiguration.setUserId(userId);
                // DO NOT fetch another access token now, or beware the possible callback from initialize()
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while setting userId to \"" + userId + "\"", e);
        }
    }

    /**
     * Gets the user id, used to identify a single identity across multiple devices,
     * and to correctly identify multiple users on a single device.
     *
     * <p>You should not call this method before initializing the SDK.</p>
     *
     * @return The user id, which may be {@code null} for anonymous users.
     * @see #setUserId(String)
     * @see #initialize(Context)
     */
    public static String getUserId() {
        String userId = null;
        try {
            userId = WonderPushConfiguration.getUserId();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while getting userId", e);
        }
        return userId;
    }

    /**
     * Gets the device id, used to identify a single device across applications,
     * and to correctly identify multiple users on a single device.
     *
     * <p>You should not call this method before initializing the SDK.</p>
     *
     * <p>
     *   Because of the way our device id is build, it is populated asynchronously,
     *   so you may get a {@code null} response even a few moments after calling
     *   {@link #initialize(Context)}.
     *   You can either wait a bit (1 second should be enough on modern devices), or
     *   wait for {@link #isReady()} to return {@code true}, which may take some more
     *   time on the first launch, especially is the network connection is bad.
     * </p>
     *
     * @return The device id, or {@code null} if the SDK is not initialized.
     * @see #isReady()
     * @see #initialize(Context)
     */
    public static String getDeviceId() {
        String deviceId = null;
        try {
            if (isUDIDReady()) {
                deviceId = OpenUDID_manager.getOpenUDID();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while getting userId", e);
        }
        return deviceId;
    }

    /**
     * Gets the device id, used to identify a single device across applications,
     * and to correctly identify multiple users on a single device.
     *
     * <p>You should not call this method before the SDK is ready.</p>
     *
     * @return The device id, or {@code null} if the SDK is not initialized.
     * @see #isReady()
     */
    public static String getInstallationId() {
        String installationId = null;
        try {
            installationId = WonderPushConfiguration.getInstallationId();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while getting userId", e);
        }
        return installationId;
    }

    /**
     * Gets the push token, used to send notification to this installation.
     *
     * <p>You should not call this method before initializing the SDK.</p>
     *
     * @return The push token, or {@code null} if the installation is not yet
     *     registered to push notifications, or has not finished refreshing
     *     the push token after a forced update.
     */
    public static String getPushToken() {
        String pushToken = null;
        try {
            pushToken = WonderPushConfiguration.getGCMRegistrationId();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while getting userId", e);
        }
        return pushToken;
    }

    /**
     * Method to be called in your own Google Cloud Messaging
     * <a href="http://developer.android.com/reference/android/content/BroadcastReceiver.html"><tt>BroadcastReceiver</tt></a>
     * to handle WonderPush push notifications.
     *
     * <p>
     *   <b>Note:</b> This is only needed if you use your own {@link BroadcastReceiver}, as previously
     *   advertised in <a href="../../../packages.html#installing-sdk--configuring-sdk">the guide</a>.
     * </p>
     *
     * <p>
     *   Implement your <a href="http://developer.android.com/reference/android/content/BroadcastReceiver.html#onReceive(android.content.Context, android.content.Intent)"><tt>BroadcastReceiver.onReceive(Context, Intent)</tt></a>
     *   method as follows:
     * </p>
     * <pre><code>public void onReceive(Context context, Intent intent) {
     *    if (WonderPush.onBroadcastReceived(context, intent, R.drawable.icon, YourMainActivity.class)) {
     *        return;
     *    }
     *    // Do your own handling here
     *}</code></pre>
     *
     * <p>
     *   For more information about Google Cloud Messaging visit:
     *   <a href="https://developers.google.com/cloud-messaging/android/client">https://developers.google.com/cloud-messaging/android/client</a>.
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
        try {
            return WonderPushGcmClient.onBroadcastReceived(context, intent, iconResource, activityClass);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while giving broadcast to the receiver", e);
        }
        return false;
    }

    /**
     * Returns whether push notification are enabled.
     * @return {@code true} by default as no explicit user permission is required.
     */
    public static boolean getNotificationEnabled() {
        return WonderPushConfiguration.getNotificationEnabled();
    }

    /**
     * Sets whether to enable push notifications for the current device.
     * @param status {@code false} to opt out of push notifications.
     */
    public static void setNotificationEnabled(boolean status) {
        try {
            String value = status
                    ? INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTIN
                    : INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTOUT;
            JSONObject properties = new JSONObject();
            JSONObject preferences = new JSONObject();
            properties.put("preferences", preferences);
            preferences.put("subscriptionStatus", value);
            updateInstallation(properties, false, null);
            WonderPushConfiguration.setNotificationEnabled(status);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while setting notification enabled to " + status, e);
        }
    }

    /**
     * Gets the application context that was captured during the
     * {@link WonderPush#initialize(Context, String, String)} call.
     */
    protected static Context getApplicationContext() {
        if (null == sApplicationContext)
            Log.e(TAG, "Application context is null, did you call WonderPush.initialize()?");
        return sApplicationContext;
    }

    protected static WonderPushDialogBuilder createDialogNotificationBase(final Context context, final NotificationModel notif) {
        WonderPushDialogBuilder builder = new WonderPushDialogBuilder(context, notif, new WonderPushDialogBuilder.OnChoice() {
            @Override
            public void onChoice(WonderPushDialogBuilder dialog, ButtonModel which) {
                handleDialogButtonAction(dialog, which);
            }
        });
        builder.setupTitleAndIcon();

        return builder;
    }

    protected static void createDefaultCloseButtonIfNeeded(WonderPushDialogBuilder builder) {
        if (builder.getNotificationModel().getButtonCount() == 0) {
            ButtonModel defaultButton = new ButtonModel();
            defaultButton.label = builder.getContext().getResources().getString(R.string.wonderpush_close);
            builder.getNotificationModel().addButton(defaultButton);
        }
    }

    protected static void handleDialogButtonAction(WonderPushDialogBuilder dialog, ButtonModel buttonClicked) {
        JSONObject eventData = new JSONObject();
        try {
            eventData.put("buttonLabel", buttonClicked == null ? null : buttonClicked.label);
            eventData.put("reactionTime", dialog.getShownDuration());
            eventData.putOpt("custom", dialog.getInteractionEventCustom());
            eventData.put("campaignId", dialog.getNotificationModel().getCampaignId());
            eventData.put("notificationId", dialog.getNotificationModel().getNotificationId());
        } catch (JSONException e) {
            WonderPush.logError("Failed to fill the @NOTIFICATION_ACTION event", e);
        }
        trackInternalEvent("@NOTIFICATION_ACTION", eventData);

        if (buttonClicked == null) {
            logDebug("User cancelled the dialog");
            return;
        }
        Context context = dialog.getContext();
        try {
            for (ActionModel action : buttonClicked.actions) {
                try {
                    if (action == null || action.getType() == null) {
                        // Skip unrecognized action types
                        continue;
                    }
                    switch (action.getType()) {
                        case CLOSE:
                            // Noop
                            break;
                        case MAP_OPEN:
                            handleMapOpenAction(context, dialog.getNotificationModel(), action);
                            break;
                        case LINK:
                            handleLinkAction(context, action);
                            break;
                        case RATING:
                            handleRatingAction(context, action);
                            break;
                        case TRACK_EVENT:
                            handleTrackEventAction(action);
                            break;
                        case UPDATE_INSTALLATION:
                            handleUpdateInstallationAction(action);
                            break;
                        case METHOD:
                            handleMethodAction(action);
                            break;
                        default:
                            Log.w(TAG, "Unhandled button action \"" + action.getType() + "\"");
                            break;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while handling button action " + action, ex);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling button actions", ex);
        }
    }

    protected static void handleTextNotification(final Context context, final NotificationTextModel notif) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);

        if (notif.getMessage() == null) {
            Log.w(TAG, "Got no message to display for a plain notification");
        } else {
            builder.setMessage(notif.getMessage());
        }

        createDefaultCloseButtonIfNeeded(builder);
        builder.setupButtons();

        builder.show();
    }

    @SuppressLint("InflateParams")
    protected static void handleMapNotification(final Context context, final NotificationMapModel notif) {
        WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);

        final NotificationMapModel.Map map = notif.getMap();
        if (map == null) {
            Log.e(TAG, "Could not get the map from the notification");
            return;
        }
        final NotificationMapModel.Place place = map.getPlace();
        if (place == null) {
            Log.e(TAG, "Could not get the place from the map");
            return;
        }
        final NotificationMapModel.Point point = place.getPoint();

        final View dialogView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.wonderpush_notification_map_dialog, null, false);
        final TextView text = (TextView) dialogView.findViewById(R.id.wonderpush_notification_map_dialog_text);
        if (notif.getMessage() != null) {
            text.setVisibility(View.VISIBLE);
            text.setText(notif.getMessage());
            text.setMovementMethod(new ScrollingMovementMethod());
        }
        final ImageView mapImg = (ImageView) dialogView.findViewById(R.id.wonderpush_notification_map_dialog_map);
        builder.setView(dialogView);

        if (notif.getButtonCount() == 0) {
            // Close button
            ButtonModel closeButton = new ButtonModel();
            closeButton.label = context.getResources().getString(R.string.wonderpush_close);
            ActionModel closeAction = new ActionModel();
            closeAction.setType(ActionModel.Type.CLOSE);
            closeButton.actions.add(closeAction);
            notif.addButton(closeButton);
            // Open button
            ButtonModel openButton = new ButtonModel();
            openButton.label = context.getResources().getString(R.string.wonderpush_open);
            ActionModel openAction = new ActionModel();
            openAction.setType(ActionModel.Type.MAP_OPEN);
            openButton.actions.add(openAction);
            notif.addButton(openButton);
        }
        builder.setupButtons();

        builder.show();

        new AsyncTask<Object, Object, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object... args) {
                try {
                    String loc;
                    if (point != null) {
                        loc = point.getLat() + "," + point.getLon();
                    } else if (place.getName() != null) {
                        loc = place.getName();
                    } else {
                        loc = place.getQuery();
                    }
                    if (loc == null) {
                        Log.e(TAG, "No location for map");
                        return null;
                    }
                    int screenWidth = getScreenWidth(context);
                    int screenHeight = getScreenHeight(context);
                    double ratio = screenWidth / (double)screenHeight;
                    int width = ratio >= 1 ? Math.min(640, screenWidth) : (int)Math.floor(ratio * Math.min(640, screenHeight));
                    int height = ratio <= 1 ? Math.min(640, screenHeight) : (int)Math.floor(Math.min(640, screenWidth) / ratio);
                    String size = width + "x" + height;
                    int scale = getScreenDensity(context) >= 192 ? 2 : 1;
                    URL url = new URL("https://maps.google.com/maps/api/staticmap"
                            + "?center=" + loc
                            + "&zoom=" + (place.getZoom() != null ? place.getZoom() : 13)
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
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error while loading map image", e);
                }
                return null;
            }
            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (bmp == null) {
                    mapImg.setVisibility(View.GONE);
                    text.setMaxLines(Integer.MAX_VALUE);
                } else {
                    mapImg.setScaleType(ScaleType.CENTER_CROP);
                    mapImg.setImageBitmap(bmp);
                }
            }
        }.execute();
    }

    protected static void handleMapOpenAction(Context context, NotificationModel notif, ActionModel action) {
        try {
            NotificationMapModel.Place place;
            try {
                place = ((NotificationMapModel) notif).getMap().getPlace();
            } catch (Exception e) {
                Log.e(TAG, "Could not get the place from the map", e);
                return;
            }
            NotificationMapModel.Point point = place.getPoint();

            Uri.Builder geo = new Uri.Builder();
            geo.scheme("geo");
            if (point != null) {
                if (place.getName() != null) {
                    geo.authority("0,0");
                    geo.appendQueryParameter("q", point.getLat() + "," + point.getLon() + "(" + place.getName() + ")");
                } else {
                    geo.authority(point.getLat() + "," + point.getLon());
                    if (place.getZoom() != null) {
                        geo.appendQueryParameter("z", place.getZoom().toString());
                    }
                }
            } else if (place.getQuery() != null) {
                geo.authority("0,0");
                geo.appendQueryParameter("q", place.getQuery());
            }
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setData(geo.build());
            if (open.resolveActivity(context.getPackageManager()) != null) {
                logDebug("Will open location " + open.getDataString());
                context.startActivity(open);
            } else {
                logDebug("No activity can open location " + open.getDataString());
                logDebug("Falling back to regular URL");
                geo = new Uri.Builder();
                geo.scheme("http");
                geo.authority("maps.google.com");
                geo.path("maps");
                if (point != null) {
                    geo.appendQueryParameter("q", point.getLat() + "," + point.getLon());
                    if (place.getZoom() != null) {
                        geo.appendQueryParameter("z", place.getZoom().toString());
                    }
                } else if (place.getQuery() != null) {
                    geo.appendQueryParameter("q", place.getQuery());
                } else if (place.getName() != null) {
                    geo.appendQueryParameter("q", place.getName());
                }
                open = new Intent(Intent.ACTION_VIEW);
                open.setData(geo.build());
                if (open.resolveActivity(context.getPackageManager()) != null) {
                    logDebug("Opening URL " + open.getDataString());
                    context.startActivity(open);
                } else {
                    logDebug("No activity can open URL " + open.getDataString());
                    Log.w(TAG, "Cannot open map!");
                    Toast.makeText(context, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while opening map", e);
            Toast.makeText(context, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
        }
    }

    protected static WonderPushDialogBuilder createWebNotificationBasePre(final Context context, final NotificationModel notif, WonderPushView webView) {
        final WonderPushDialogBuilder builder = createDialogNotificationBase(context, notif);
        builder.setupButtons();

        webView.setShowCloseButton(notif.getButtonCount() < 1);

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

    protected static void handleHTMLNotification(final Context context, final NotificationHtmlModel notif) {
        if (notif.getMessage() == null) {
            Log.w(TAG, "No HTML content to display in the notification!");
            return;
        }

        // Build the dialog
        WonderPushView webView = new WonderPushView(context);
        final WonderPushDialogBuilder builder = createWebNotificationBasePre(context, notif, webView);
        builder.setupButtons();

        // Set content
        webView.loadDataWithBaseURL(notif.getBaseUrl(), notif.getMessage(), "text/html", "utf-8", null);

        // Show time!
        builder.show();
    }

    protected static void handleURLNotification(Context context, NotificationUrlModel notif) {
        if (notif.getUrl() == null) {
            Log.e(TAG, "No URL to display in the notification!");
            return;
        }

        WonderPushView webView = new WonderPushView(context);
        final WonderPushDialogBuilder builder = createWebNotificationBasePre(context, notif, webView);
        builder.setupButtons();

        // Set content
        webView.setFullUrl(notif.getUrl());

        // Show time!
        builder.show();
    }

    protected static void handleLinkAction(Context context, ActionModel action) {
        try {
            String url = action.getUrl();
            if (url == null) {
                Log.e(TAG, "No url in a " + ActionModel.Type.LINK + " action!");
                return;
            }
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "No service for intent " + intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform a " + ActionModel.Type.LINK + " action", e);
        }
    }

    protected static void handleRatingAction(Context context, ActionModel action) {
        try {
            Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "No service for intent " + intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform a " + ActionModel.Type.RATING + " action", e);
        }
    }

    protected static void handleTrackEventAction(ActionModel action) {
        JSONObject event = action.getEvent();
        if (event == null) {
            Log.e(TAG, "Got no event to track for a " + ActionModel.Type.TRACK_EVENT + " action");
            return;
        }
        if (!event.has("type") || event.optString("type", null) == null) {
            Log.e(TAG, "Got no type in the event to track for a " + ActionModel.Type.TRACK_EVENT + " action");
            return;
        }
        trackEvent(event.optString("type", null), event.optJSONObject("custom"));
    }

    protected static void handleUpdateInstallationAction(ActionModel action) {
        JSONObject custom = action.getCustom();
        if (custom == null) {
            Log.e(TAG, "Got no installation custom properties to update for a " + ActionModel.Type.UPDATE_INSTALLATION + " action");
            return;
        }
        if (custom.length() == 0) {
            logDebug("Empty installation custom properties for an update, for a " + ActionModel.Type.UPDATE_INSTALLATION + " action");
            return;
        }
        putInstallationCustomProperties(custom);
    }

    protected static void handleMethodAction(ActionModel action) {
        String method = action.getMethod();
        String arg = action.getMethodArg();
        if (method == null) {
            Log.e(TAG, "Got no method to call for a " + ActionModel.Type.METHOD + " action");
            return;
        }
        Intent intent = new Intent();
        intent.setPackage(getApplicationContext().getPackageName());
        intent.setAction(INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_ACTION);
        intent.setData(new Uri.Builder()
                .scheme(INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_SCHEME)
                .authority(INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_AUTHORITY)
                .appendPath(method)
                .build());
        intent.putExtra(INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_METHOD, method);
        intent.putExtra(INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_ARG, arg);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    protected static boolean safeDefer(final Runnable runnable, long defer) {
        return sDeferHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error on deferred task", ex);
                }
            }
        }, defer);
    }

    /**
     * Monitors activities lifecycle operations, which are evidences of user interactions.
     * @link http://www.mjbshaw.com/2012/12/determining-if-your-android-application.html
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected static class ActivityLifecycleMonitor implements ActivityLifecycleCallbacks {

        private int createCount;
        private int startCount;
        private int resumeCount;
        private int pausedCount;
        private int stopCount;
        private int destroyCount;

        private long createFirstDate;
        private long startFirstDate;
        private long resumeFirstDate;
        private long pausedLastDate;
        private long stopLastDate;
        private long destroyLastDate;

        private Activity lastResumedActivity;

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (!hasCreatedActivities()) {
                createFirstDate = WonderPush.getTime();
            }
            ++createCount;
            WonderPush.showPotentialNotification(activity, activity.getIntent());
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (createCount == 0) {
                // The monitor was probably setup inside a Activity.onCreate() call
                this.onActivityCreated(activity, null);
            }
            if (!hasStartedActivities()) {
                startFirstDate = WonderPush.getTime();
            }
            ++startCount;
            onInteraction();
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (!hasResumedActivities()) {
                resumeFirstDate = WonderPush.getTime();
            }
            lastResumedActivity = activity;
            ++resumeCount;
            onInteraction();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            ++pausedCount;
            if (!hasResumedActivities()) {
                pausedLastDate = WonderPush.getTime();
            }
            onInteraction();
        }

        @Override
        public void onActivityStopped(Activity activity) {
            ++stopCount;
            if (!hasStartedActivities()) {
                stopLastDate = WonderPush.getTime();
            }
            onInteraction();
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            ++destroyCount;
            if (!hasCreatedActivities()) {
                destroyLastDate = WonderPush.getTime();
            }
            onInteraction();
        }

        protected boolean hasResumedActivities() {
            return resumeCount > pausedCount;
        }

        protected boolean hasStartedActivities() {
            return startCount > stopCount;
        }

        protected boolean hasCreatedActivities() {
            return createCount > destroyCount;
        }

        protected long getCreateFirstDate() {
            return createFirstDate;
        }

        protected long getStartFirstDate() {
            return startFirstDate;
        }

        protected long getResumeFirstDate() {
            return resumeFirstDate;
        }

        protected long getPausedLastDate() {
            return pausedLastDate;
        }

        protected long getStopLastDate() {
            return stopLastDate;
        }

        protected long getDestroyLastDate() {
            return destroyLastDate;
        }

        protected Activity getLastResumedActivity() {
            return lastResumedActivity;
        }

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

        // Only redeclared for package private access
        @Override
        protected List<BasicNameValuePair> getParamsList() {
            return super.getParamsList();
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
            java.util.List<BasicNameValuePair> params = getParamsList();
            for (BasicNameValuePair parameter : params) {
                try {
                    result.put(parameter.getName(), parameter.getValue());
                } catch (JSONException e) {
                    WonderPush.logError("Failed to add parameter " + parameter, e);
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
                } catch (Exception e) {
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
        String mError;

        public Response(String responseContent) {
            try {
                mJson = new JSONObject(responseContent);
            } catch (JSONException e) {
                mError = responseContent;
            }
        }

        public Response(JSONObject responseJson) {
            mJson = responseJson;
        }

        public boolean isError() {
            return mJson == null || mJson.has("error");
        }

        public String getErrorMessage() {
            if (!isError())
                return null;

            if (mJson == null) {
                return mError;
            }

            JSONObject error = mJson.optJSONObject("error");
            if (error == null) {
                return null;
            }
            return error.optString("message", null);
        }

        public int getErrorStatus() {
            if (!isError() || mJson == null)
                return 0;

            JSONObject error = mJson.optJSONObject("error");
            if (error == null) {
                return 0;
            }
            return error.optInt("status", 0);
        }

        public int getErrorCode() {
            if (!isError() || mJson == null)
                return 0;

            JSONObject error = mJson.optJSONObject("error");
            if (error == null) {
                return 0;
            }
            return error.optInt("code", 0);
        }

        public JSONObject getJSONObject() {
            return mJson;
        }

        @Override
        public String toString() {
            if (mJson == null) {
                return mError;
            }
            return mJson.toString();
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
