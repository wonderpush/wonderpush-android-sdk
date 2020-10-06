package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.display.InAppMessagingDisplay;
import com.wonderpush.sdk.push.PushServiceManager;
import com.wonderpush.sdk.push.PushServiceResult;

import com.wonderpush.sdk.remoteconfig.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.wonderpush.sdk.remoteconfig.Constants.REMOTE_CONFIG_EVENTS_BLACK_WHITE_LIST_KEY;

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
 *   make sure to monitor your logs for the WonderPush tag during development,
 *   if things did not went as smoothly as they should have.
 * </p>
 */
public class WonderPush {

    static final String TAG = "WonderPush";
    protected static boolean SHOW_DEBUG = false;
    private static boolean SHOW_DEBUG_OVERRIDDEN = false;

    private static InAppMessaging sInAppMessaging;
    private static Context sApplicationContext;
    protected static Application sApplication;

    private static WonderPushRequestVault sMeasurementsApiRequestVault;
    private static Looper sLooper;
    private static Handler sDeferHandler;
    protected static final ScheduledExecutorService sScheduledExecutor;
    private static PresenceManager sPresenceManager;
    private static RemoteConfigManager sRemoteConfigManager;
    @Nullable
    private static BlackWhiteList sEventsBlackWhiteList = null;

    static {
        sDeferHandler = new Handler(Looper.getMainLooper()); // temporary value until our thread is started
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                sLooper = Looper.myLooper();
                sDeferHandler = new Handler(sLooper);
                for (;;) {
                    try {
                        Looper.loop();
                    } catch (Exception ex) {
                        Log.e(WonderPush.TAG, "Uncaught exception in WonderPush defer handler", ex);
                        continue; // loop on exceptions
                    }
                    break; // allow normal exits
                }
            }
        }, "WonderPush").start();
        sScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private static String sClientId;
    private static String sClientSecret;
    private static String sBaseURL;
    private static boolean sIsInitialized = false;
    private static boolean sIsReady = false;
    private static boolean sIsReachable = false;

    private static boolean sRequiresUserConsent = false;
    private static final WonderPushNotInitializedImpl sNotInitializedImpl = new WonderPushNotInitializedImpl();
    private static IWonderPush sApiImpl = sNotInitializedImpl;

    private static final Map<String, Runnable> sUserConsentDeferred = new TreeMap<>();
    private static final Set<UserConsentListener> sUserConsentListeners = new LinkedHashSet<>();
    interface UserConsentListener {
        void onUserConsentChanged(boolean hasUserConsent);
    }
    static {
        // Add the necessary user consent listener to dequeue sUserConsentDeferred
        addUserConsentListener(new UserConsentListener() {
            @Override
            public void onUserConsentChanged(boolean hasUserConsent) {
                if (hasUserConsent) {
                    synchronized (sUserConsentDeferred) {
                        for (Runnable runnable : sUserConsentDeferred.values()) {
                            WonderPush.safeDefer(runnable, 0);
                        }
                        sUserConsentDeferred.clear();
                    }
                }
            }
        });
    }

    private static boolean sBeforeInitializationUserIdSet = false;
    private static String sBeforeInitializationUserId;

    private static String sIntegrator = null;
    private static AtomicReference<Location> sLocationOverride = null;

    private static WonderPushDelegate sDelegate;

    static final int API_INT = 1;
    static final String API_VERSION = "v" + API_INT;
    static final String SDK_SHORT_VERSION = BuildConfig.VERSION_NAME;
    static final String SDK_VERSION = "Android-" + SDK_SHORT_VERSION;
    private static final String PRODUCTION_API_URL = "https://api.wonderpush.com/" + API_VERSION;
    protected static final String MEASUREMENTS_API_URL = "https://measurements-api.wonderpush.com/v1";

    /**
     * The amount of time in milliseconds a presence is expected to last at most.
     * After this time, user is considered absent if presence hasn't been renewed.
     */
    private static final long PRESENCE_ANTICIPATED_TIME = 30 * 60 * 1000;

    /**
     * The amount of time in milliseconds it takes at most to reach our servers and renew a presence.
     * We'll try to renew the presence this amount of time before the presence expires.
     */
    private static final long PRESENCE_UPDATE_SAFETY_MARGIN = 60 * 1000;

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
     * How long to hold a wakelock at maximum when receiving a push notifications.
     * This wake lock is only used to fetch necessary resources before displaying the notification.
     */
    static final long NOTIFICATION_RECEIVED_WAKELOCK_TIMEOUT = 30 * 1000;

    /**
     * The metadata key name corresponding to the name of the WonderPushInitializer implementation.
     */
    private static final String METADATA_INITIALIZER_CLASS = "wonderpushInitializerClass";

    /**
     * The preference.subscriptionStatus value when notifications are enabled.
     */
    static final String INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTIN = "optIn";

    /**
     * The preference.subscriptionStatus value when notifications are disabled.
     */
    static final String INSTALLATION_PREFERENCES_SUBSCRIPTION_STATUS_OPTOUT = "optOut";

    /**
     * Local intent broadcasted when the WonderPush SDK has been initialized and network is reachable.
     */
    public static final String INTENT_INTIALIZED = "wonderpushInitialized";

    /**
     * Local intent broadcasted when the push token has changed.
     */
    public static final String INTENT_PUSH_TOKEN_CHANGED = "wonderpushPushTokenChanged";

    /**
     * The extra key for the previously known push token, can be null.
     */
    public static final String INTENT_PUSH_TOKEN_CHANGED_EXTRA_OLD_KNOWN_PUSH_TOKEN = "wonderpushOldPushToken";

    /**
     * The extra key for the new push token, can be null.
     */
    public static final String INTENT_PUSH_TOKEN_CHANGED_EXTRA_PUSH_TOKEN = "wonderpushPushToken";

    /**
     * Local intent broadcasted when a push notification created by the WonderPush SDK has been opened.
     */
    public static final String INTENT_NOTIFICATION_OPENED = "wonderpushNotificationOpened";

    /**
     * The extra key for the original received push notification intent in a {@link #INTENT_NOTIFICATION_OPENED} intent.
     */
    public static final String INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION = "wonderpushReceivedPushNotification";

    /**
     * The extra key for the parsed notification in a {@link #INTENT_NOTIFICATION_OPENED} intent.
     */
    public static final String INTENT_NOTIFICATION_OPENED_EXTRA_NOTIFICATION_MODEL = "wonderpushNotificationModel";

    /**
     * The extra key for whether the user clicked the notification or it was automatically opened by the SDK
     * in a {@link #INTENT_NOTIFICATION_OPENED} intent.
     */
    public static final String INTENT_NOTIFICATION_OPENED_EXTRA_FROM_USER_INTERACTION = "wonderpushFromUserInteraction";

    /**
     * The extra key indicating which action button the user clicked on the notification
     * in a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     */
    public static final String INTENT_NOTIFICATION_OPENED_EXTRA_BUTTON_INDEX =
            "wonderpushButtonIndex";

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
     * The first path segment for opening the notification without any UI action, not even the default activity.
     */
    protected static final String INTENT_NOTIFICATION_WILL_OPEN_PATH_NOOP = "noop";

    /**
     * The extra key for the original received push notification intent in a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION =
            INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION;

    /**
     * The extra key for the parsed notification in a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL =
            INTENT_NOTIFICATION_OPENED_EXTRA_NOTIFICATION_MODEL;

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
    @SuppressWarnings("unused")
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE_DATA = "data";

    /**
     * Local intent broadcast when an event is tracked by the WonderPush SDK.
     */
    public static final String INTENT_EVENT_TRACKED = "wonderpushEventTracked";

    /**
     * Intent extra key holding the type of event being tracked.
     */
    public static final String INTENT_EVENT_TRACKED_EVENT_TYPE = "eventType";

    /**
     * Intent extra key holding custom data serialized as JSON of event being tracked.
     */
    public static final String INTENT_EVENT_TRACKED_CUSTOM_DATA = "customData";

    /**
     * Local intent broadcasted when a resource has been successfully preloaded.
     */
    protected static final String INTENT_RESOURCE_PRELOADED = "wonderpushResourcePreloaded";

    /**
     * The extra key for the path of a preloaded resource in a {@link #INTENT_RESOURCE_PRELOADED} intent.
     */
    protected static final String INTENT_RESOURCE_PRELOADED_EXTRA_PATH = "wonderpushResourcePreloadedPath";

    /**
     * Intent scheme for push notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_SCHEME = "wonderpush";

    /**
     * Intent authority for push notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_AUTHORITY = "notification";

    /**
     * Intent data type for push notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_TYPE = "application/vnd.wonderpush.notification";

    /**
     * Intent query parameter key for push notification data when the user clicks the notification.
     */
    protected static final String INTENT_NOTIFICATION_QUERY_PARAMETER = "body";

    /**
     * Intent query parameter key for identifying which notification action button the user clicked.
     */
    protected static final String INTENT_NOTIFICATION_QUERY_PARAMETER_BUTTON_INDEX = "buttonIndex";

    /**
     * Intent query parameter key for identifying which local notification id the user clicked.
     */
    protected static final String INTENT_NOTIFICATION_QUERY_PARAMETER_LOCAL_NOTIFICATION_ID = "localNotificationId";

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

    static {
        // Ensure we get an @APP_OPEN with deferred initialization
        addUserConsentListener(new UserConsentListener() {
            @Override
            public void onUserConsentChanged(boolean hasUserConsent) {
                if (hasUserConsent) {
                    onInteraction(false);
                }
            }
        });
    }

    protected WonderPush() {
        throw new IllegalAccessError("You should not instantiate this class!");
    }

    static void applyOverrideLogging(Boolean value) {
        if (value != null) {
            Log.d(TAG, "OVERRIDE setLogging(" + value + ")");
            setLogging(value);
            SHOW_DEBUG_OVERRIDDEN = true;
        } else {
            SHOW_DEBUG_OVERRIDDEN = false;
        }
    }

    /**
     * Whether to enable debug logging.
     *
     * You should not do this in production builds.
     *
     * @param enable {@code true} to enable debug logs.
     */
    @SuppressWarnings("unused")
    public static void setLogging(boolean enable) {
        if (!SHOW_DEBUG_OVERRIDDEN) {
            WonderPush.SHOW_DEBUG = enable;
        }
    }

    /**
     * Whether debug logging is enabled.
     */
    public static boolean getLogging() {
        return WonderPush.SHOW_DEBUG;
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

    /**
     * @param activity
     *            The current {@link Activity}.
     * @param intent
     *            The intent the activity received.
     *
     * @return <code>true</code> if handled, <code>false</code> otherwise.
     */
    static boolean showPotentialNotification(final Activity activity, Intent intent) {
        try {
            return NotificationManager.showPotentialNotification(activity, intent);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while showing potential notification", e);
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
    protected static void get(String resource, Request.Params params,
            ResponseHandler responseHandler) {
        ApiClient.get(resource, params, responseHandler);
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
    protected static void post(String resource, Request.Params params,
            ResponseHandler responseHandler) {
        ApiClient.post(resource, params, responseHandler);
    }

    /**
     * A POST request that is guaranteed to be executed when a network
     * connection is present, surviving application reboot. The responseHandler
     * will be called only if the network is present when the request is first run.
     *
     * @param resource
     *            The resource path, starting with /.
     * @param params
     *            The request parameters. Only serializable parameters are
     *            guaranteed to survive a network error or device reboot.
     */
    protected static void postEventually(String resource,
            Request.Params params) {
        ApiClient.postEventually(resource, params);
    }

    private static WonderPushRequestVault getMeasurementsApiRequestVault() {
        if (null == sMeasurementsApiRequestVault) {
            sMeasurementsApiRequestVault = new WonderPushRequestVault(WonderPushJobQueue.getMeasurementsApiQueue(), new WonderPushRequestVault.RequestExecutor() {
                @Override
                public void execute(Request request) {
                    MeasurementsApiClient.execute(request);
                }
            });
        }
        return sMeasurementsApiRequestVault;
    }

    /**
     * A POST request that is guaranteed to be executed when a network
     * connection is present, surviving application reboot. The responseHandler
     * will be called only if the network is present when the request is first run.
     *
     * @param resource
     * @param params
     */
    protected static void postEventuallyWithMeasurementsApiClient(String resource, Request.Params params) {
        final Request request = new Request(WonderPushConfiguration.getUserId(), ApiClient.HttpMethod.POST, resource, params, null);
        getMeasurementsApiRequestVault().put(request, 0);
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
    protected static void put(String resource, Request.Params params,
            ResponseHandler responseHandler) {
        ApiClient.put(resource, params, responseHandler);
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
            ResponseHandler responseHandler) {
        ApiClient.delete(resource, responseHandler);
    }

    /**
     * Disables the collection of the user's geolocation.
     */
    public static void disableGeolocation() {
        setGeolocation(null);
    }

    /**
     * Enables the collection of the user's geolocation.
     *
     * You still need the appropriate geolocation permissions in your AndroidManifest.xml to be able to read the user's location.
     */
    public static void enableGeolocation() {
        sLocationOverride = null;
    }

    /**
     * Overrides the user's geolocation.
     *
     * <p>Using this method you can have the user's location be set to wherever you want.
     * This may be useful to use a pre-recorded location.</p>
     *
     * <p>Note that the value is not persisted.</p>
     *
     * @param location The location to use as the user's current geolocation.
     *                 Using {@code null} has the same effect as calling {@link #disableGeolocation()}.
     */
    public static void setGeolocation(Location location) {
        sLocationOverride = new AtomicReference<>(location == null ? null : new Location(location));
    }

    /**
     * Returns the last known location of the {@link LocationManager}
     * or null if permission was not given.
     */
    @SuppressLint("MissingPermission")
    protected static Location getLocation() {
        Context applicationContext = getApplicationContext();

        if (applicationContext == null)
            return null;

        AtomicReference<Location> locationOverrideRef = sLocationOverride;
        if (locationOverrideRef != null) {
            Location override = locationOverrideRef.get();
            return override;
        }

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

                // Skip locations old enough to belong to an older session
                if (location.getTime() < System.currentTimeMillis() - WonderPush.DIFFERENT_SESSION_REGULAR_MIN_TIME_GAP) {
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
     * Overrides the user's locale.
     *
     * <p>You should use an {@code xx-XX} form of RFC 1766, composed of a lowercase ISO 639-1 language code,
     * an underscore or a dash, and an uppercase ISO 3166-1 alpha-2 country code.</p>
     *
     * <p>Defaults to getting the language and country codes from the system default locale.</p>
     *
     * @param locale The locale to use as the user's locale.
     *               Use {@code null} to disable the override.
     */
    public static void setLocale(String locale) {
        if (locale != null) {
            // Validate locale against simple expected values,
            // but accept any input as is
            String localeUC = locale.toUpperCase();
            if (locale.length() != 2 && locale.length() != 5) {
                Log.w(TAG, "The given locale \"" + locale + "\" is not of the form xx-XX of RFC 1766");
            } else if (!(
                    localeUC.charAt(0) >= 'A' && localeUC.charAt(0) <= 'Z'
                    && localeUC.charAt(1) >= 'A' && localeUC.charAt(1) <= 'Z'
                    && (locale.length() == 2 || locale.length() == 5
                        && (localeUC.charAt(2) == '-' || localeUC.charAt(2) == '_')
                        && localeUC.charAt(3) >= 'A' && localeUC.charAt(3) <= 'Z'
                        && localeUC.charAt(4) >= 'A' && localeUC.charAt(4) <= 'Z'
                    )
            )) {
                Log.w(TAG, "The given locale \"" + locale + "\" is not of the form xx-XX of RFC 1766");
            } else {
                // Normalize simple expected values into xx_XX
                locale = locale.substring(0, 2).toLowerCase() + (locale.length() == 5 ? "_" + locale.substring(3, 5).toUpperCase() : "");
            }
        }
        WonderPushConfiguration.setLocale(locale);
        refreshPreferencesAndConfiguration(false);
    }

    /**
     * Gets the user's locale, either as previously stored, or as guessed from the system.
     *
     * @return The user's locale.
     * @see WonderPush#setLocale(String)
     */
    public static String getLocale() {
        String rtn = WonderPushConfiguration.getLocale();
        if (rtn == null) {
            Locale locale = Locale.getDefault();
            if (locale != null) {
                String language = locale.getLanguage();
                if (!TextUtils.isEmpty(language)) {
                    language = language.toLowerCase(Locale.ENGLISH);

                    String country = locale.getCountry();
                    if (TextUtils.isEmpty(country)) {
                        rtn = language;
                    } else {
                        country = country.toUpperCase(Locale.ENGLISH);
                        rtn = language + "_" + country;
                    }
                }
            }
        }
        return rtn;
    }

    /**
     * Overrides the user's country.
     *
     * <p>You should use an ISO 3166-1 alpha-2 country code.</p>
     *
     * <p>Defaults to getting the country code from the system default locale.</p>
     *
     * @param country The country to use as the user's country.
     *                Use {@code null} to disable the override.
     */
    public static void setCountry(String country) {
        if (country != null) {
            // Validate against simple expected values,
            // but accept any input as is
            String countryUC = country.toUpperCase();
            if (country.length() != 2) {
                Log.w(TAG, "The given country \"" + country + "\" is not of the form XX of ISO 3166-1 alpha-2");
            } else if (!(
                    countryUC.charAt(0) >= 'A' && countryUC.charAt(0) <= 'Z'
                    && countryUC.charAt(1) >= 'A' && countryUC.charAt(1) <= 'Z'
            )) {
                Log.w(TAG, "The given country \"" + country + "\" is not of the form XX of ISO 3166-1 alpha-2");
            } else {
                // Normalize simple expected value into XX
                country = countryUC;
            }
        }
        WonderPushConfiguration.setCountry(country);
        refreshPreferencesAndConfiguration(false);
    }

    /**
     * Gets the user's country, either as previously stored, or as guessed from the system.
     *
     * @return The user's country.
     * @see WonderPush#setCountry(String)
     */
    public static String getCountry() {
        String rtn = WonderPushConfiguration.getCountry();
        if (rtn == null) {
            rtn = Locale.getDefault().getCountry();
            if (TextUtils.isEmpty(rtn)) {
                rtn = null;
            } else {
                rtn = rtn.toUpperCase();
            }
        }
        return rtn;
    }

    /**
     * Overrides the user's currency.
     *
     * <p>You should use an ISO 4217 currency code.</p>
     *
     * <p>Defaults to getting the currency code from the system default locale.</p>
     *
     * @param currency The currency to use as the user's currency.
     *                 Use {@code null} to disable the override.
     */
    public static void setCurrency(String currency) {
        if (currency != null) {
            // Validate against simple expected values,
            // but accept any input as is
            String currencyUC = currency.toUpperCase();
            if (currency.length() != 3) {
                Log.w(TAG, "The given currency \"" + currency + "\" is not of the form XXX of ISO 4217");
            } else if (!(
                    currencyUC.charAt(0) >= 'A' && currencyUC.charAt(0) <= 'Z'
                    && currencyUC.charAt(1) >= 'A' && currencyUC.charAt(1) <= 'Z'
                    && currencyUC.charAt(2) >= 'A' && currencyUC.charAt(2) <= 'Z'
            )) {
                Log.w(TAG, "The given currency \"" + currency + "\" is not of the form XXX of ISO 4217");
            } else {
                // Normalize simple expected value into XXX
                currency = currencyUC;
            }
        }
        WonderPushConfiguration.setCurrency(currency);
        refreshPreferencesAndConfiguration(false);
    }

    /**
     * Gets the user's currency, either as previously stored, or as guessed from the system.
     *
     * @return The user's currency.
     * @see WonderPush#setCurrency(String)
     */
    public static String getCurrency() {
        String rtn = WonderPushConfiguration.getCurrency();
        if (rtn == null) {
            try {
                Currency currency = Currency.getInstance(Locale.getDefault());
                if (currency == null) return null;
                rtn = currency.getCurrencyCode();
                if (TextUtils.isEmpty(rtn)) {
                    rtn = null;
                } else {
                    rtn = rtn.toUpperCase();
                }
            } catch (Exception e) { // mostly for IllegalArgumentException
            }
        }
        return rtn;
    }

    /**
     * Overrides the user's timeZone.
     *
     * <p>You should use an IANA time zone database codes, {@code Continent/Country} style preferably,
     * abbreviations like {@code CET}, {@code PST}, {@code UTC}, which have the drawback of changing on daylight saving transitions.</p>
     *
     * <p>Defaults to getting the time zone code from the system default locale.</p>
     *
     * @param timeZone The time zone to use as the user's time zone.
     *                Use {@code null} to disable the override.
     */
    public static void setTimeZone(String timeZone) {
        if (timeZone != null) {
            // Validate against simple expected values,
            // but accept any input as is
            String timeZoneUC = timeZone.toUpperCase();
            if (timeZone.indexOf('/') >= 0) {
                if (!(timeZone.startsWith("Africa/")
                        || timeZone.startsWith("America/")
                        || timeZone.startsWith("Antarctica/")
                        || timeZone.startsWith("Asia/")
                        || timeZone.startsWith("Atlantic/")
                        || timeZone.startsWith("Australia/")
                        || timeZone.startsWith("Etc/")
                        || timeZone.startsWith("Europe/")
                        || timeZone.startsWith("Indian/")
                        || timeZone.startsWith("Pacific/")
                ) || timeZone.endsWith("/")) {
                    Log.w(TAG, "The given time zone \"" + timeZone + "\" is not of the form Continent/Country or ABBR of IANA time zone database codes");
                }
            } else {
                boolean allLetters = true;
                for (int i = 0; i < timeZoneUC.length(); ++i) {
                    if (timeZoneUC.charAt(i) < 'A' || timeZoneUC.charAt(i) > 'Z') {
                        allLetters = false;
                        break;
                    }
                }
                if (!allLetters) {
                    Log.w(TAG, "The given time zone \"" + timeZone + "\" is not of the form Continent/Country or ABBR of IANA time zone database codes");
                } else if (!(timeZone.length() == 1
                        || timeZoneUC.endsWith("T")
                        || timeZoneUC.equals("UTC")
                        || timeZoneUC.equals("AOE")
                        || timeZoneUC.equals("MSD")
                        || timeZoneUC.equals("MSK")
                        || timeZoneUC.equals("WIB")
                        || timeZoneUC.equals("WITA"))) {
                    Log.w(TAG, "The given time zone \"" + timeZone + "\" is not of the form Continent/Country or ABBR of IANA time zone database codes");
                } else {
                    // Normalize abbreviations in uppercase
                    timeZone = timeZoneUC;
                }
            }
        }
        WonderPushConfiguration.setTimeZone(timeZone);
        refreshPreferencesAndConfiguration(false);
    }

    /**
     * Gets the user's time zone, either as previously stored, or as guessed from the system.
     *
     * @return The user's time zone.
     * @see WonderPush#setTimeZone(String)
     */
    public static String getTimeZone() {
        String rtn = WonderPushConfiguration.getTimeZone();
        if (rtn == null) {
            rtn = TimeZone.getDefault().getID();
        }
        return rtn;
    }

    /**
     * Returns the latest known properties attached to the current installation object stored by WonderPush.
     *
     * <p>Returns an empty {@code JSONObject} if called without required user consent.</p>
     */
    public static JSONObject getProperties() {
        return sApiImpl.getProperties();
    }

    /**
     * Update the properties attached to the current installation object stored by WonderPush.
     *
     * <p>
     *   In order to remove a value, don't forget to use the
     *   <a href="http://d.android.com/reference/org/json/JSONObject.html#NULL">JSONObject.NULL</a>
     *   object as value.
     * </p>
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param properties
     *            The partial object containing only the properties to update.
     */
    public static void putProperties(JSONObject properties) {
        sApiImpl.putProperties(properties);
    }

    /**
     * Sets the value to a given property attached to the current installation object stored by WonderPush.
     * The previous value is replaced entirely.
     * The value can be a String, Boolean, Number (coerced to Long or Double), JSONObject, JSONArray, or JSONObject.NULL (which has the same effect as {@link #unsetProperty(String)}).
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param field The name of the property to set
     * @param value The value to be set, can be an array or Collection
     */
    public static void setProperty(String field, Object value) {
        sApiImpl.setProperty(field, value);
    }

    /**
     * Removes the value of a given property attached to the current installation object stored by WonderPush.
     * The previous value is replaced with {@link JSONObject#NULL}.
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param field The name of the property to set
     */
    public static void unsetProperty(String field) {
        sApiImpl.unsetProperty(field);
    }

    /**
     * Adds the value to a given property attached to the current installation object stored by WonderPush.
     * The stored value is made an array if not already one.
     * If the given value is an array, all its values are added.
     * If a value is already present in the stored value, it won't be added.
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param field The name of the property to add values to
     * @param value The value(s) to be added, can be an array
     */
    public static void addProperty(String field, Object value) {
        sApiImpl.addProperty(field, value);
    }

    /**
     * Removes the value from a given property attached to the current installation object stored by WonderPush.
     * The stored value is made an array if not already one.
     * If the given value is an array, all its values are removed.
     * If a value is present multiple times in the stored value, they will all be removed.
     *
     * @param field The name of the property to remove values from
     * @param value The value(s) to be removed, can be an array
     */
    public static void removeProperty(String field, Object value) {
        sApiImpl.removeProperty(field, value);
    }

    /**
     * Returns the value of a given property attached to the current installation object stored by WonderPush.
     * If the property stores an array, only the first value is returned.
     * This way you don't have to deal with potential arrays if that property is not supposed to hold one.
     * Returns {@link JSONObject#NULL} instead of {@code null} if the property is absent or has an empty array value.
     *
     * @param field The name of the property to read values from
     * @return {@link JSONObject#NULL} or a single value stored in the property, never a {@link JSONArray} or {@code null}
     */
    public static Object getPropertyValue(String field) {
        return sApiImpl.getPropertyValue(field);
    }

    /**
     * Returns an immutable list of the values of a given property attached to the current installation object stored by WonderPush.
     * If the property does not store an array, a list is returned nevertheless.
     * This way you don't have to deal with potential scalar values if that property is supposed to hold an array.
     * Returns an empty list instead of {@link JSONObject#NULL} or {@code null} if the property is absent.
     * Returns a list wrapping any scalar value held by the property.
     *
     * <p>Note, the returned value is an <em>immutable</em> list.</p>
     *
     * @param field The name of the property to read values from
     * @return A possibly empty {@link org.json.JSONArray} of the values stored in the property, but never {@link JSONObject#NULL} nor {@code null}
     */
    public static List<Object> getPropertyValues(String field) {
        return sApiImpl.getPropertyValues(field);
    }

    /**
     * Returns the latest known custom properties attached to the current installation object stored by WonderPush.
     *
     * <p>Use {@link #getProperties()} instead.</p>
     *
     * <p>Returns an empty {@code JSONObject} if called without required user consent.</p>
     * @see #getProperties()
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static synchronized JSONObject getInstallationCustomProperties() {
        return sApiImpl.getInstallationCustomProperties();
    }

    /**
     * Update the custom properties attached to the current installation object stored by WonderPush.
     *
     * <p>Use {@link #putProperties(JSONObject)} instead.</p>
     *
     * <p>
     *   In order to remove a value, don't forget to use the
     *   <a href="http://d.android.com/reference/org/json/JSONObject.html#NULL">JSONObject.NULL</a>
     *   object as value.
     * </p>
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param customProperties
     *            The partial object containing only the properties to update.
     * @see #putProperties(JSONObject)
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static synchronized void putInstallationCustomProperties(JSONObject customProperties) {
        sApiImpl.putInstallationCustomProperties(customProperties);
    }

    static synchronized void receivedFullInstallationFromServer(JSONObject installation) {
        WonderPush.logDebug("Synchronizing installation custom fields");
        WonderPush.logDebug("Received installation: " + installation);
        try {
            JSONSyncInstallation.forCurrentUser().receiveState(installation, false);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to receive custom from server", ex);
        }
    }

    /**
     * Send an event to be tracked to WonderPush.
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param type
     *            The event type, or name.
     *            Event types starting with an {@code @} character are reserved.
     */
    @SuppressWarnings("unused")
    public static void trackEvent(String type) {
        sApiImpl.trackEvent(type);
    }

    /**
     * Send an event to be tracked to WonderPush.
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param type
     *            The event type, or name.
     *            Event types starting with an {@code @} character are reserved.
     * @param attributes
     *            A JSON object containing attributes to be attached to the event.
     *            Prefer using a few attributes over a plethora of event type variants.
     */
    public static void trackEvent(String type, JSONObject attributes) {
        sApiImpl.trackEvent(type, attributes);
    }

    static void trackEvent(String type, JSONObject eventData, JSONObject attributes) {
        if (type == null || type.length() == 0 || type.charAt(0) == '@') {
            throw new IllegalArgumentException("Bad event type");
        }
        sendEvent(type, eventData, attributes);
    }

    static void trackInternalEvent(String type, JSONObject eventData) {
        trackInternalEvent(type, eventData, null);
    }

    static void trackInternalEventWithMeasurementsApi(String type, JSONObject eventData) {
        trackInternalEventWithMeasurementsApi(type, eventData, null);
    }

    static void trackInternalEventWithMeasurementsApi(String type, JSONObject eventData, JSONObject customData) {
        if (type.charAt(0) != '@') {
            throw new IllegalArgumentException("This method must only be called for internal events, starting with an '@'");
        }
        sendEvent(type, eventData, customData, true);
    }

    static void trackInternalEvent(String type, JSONObject eventData, JSONObject customData) {
        if (type.charAt(0) != '@') {
            throw new IllegalArgumentException("This method must only be called for internal events, starting with an '@'");
        }
        sendEvent(type, eventData, customData);
    }

    private static void sendEvent(String type, JSONObject eventData, JSONObject customData) {
        sendEvent(type, eventData, customData, false);
    }

    private static void sendEvent(String type, JSONObject eventData, JSONObject customData, boolean useMeasurementsApi) {
        if (!hasUserConsent()) {
            logError("Not tracking event without user consent. type=" + type + ", data=" + eventData + " custom=" + customData);
            return;
        }

        if (sEventsBlackWhiteList != null && !sEventsBlackWhiteList.allow(type)) {
            logError("Not tracking event forbidden by config. type=" + type + ", data=" + eventData + " custom=" + customData);
            return;
        }

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
                event.put("actionDate", TimeSync.getTime());
            }
        } catch (JSONException ex) {
            WonderPush.logError("Error building event object body", ex);
        }

        Request.Params parameters = new Request.Params();
        parameters.put("body", event.toString());

        // Remember
        WonderPushConfiguration.rememberTrackedEvent(event);

        // Broadcast locally that an event was tracked
        Intent eventTrackedIntent = new Intent(WonderPush.INTENT_EVENT_TRACKED);
        eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_EVENT_TYPE, type);
        if (customData != null) {
            eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_CUSTOM_DATA, customData.toString());
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(eventTrackedIntent);

        if (useMeasurementsApi) {
            postEventuallyWithMeasurementsApiClient("/events", parameters);
        } else {
            postEventually(eventEndpoint, parameters);
        }
    }

    /**
     * Add one or more tags to the current installation object stored by WonderPush.
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param tag
     *      The tags to add to the installation.
     */
    public static void addTag(String... tag) {
        sApiImpl.addTag(tag);
    }

    /**
     * Remove one or more tags from the current installation object stored by WonderPush.
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param tag
     *      The tags to remove from the installation.
     */
    public static void removeTag(String... tag) {
        sApiImpl.removeTag(tag);
    }

    /**
     * Remove all tags from the current installation object stored by WonderPush.
     *
     * <p>Does nothing if called without required user consent.</p>
     */
    public static void removeAllTags() {
        sApiImpl.removeAllTags();
    }

    /**
     * Returns all the tags of the current installation object stored by WonderPush.
     *
     * @return
     *      A copy of the set of tags attached to the installation.
     *      Never returns {@code null}.
     */
    public static Set<String> getTags() {
        return sApiImpl.getTags();
    }

    /**
     * Test whether the current installation has the given tag attached to it.
     * @param tag
     *      The tag to test.
     * @return
     *      {@code true} if the given tag is attached to the installation, {@code false} otherwise.
     */
    public static boolean hasTag(String tag) {
        return sApiImpl.hasTag(tag);
    }

    protected static void onInteraction(boolean leaving) {
        if (!hasUserConsent()) {
            logDebug("onInteraction ignored without user consent");
            return;
        }
        long lastInteractionDate = WonderPushConfiguration.getLastInteractionDate();
        long lastAppOpenDate = WonderPushConfiguration.getLastAppOpenDate();
        long lastAppCloseDate = WonderPushConfiguration.getLastAppCloseDate();
        JSONObject lastReceivedNotificationInfo = WonderPushConfiguration.getLastReceivedNotificationInfoJson();
        if (lastReceivedNotificationInfo == null) lastReceivedNotificationInfo = new JSONObject();
        long lastReceivedNotificationDate = lastReceivedNotificationInfo.optLong("actionDate", Long.MAX_VALUE);
        JSONObject lastOpenedNotificationInfo = WonderPushConfiguration.getLastOpenedNotificationInfoJson();
        if (lastOpenedNotificationInfo == null) lastOpenedNotificationInfo = new JSONObject();
        long lastOpenedNotificationDate = lastOpenedNotificationInfo.optLong("actionDate", Long.MAX_VALUE);
        long now = TimeSync.getTime();

        boolean shouldInjectAppOpen =
                now - lastInteractionDate >= DIFFERENT_SESSION_REGULAR_MIN_TIME_GAP
                || (
                        lastReceivedNotificationDate > lastInteractionDate
                        && now - lastInteractionDate >= DIFFERENT_SESSION_NOTIFICATION_MIN_TIME_GAP
                )
        ;

        if (leaving) {

            if (shouldInjectAppOpen) {
                // Keep the old date as this new interaction is only for a closing activity
            } else {
                // Note the current time as the most accurate hint of last interaction
                WonderPushConfiguration.setLastInteractionDate(now);
            }

        } else {

            if (shouldInjectAppOpen) {
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
                    // trackInternalEvent("@APP_CLOSE", closeInfo);
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
                // Presence
                try {
                    PresenceManager.PresencePayload presence = getPresenceManager().presenceDidStart();
                    openInfo.put("presence", presence.toJSONObject());
                } catch (InterruptedException e) {
                    WonderPush.logError("Could not start presence", e);
                } catch (JSONException e) {
                    WonderPush.logError("Could not serialize presence", e);
                }
                trackInternalEvent("@APP_OPEN", openInfo);
                WonderPushConfiguration.setLastAppOpenDate(now);
                WonderPushConfiguration.setLastAppOpenInfoJson(openInfo);
            }

            WonderPushConfiguration.setLastInteractionDate(now);

        }
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
    @SuppressWarnings("unused")
    public static boolean isReady() {
        return sIsReady;
    }

    /**
     * Initialize WonderPush.
     *
     * <p>
     *     Using automatic initialization, you do not need to take care of this yourself.
     *     You must otherwise call this method before using the SDK.
     *     A good place for that is in the {@link Application#onCreate()} of your {@link Application} class.
     * </p>
     *
     * @param context
     *            And {@link Activity} or {@link Application} context.
     */
    @SuppressWarnings("unused")
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
     *     Using automatic initialization, you do not need to take care of this yourself.
     * </p>
     *
     * <p>
     *   Prefer calling the simpler {@link WonderPush#initialize(Context)} function directly, as it will
     *   instantiate your {@link WonderPushInitializer} implementation which will in turn call this function.
     *   This way you concentrate the retrieval of your credentials from secure storage in a single location.
     * </p>
     *
     * @param context
     *            The main {@link Activity} of your application, or failing that, the {@link Application} context.
     * @param clientId
     *            The clientId of your application.
     * @param clientSecret
     *            The clientSecret of your application.
     */
    public static void initialize(final Context context, final String clientId, String clientSecret) {
        try {
            WonderPush.logDebug("initialize(" + context.getClass().getSimpleName() + ", " + clientId + ", <redacted clientSecret>)");
            if (!sIsInitialized || (
                    clientId != null && clientSecret != null && (!clientId.equals(sClientId) || !clientSecret.equals(sClientSecret))
            )) {

                sIsInitialized = false;
                setNetworkAvailable(false);

                sApplicationContext = context.getApplicationContext();
                sClientId = clientId;
                sClientSecret = clientSecret;
                sBaseURL = PRODUCTION_API_URL;
                OkHttpRemoteConfigFetcher fetcher = new OkHttpRemoteConfigFetcher(clientId, (Runnable r, long defer) -> {
                    WonderPush.safeDefer(r, defer);
                });
                SharedPreferencesRemoteConfigStorage storage = new SharedPreferencesRemoteConfigStorage(clientId, context);
                sRemoteConfigManager = new RemoteConfigManager(fetcher, storage, context);

                PushServiceManager.initialize(getApplicationContext());
                WonderPushConfiguration.initialize(getApplicationContext());
                WonderPushUserPreferences.initialize();
                applyOverrideLogging(WonderPushConfiguration.getOverrideSetLogging());
                JSONSyncInstallation.setDisabled(true);
                ApiClient.setDisabled(true);
                MeasurementsApiClient.setDisabled(true);
                JSONSyncInstallation.initialize();
                WonderPushRequestVault.initialize();
                initializeInAppMessaging(context);

                // Setup a remote config handler to execute as soon as we get the config
                // and everytime the config changes.
                final RemoteConfigHandler remoteConfigHandler = new RemoteConfigHandler() {
                    @Override
                    public void handle(@javax.annotation.Nullable RemoteConfig config, @javax.annotation.Nullable Throwable error) {
                        if (config == null) return;
                        JSONSyncInstallation.setDisabled(config.getData().optBoolean(Constants.REMOTE_CONFIG_DISABLE_JSON_SYNC_KEY, false));
                        if (!JSONSyncInstallation.isDisabled()) JSONSyncInstallation.flushAll();
                        ApiClient.setDisabled(config.getData().optBoolean(Constants.REMOTE_CONFIG_DISABLE_API_CLIENT_KEY, false));
                        MeasurementsApiClient.setDisabled(config.getData().optBoolean(Constants.REMOTE_CONFIG_DISABLE_MEASUREMENTS_API_CLIENT_KEY, false));
                        updateEventsBlackWhiteList(config);
                    }
                };
                // Read the config right away
                ensureConfigurationFetched(remoteConfigHandler, 10000);
                // Call the handler when the config changes
                LocalBroadcastManager.getInstance(context).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String configString = intent.getStringExtra(Constants.EXTRA_REMOTE_CONFIG);
                        if (configString != null) {
                            RemoteConfig config = RemoteConfig.fromString(configString);
                            remoteConfigHandler.handle(config, null);
                        }
                    }
                }, new IntentFilter(Constants.INTENT_REMOTE_CONFIG_UPDATED));

                initForNewUser(sBeforeInitializationUserIdSet
                        ? sBeforeInitializationUserId
                        : WonderPushConfiguration.getUserId());

                sIsInitialized = true;
                hasUserConsentChanged(hasUserConsent()); // make sure to set sIsInitialized=true before

                // Permission checks
                if (context.getPackageManager().checkPermission(android.Manifest.permission.INTERNET, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Missing INTERNET permission. Add <uses-permission android:name=\"android.permission.INTERNET\" /> under <manifest> in your AndroidManifest.xml");
                }
                if (sLocationOverride == null) {
                    if (context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED
                            && context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Log.w(TAG, "Permissions ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION have not been declared or granted yet. Make sure you declare <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /> under <manifest> in your AndroidManifest.xml (you can add either or both), and call ActivityCompat.requestPermissions() to request the permission at runtime");
                        } else {
                            Log.w(TAG, "Missing ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION permission. Add <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /> under <manifest> in your AndroidManifest.xml (you can add either or both)");
                        }
                    } else if (context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Only ACCESS_COARSE_LOCATION permission is granted. For more precision, you should strongly consider adding <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /> under <manifest> in your AndroidManifest.xml");
                    }
                }
            }

            initializeForApplication(context);
            initializeForActivity(context);
            refreshPreferencesAndConfiguration(false);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while initializing the SDK", e);
        }
    }

    private static void ensureConfigurationFetched(final RemoteConfigHandler handler, long delay) {
        if (sRemoteConfigManager == null) return;
        sRemoteConfigManager.read(new RemoteConfigHandler() {
            @Override
            public void handle(@javax.annotation.Nullable RemoteConfig config, @javax.annotation.Nullable Throwable error) {
                if (config == null) {
                    safeDefer(() -> {
                        ensureConfigurationFetched(handler, delay);
                    }, delay);
                    return;
                }
                handler.handle(config, error);
            }
        });
    }
    private static void initForNewUser(final String userId) {
        WonderPush.logDebug("initForNewUser(" + userId + ")");
        sIsReady = false;
        WonderPushConfiguration.changeUserId(userId);
        // Wait for SDK to be initialized and fetch anonymous token if needed.
        WonderPush.safeDeferWithConsent(new Runnable() {
            @Override
            public void run() {
                if (isInitialized()) {
                    final Runnable init = new Runnable() {
                        @Override
                        public void run() {
                            refreshPreferencesAndConfiguration(false);
                            sIsReady = true;
                            Intent broadcast = new Intent(INTENT_INTIALIZED);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);
                        }
                    };
                    boolean isFetchingToken = ApiClient.fetchAnonymousAccessTokenIfNeeded(userId, new ResponseHandler() {
                        @Override
                        public void onFailure(Throwable e, Response errorResponse) {
                        }
                        @Override
                        public void onSuccess(Response response) {
                            init.run();
                        }
                    });
                    if (!isFetchingToken) {
                        init.run();
                    }
                } else {
                    WonderPush.safeDefer(this, 100);
                }
            }
        }, "initForNewUser"); // using this constant id we ensure we only initialize for the last used userId
    }

    protected static void initializeForApplication(Context context) {
        context = context.getApplicationContext();
        if (sApplication != null || !(context instanceof Application)) {
            return;
        }
        sApplication = (Application) context;
        ActivityLifecycleMonitor.monitorActivitiesLifecycle();
    }

    protected static void initializeForActivity(Context context) {
        if (!(context instanceof Activity)) {
            return;
        }
        Activity activity = (Activity) context;

        ActivityLifecycleMonitor.addTrackedActivity(activity);

        if (hasUserConsent()) {
            showPotentialNotification(activity, activity.getIntent());
            onInteraction(false); // keep after onCreateMainActivity() as a possible received notification's information is needed
        }
    }

    protected static void refreshPreferencesAndConfiguration(boolean force) {
        // Refresh core properties
        InstallationManager.updateInstallationCoreProperties(WonderPush.getApplicationContext());

        // Refresh push token
        String oldRegistrationId = WonderPushConfiguration.getGCMRegistrationId();
        if (force) {
            // Avoid depending on the success of getting a new registration id when forcing the refresh
            WonderPushConfiguration.setGCMRegistrationId(null);
            PushServiceResult cachedPushServiceResult = new PushServiceResult();
            cachedPushServiceResult.setData(oldRegistrationId);
            cachedPushServiceResult.setService(WonderPushConfiguration.getGCMRegistrationService());
            cachedPushServiceResult.setSenderIds(WonderPushConfiguration.getGCMRegistrationSenderIds());
            PushServiceManager.onResult(cachedPushServiceResult);
        } else {
            PushServiceManager.refreshSubscription();
        }

        // Refresh preferences
        boolean notificationEnabled = WonderPushConfiguration.getNotificationEnabled();
        if (force) {
            WonderPushConfiguration.setNotificationEnabled(!notificationEnabled);
        }
        if (notificationEnabled) {
            WonderPush.subscribeToNotifications();
        } else {
            WonderPush.unsubscribeFromNotifications();
        }
    }

    static void initializeInAppMessaging(Context context) {
        Application application = (Application)context.getApplicationContext();
        if (sInAppMessaging == null) {
            sInAppMessaging = InAppMessaging.initialize(application, new InternalEventTracker(), new InAppMessaging.InAppMessagingConfiguration() {
                @Override
                public boolean inAppViewedReceipts() {
                    Boolean b = WonderPushConfiguration.getOverrideNotificationReceipt();
                    if (b != null) return b;
                    return false;
                }

                @Override
                public void fetchInAppConfig(InAppMessaging.JSONObjectHandler handler) {
                    if (sRemoteConfigManager == null) {
                        handler.handle(null, null);
                        return;
                    }
                    sRemoteConfigManager.read((RemoteConfig config, Throwable error) -> {
                        handler.handle(config != null ? config.getData().optJSONObject("inAppConfig") : null, error);
                    });
                }

            });
        }
        InAppMessagingDisplay.initialize(application, sInAppMessaging);
    }
    /**
     * @see #ensureInitialized(Context, boolean)
     * @param context
     *            The main {@link Activity} of your application, or failing that, the {@link Application} context.
     * @return {@code true} if no error happened, {@code false} otherwise
     */
    static boolean ensureInitialized(Context context) {
        return ensureInitialized(context, false);
    }

    /**
     * @param context
     *            The main {@link Activity} of your application, or failing that, the {@link Application} context.
     * @param fromInitProvider
     *            Whether we are executed in the context of the {@link WonderPushInitProvider} or any place else from the SDK.
     * @return {@code true} if no error happened, {@code false} otherwise
     */
    static boolean ensureInitialized(Context context, boolean fromInitProvider) {
        if (isInitialized()) {
            // No need to get clientId/clientSecret once again
            // we only need to re-run the Activity-related initialization
            initialize(context, null, null);
            return true;
        }

        WonderPushSettings.initialize(context);
        boolean foundBuildConfig = WonderPushSettings.getBuildConfigFound();

        // Collect all configuration here
        Boolean initProviderAllowed = WonderPushSettings.getBoolean("WONDERPUSH_AUTO_INIT", "wonderpush_autoInit", "com.wonderpush.sdk.autoInit");
        if (initProviderAllowed == null) initProviderAllowed = true;
        String clientId = WonderPushSettings.getString("WONDERPUSH_CLIENT_ID", "wonderpush_clientId", "com.wonderpush.sdk.clientId");
        String clientSecret = WonderPushSettings.getString("WONDERPUSH_CLIENT_SECRET", "wonderpush_clientSecret", "com.wonderpush.sdk.clientSecret");
        Boolean logging = WonderPushSettings.getBoolean("WONDERPUSH_LOGGING", "wonderpush_logging", "com.wonderpush.sdk.logging");
        Boolean requiresUserConsent = WonderPushSettings.getBoolean("WONDERPUSH_REQUIRES_USER_CONSENT", "wonderpush_requiresUserConsent", "com.wonderpush.sdk.requiresUserConsent");
        String integrator = WonderPushSettings.getString("WONDERPUSH_INTEGRATOR", "wonderpush_integrator", "com.wonderpush.sdk.integrator");
        Boolean geolocation = WonderPushSettings.getBoolean("WONDERPUSH_GEOLOCATION", "wonderpush_geolocation", "com.wonderpush.sdk.geolocation");

        // Apply any found configuration prior to initializing the SDK
        if (logging != null) {
            if (!logging) logDebug("Applying configuration: logging: " + logging);
            WonderPush.setLogging(logging);
            if (logging) logDebug("Applying configuration: logging: " + logging);
        }
        if (geolocation != null) {
            logDebug("Applying configuration: geolocation: " + geolocation);
            if (geolocation) {
                WonderPush.enableGeolocation();
            } else {
                WonderPush.disableGeolocation();
            }
        }
        if (requiresUserConsent != null) {
            logDebug("Applying configuration: requiresUserConsent: " + requiresUserConsent);
            WonderPush.setRequiresUserConsent(requiresUserConsent);
        }
        if (integrator != null) {
            logDebug("Applying configuration: integrator: " + integrator);
            setIntegrator(integrator);
        }

        // Store the ApplicationContext at the very least, this will benefit many codepath that may
        // accepts that initialization is not possible but expect WonderPushConfiguration to work
        if (sApplicationContext == null) {
            sApplicationContext = context.getApplicationContext();
            // WonderPushConfiguration will warn if used before SDK is initialized,
            // but thanks to this static variable it will work
        }

        // Stop the automatic WonderPushInitProvider here if necessary
        if (fromInitProvider && !initProviderAllowed) {
            logDebug("Skipping automatic initialization");
            return false;
        }

        // Try using the initializer class first for maximum control (using custom code)
        if (!isInitialized()) {
            String initializerClassName = null;
            try {
                Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
                initializerClassName = metaData.getString(METADATA_INITIALIZER_CLASS);
                if (initializerClassName != null) {
                    if (initializerClassName.startsWith(".")) {
                        initializerClassName = context.getPackageName() + initializerClassName;
                    }
                    logDebug("Initializing WonderPush using initializer class " + initializerClassName);
                    Class<? extends WonderPushInitializer> initializerClass = Class.forName(initializerClassName).asSubclass(WonderPushInitializer.class);
                    WonderPushInitializer initializer = initializerClass.newInstance();
                    initializer.initialize(context);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Failed to read application meta-data", e);
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
        }

        // Try to initializing WonderPush using collected credentials
        if (!isInitialized()) {
            if (!TextUtils.isEmpty(clientId) && !TextUtils.isEmpty(clientSecret)) {
                logDebug("Initializing WonderPush using collected credentials");
                WonderPush.initialize(context, clientId, clientSecret);
            }
        }

        // Warn the user once if not initialization means has been found
        if (!isInitialized()) {
            Log.e(TAG, "Could not initialize WonderPush using the initializer class, BuildConfig options or manifest <meta-data> options!");
            if (!foundBuildConfig) {
                Log.w(TAG, "No BuildConfig class found. You probably need to give the value of your gradle defaultConfig.applicationId as the a \"wonderpush_buildConfigPackage\" string resource or a \"com.wonderpush.sdk.buildConfigPackage\" manifest <meta-data>.");
            }
        }

        return isInitialized();
    }

    /**
     * Sets whether user consent is required before the SDK is allowed to work.
     *
     * <p>Call this method before {@link #initialize(Context)}.</p>
     *
     * @param value Whether user consent is required before the SDK is allowed to work.
     * @see #setUserConsent(boolean)
     */
    public static void setRequiresUserConsent(boolean value) {
        if (!sIsInitialized) {
            // We can't read hasUserConsent() before we're initialized
            sRequiresUserConsent = value;
        } else {
            boolean hadUserConsent = hasUserConsent();
            sRequiresUserConsent = value;
            Log.w(TAG, "WonderPush.setRequiresUserConsent(" + value + ") called after WonderPush.initialize(). Although supported, a proper implementation typically only calls it before.");
            // Refresh user consent
            boolean nowHasUserConsent = hasUserConsent();
            if (hadUserConsent != nowHasUserConsent) {
                hasUserConsentChanged(nowHasUserConsent);
            }
        }
    }

    /**
     * Returns whether user consent has already provided consent.
     *
     * <p>Call this method after {@link #initialize(Context)}.</p>
     */
    public static boolean getUserConsent() {
        return WonderPushConfiguration.getUserConsent();
    }

    /**
     * Returns false if user consent is required and was not provided,
     * true if user consent is not required or if it was provided.
     */
    static boolean hasUserConsent() {
        return !sRequiresUserConsent || WonderPushConfiguration.getUserConsent();
    }

    /**
     * Provides or withdraws user consent.
     *
     * <p>Call this method after {@link #initialize(Context)}.</p>
     *
     * @param value Whether the user provided or withdrew consent.
     * @see #setRequiresUserConsent(boolean)
     */
    public static void setUserConsent(boolean value) {
        boolean hadUserConsent = hasUserConsent();
        if (hadUserConsent && !value) {
            JSONSyncInstallation.flushAll();
        }
        WonderPushConfiguration.setUserConsent(value);
        boolean nowHasUserConsent = hasUserConsent();
        if (sIsInitialized && hadUserConsent != nowHasUserConsent) {
            hasUserConsentChanged(nowHasUserConsent);
        }
    }

    private static void hasUserConsentChanged(boolean hasUserConsent) {
        synchronized (sUserConsentListeners) {
            if (!sIsInitialized) logError("hasUserConsentChanged called before SDK is initialized");
            logDebug("User consent changed to " + hasUserConsent);
            sApiImpl._deactivate();
            if (hasUserConsent) {
                sApiImpl = new WonderPushImpl();
            } else {
                sApiImpl = new WonderPushNoConsentImpl();
            }
            sApiImpl._activate();
            for (UserConsentListener listener : sUserConsentListeners) {
                try {
                    listener.onUserConsentChanged(hasUserConsent);
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while processing user consent changed listeners", ex);
                }
            }
        }
    }

    static void addUserConsentListener(UserConsentListener listener) {
        synchronized (sUserConsentListeners) {
            sUserConsentListeners.add(listener);
        }
    }

    static void removeUserConsentListener(UserConsentListener listener) {
        synchronized (sUserConsentListeners) {
            sUserConsentListeners.remove(listener);
        }
    }

    /**
     * Defers code to execute once consent is provided *and* SDK is initialized, using {@link #safeDefer(Runnable, long)}.
     * If consent is available, the code is still executed using {@link #safeDefer(Runnable, long)}.
     * @param runnable The code to execute
     * @param id - Permits to replace an old runnable still waiting with the same id.
     *           If {@code null}, a UUID is generated.
     */
    static void safeDeferWithConsent(final Runnable runnable, @Nullable final String id) {
        safeDefer(new Runnable() {
            @Override
            public void run() {
                if (hasUserConsent()) {
                    runnable.run();
                } else {
                    synchronized (sUserConsentDeferred) {
                        sUserConsentDeferred.put(id != null ? id : UUID.randomUUID().toString(), runnable);
                    }
                }
            }
        }, 0);
    }

    /**
     * Exports all data stored locally and on WonderPush servers and then starts a sharing activity
     * for the user to save it.
     *
     * <p>
     *     Call this within an {@link com.wonderpush.sdk.CacheUtil.FetchWork.AsyncTask}
     *     as this method is blocking.
     * </p>
     */
    public static void downloadAllData() {
        DataManager.downloadAllData();
    }

    /**
     * Ask the WonderPush servers to delete any event associated with the all local installations.
     */
    public static void clearEventsHistory() {
        DataManager.clearEventsHistory();
    }

    /**
     * Ask the WonderPush servers to delete any custom data associated with the all local installations and related users.
     */
    public static void clearPreferences() {
        DataManager.clearPreferences();
    }

    /**
     * Remove any local storage and ask the WonderPush servers to delete any data associated with the all local installations and related users.
     */
    public static void clearAllData() {
        DataManager.clearAllData();
    }

    /**
     * Remove any local storage and ask the WonderPush servers to delete any data associated with the all local installations and related users.
     * @deprecated
     * @see WonderPush#clearAllData()
     */
    public static void clearAll() {
        clearAllData();
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
     *            Use {@code null} for anonymous users.<br>
     *            You are strongly encouraged to use your own unique internal identifier.
     */
    @SuppressWarnings("unused")
    public static void setUserId(String userId) {
        try {
            if ("".equals(userId)) userId = null;
            logDebug("setUserId(" + userId + ")");

            // Do nothing if not initialized
            if (!isInitialized()) {
                logDebug("setting user id for next initialization");
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
                // Note the server will remove the push token from the current installation
                // once we set it to the new installation
                JSONObject diff = new JSONObject("{\"pushToken\": {\"data\": null}}");
                JSONSyncInstallation.forCurrentUser().receiveDiff(diff);

                // The user id changed, we must reset the access token
                initForNewUser(userId);
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
    @SuppressWarnings("unused")
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
     * Gets the device id, used to identify a single device,
     * and to correctly identify multiple users on a single device.
     *
     * <p>You should not call this method before initializing the SDK.</p>
     *
     * <p>Returns {@code null} if called without required user consent.</p>
     *
     * @return The device id, or {@code null} if the SDK is not initialized.
     * @see #isReady()
     * @see #initialize(Context)
     */
    @SuppressWarnings("unused")
    public static String getDeviceId() {
        return sApiImpl.getDeviceId();
    }

    /**
     * Gets the device id, used to identify a single device across applications,
     * and to correctly identify multiple users on a single device.
     *
     * <p>You should not call this method before the SDK is ready.</p>
     *
     * <p>Returns {@code null} if called without required user consent.</p>
     *
     * @return The device id, or {@code null} if the SDK is not initialized.
     * @see #isReady()
     */
    @SuppressWarnings("unused")
    public static String getInstallationId() {
        return sApiImpl.getInstallationId();
    }

    /**
     * Gets the push token, used to send notification to this installation.
     *
     * <p>You should not call this method before initializing the SDK.</p>
     *
     * <p>Returns {@code null} if called without required user consent.</p>
     *
     * @return The push token, or {@code null} if the installation is not yet
     *     registered to push notifications, or has not finished refreshing
     *     the push token after a forced update.
     */
    @SuppressWarnings("unused")
    public static String getPushToken() {
        return sApiImpl.getPushToken();
    }

    /**
     * Gets the access token, used to grant access to the current installation
     * to the WonderPush REST API.
     *
     * <p>You should not call this method before the SDK is ready.</p>
     *
     * <p>
     *     This together with your client secret gives entire control to the current installation
     *     and the associated user, you should not disclose it unnecessarily.
     * </p>
     *
     * <p>Returns {@code null} if called without required user consent.</p>
     *
     * @return The access token, or {@code null} if the SDK is not initialized.
     * @see #isReady()
     */
    @SuppressWarnings("unused")
    public static String getAccessToken() {
        return sApiImpl.getAccessToken();
    }

    /**
     * Enables push notifications for the current device.
     *
     * <p>Does nothing if called without required user consent.</p>
     */
    public static void subscribeToNotifications() {
        sApiImpl.subscribeToNotifications();
    }

    /**
     * Disables push notifications for the current device.
     *
     * <p>Does nothing if called without required user consent.</p>
     */
    public static void unsubscribeFromNotifications() {
        sApiImpl.unsubscribeFromNotifications();
    }

    /**
     * Returns whether push notification are enabled.
     *
     * <p>Returns {@code false} if called without required user consent.</p>
     *
     * @return {@code true} by default as no explicit user permission is required,
     * unless required user consent is lacking.
     */
    public static boolean isSubscribedToNotifications() {
        return sApiImpl.isSubscribedToNotifications();
    }

    /**
     * Returns whether push notification are enabled.
     *
     * <p>Use {@link #isSubscribedToNotifications()} instead.</p>
     *
     * <p>Returns {@code false} if called without required user consent.</p>
     *
     * @return {@code true} by default as no explicit user permission is required,
     * unless required user consent is lacking.
     * @see #isSubscribedToNotifications()
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static boolean getNotificationEnabled() {
        return sApiImpl.getNotificationEnabled();
    }

    /**
     * Sets whether to enable push notifications for the current device.
     *
     * <p>Use {@link #subscribeToNotifications()} or {@link #unsubscribeFromNotifications()} instead.</p>
     *
     * <p>Does nothing if called without required user consent.</p>
     *
     * @param status {@code false} to opt out of push notifications.
     * @see #subscribeToNotifications()
     * @see #unsubscribeFromNotifications()
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static void setNotificationEnabled(boolean status) {
        sApiImpl.setNotificationEnabled(status);
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

    /**
     * Gets the framework, library or wrapper used for integration.
     *
     * This method should not be used by the developer directly,
     * only by components that facilitates the native SDK integration.
     *
     */
    static String getIntegrator() {
        return sIntegrator;
    }

    /**
     * Sets the framework, library or wrapper used for integration.
     *
     * This method should not be used by the developer directly,
     * only by components that facilitates the native SDK integration.
     *
     * @param integrator Expected format is "some-component-1.2.3"
     */
    public static void setIntegrator(String integrator) {
        sIntegrator = integrator;
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

    protected static Looper getLooper() {
        return sLooper;
    }

    /**
     * Returns the configured delegate, or {@code null} if none was set.
     * @see WonderPush#setDelegate(WonderPushDelegate)
     */
    public static WonderPushDelegate getDelegate() {
        return sDelegate;
    }

    /**
     * Configures a delegate for tighter integration with the SDK.
     *
     * <p>Call this method as early as possible, like just before calling {@link #initialize(Context)}.</p>
     *
     * @param delegate The new delegate to use.
     * @see WonderPushDelegate
     * @see WonderPushAbstractDelegate
     */
    public static void setDelegate(WonderPushDelegate delegate) {
        sDelegate = delegate;
    }

    static String delegateUrlForDeepLink(DeepLinkEvent event) {
        String url = event.getUrl();
        WonderPushDelegate delegate = WonderPush.getDelegate();

        if (delegate != null) {

            String newUrl = url;
            try {
                WonderPush.logDebug("Asking delegate to handle a deep-link: " + event);
                newUrl = delegate.urlForDeepLink(event);
                WonderPush.logDebug("Delegate returned: " + newUrl);
            } catch (Exception ex) {
                Log.e(WonderPush.TAG, "Delegate failed to handle a deep-link: " + event, ex);
            }

            if (newUrl == null) {
                WonderPush.logDebug("Delegate handled the deep-link, aborting normal processing");
            } else if (newUrl.equals(url)) {
                WonderPush.logDebug("Delegate did not handle the deep-link, continuing normal processing");
            } else {
                WonderPush.logDebug("Delegate handled the deep-link and gave a new url, continuing with it: " + newUrl);
            }

            url = newUrl;

        }

        return url;
    }

    protected static PresenceManager getPresenceManager() {
        if (sPresenceManager == null) {
            sPresenceManager = new PresenceManager(new PresenceManager.PresenceManagerAutoRenewDelegate() {
                @Override
                public void autoRenewPresence(PresenceManager presenceManager, PresenceManager.PresencePayload presence) {
                    if (presence == null) return;
                    try {
                        JSONObject data = new JSONObject();
                        data.put("presence", presence.toJSONObject());
                        trackInternalEvent("@PRESENCE", data);
                    } catch (JSONException e) {
                        WonderPush.logError("Could not serialize presence", e);
                    }
                }
            }, PRESENCE_ANTICIPATED_TIME, PRESENCE_UPDATE_SAFETY_MARGIN);
        }
        return sPresenceManager;
    }

    static RemoteConfigManager getRemoteConfigManager() {
        return sRemoteConfigManager;
    }

    private static void updateEventsBlackWhiteList(@NonNull RemoteConfig config) {
        sEventsBlackWhiteList = null;
        JSONArray blackWhiteListRules = config.getData().optJSONArray(REMOTE_CONFIG_EVENTS_BLACK_WHITE_LIST_KEY);
        if (blackWhiteListRules != null) {
            List<String> rules = new ArrayList<>();
            for (int i = 0; i < blackWhiteListRules.length(); i++) {
                String rule = blackWhiteListRules.optString(i);
                if (rule != null) rules.add(rule);
            }
            sEventsBlackWhiteList = new BlackWhiteList(rules);
        }
    }
}
