package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.TextUtils;
import android.util.Log;

import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.display.InAppMessagingDisplay;
import com.wonderpush.sdk.push.PushServiceManager;
import com.wonderpush.sdk.push.PushServiceResult;

import com.wonderpush.sdk.ratelimiter.RateLimiter;
import com.wonderpush.sdk.remoteconfig.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.wonderpush.sdk.remoteconfig.Constants.REMOTE_CONFIG_EVENTS_BLACK_WHITE_LIST_KEY;

import okhttp3.OkHttp;

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
    private static InAppMessaging.PrivateController sInAppMessagingPrivateController;

    private static Context sApplicationContext;
    protected static Application sApplication;

    protected static final ScheduledExecutorService sScheduledExecutor = Executors.newScheduledThreadPool(4, new ThreadFactory() {
        // Adapted from Executors.DefaultThreadFactory to customize the thread names for better debuggability
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        {
            SecurityManager s = System.getSecurityManager();
            group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, "WonderPush-" + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            int priority = Thread.NORM_PRIORITY - 1;
            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
            return t;
        }
    });

    private static WonderPushRequestVault sMeasurementsApiRequestVault;
    private static PresenceManager sPresenceManager;
    private static RemoteConfigManager sRemoteConfigManager;

    private static String sClientId;
    private static String sClientSecret;
    private static String sBaseURL;
    private static boolean sIsInitialized = false;

    private static boolean sRequiresUserConsent = false;
    private static final WonderPushNotInitializedImpl sNotInitializedImpl = new WonderPushNotInitializedImpl();
    private static IWonderPush sApiImpl = sNotInitializedImpl;

    private static final Map<String, Runnable> sUserConsentDeferred = new TreeMap<>();
    private static final Set<UserConsentListener> sUserConsentListeners = new LinkedHashSet<>();
    private static final Map<String, Runnable> sSubscriptionDeferred = new TreeMap<>();
    private static final Set<SubscriptionStatusListener> sSubscriptionStatusListeners = new LinkedHashSet<>();

    static void resumeInAppMessaging() {
        sInAppMessagingPrivateController.resume();
    }

    static void pauseInAppMessaging() {
        sInAppMessagingPrivateController.pause();
    }

    interface UserConsentListener {
        void onUserConsentChanged(boolean hasUserConsent);
    }
    interface SubscriptionStatusListener {
        void onSubscriptionStatus(SubscriptionStatus subscriptionStatus);
    }
    static {
        // Add the necessary user consent listener to dequeue sUserConsentDeferred
        addUserConsentListener(new UserConsentListener() {
            @Override
            public void onUserConsentChanged(boolean hasUserConsent) {
                if (hasUserConsent) {
                    List<Runnable> runnables;
                    synchronized (sUserConsentDeferred) {
                        runnables = new ArrayList<>(sUserConsentDeferred.values());
                        sUserConsentDeferred.clear();
                    }
                    for (Runnable runnable : runnables) {
                        WonderPush.safeDefer(runnable, 0);
                    }
                }
            }
        });

        addSubscriptionStatusListener(new SubscriptionStatusListener() {
            @Override
            public void onSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
                if (subscriptionStatus == SubscriptionStatus.OPT_IN) {
                    List<Runnable> runnables;
                    synchronized (sSubscriptionDeferred) {
                        runnables = new ArrayList<>(sSubscriptionDeferred.values());
                        sSubscriptionDeferred.clear();
                    }
                    for (Runnable runnable : runnables) {
                        WonderPush.safeDefer(runnable, 0);
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
    static final String SDK_SHORT_VERSION = BuildConfig.WONDERPUSH_SDK_VERSION;
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
     * in a {@link #INTENT_NOTIFICATION_OPENED} intent.
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
     * The scheme for the WonderPush intents.
     */
    protected static final String INTENT_NOTIFICATION_WILL_OPEN_SCHEME = "wonderpush";

    /**
     * The authority for handling notification opens with deep links with WonderPush special handling.
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
     * The extra key indicating which action button the user clicked on the notification
     * in a {@link #INTENT_NOTIFICATION_WILL_OPEN} intent.
     */
    public static final String INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX =
            INTENT_NOTIFICATION_OPENED_EXTRA_BUTTON_INDEX;

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
     * Intent extra key holding occurences data serialized as JSON of event being tracked.
     */
    public static final String INTENT_EVENT_TRACKED_OCCURRENCES = "occurrences";

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

    protected static void logDebug(String tag, String debug) {
        if (WonderPush.SHOW_DEBUG) {
            Log.d(tag, debug);
        }
    }

    protected static void logDebug(String debug, Throwable tr) {
        if (WonderPush.SHOW_DEBUG) {
            Log.d(TAG, debug, tr);
        }
    }

    protected static void logDebug(String tag, String debug, Throwable tr) {
        if (WonderPush.SHOW_DEBUG) {
            Log.d(tag, debug, tr);
        }
    }

    protected static void logError(String msg) {
        if (WonderPush.SHOW_DEBUG) {
            Log.e(TAG, msg);
        }
    }

    protected static void logError(String tag, String msg) {
        if (WonderPush.SHOW_DEBUG) {
            Log.e(tag, msg);
        }
    }

    protected static void logError(String msg, Throwable tr) {
        if (WonderPush.SHOW_DEBUG) {
            Log.e(TAG, msg, tr);
        }
    }

    protected static void logError(String tag, String msg, Throwable tr) {
        if (WonderPush.SHOW_DEBUG) {
            Log.e(tag, msg, tr);
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
        ApiClient.getInstance().get(resource, params, responseHandler);
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
        ApiClient.getInstance().post(resource, params, responseHandler);
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
        ApiClient.getInstance().postEventually(resource, params);
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
     * Sends request using the ApiClient when we have an accessToken,
     * or using the AnonymousApiClient otherwise.
     *
     * @param request
     */
    protected static void requestEventuallyWithOptionalAccessToken(Request request) {
        String accessToken = WonderPushConfiguration.getAccessToken();
        if (accessToken != null) {
            ApiClient.getInstance().requestEventually(request);
        } else {
            AnonymousApiClient.getInstance().requestEventually(request);
        }
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
        final Request request = new Request(WonderPushConfiguration.getUserId(), HttpMethod.POST, resource, params, null);
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
        ApiClient.getInstance().put(resource, params, responseHandler);
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
        ApiClient.getInstance().delete(resource, responseHandler);
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
        try {
            sLocationOverride = new AtomicReference<>(location == null ? null : new Location(location));
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting geolocation", e);
        }
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
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting locale", e);
        }
    }

    /**
     * Gets the user's locale, either as previously stored, or as guessed from the system.
     *
     * @return The user's locale.
     * @see WonderPush#setLocale(String)
     */
    public static String getLocale() {
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting locale", e);
            return null;
        }
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
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting country", e);
        }
    }

    /**
     * Gets the user's country, either as previously stored, or as guessed from the system.
     *
     * @return The user's country.
     * @see WonderPush#setCountry(String)
     */
    public static String getCountry() {
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting country", e);
            return null;
        }
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
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting currency", e);

        }
    }

    /**
     * Gets the user's currency, either as previously stored, or as guessed from the system.
     *
     * @return The user's currency.
     * @see WonderPush#setCurrency(String)
     */
    public static String getCurrency() {
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting currency", e);
            return null;
        }
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
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting timezone", e);
        }
    }

    /**
     * Gets the user's time zone, either as previously stored, or as guessed from the system.
     *
     * @return The user's time zone.
     * @see WonderPush#setTimeZone(String)
     */
    public static String getTimeZone() {
        try {
            String rtn = WonderPushConfiguration.getTimeZone();
            if (rtn == null) {
                rtn = TimeZone.getDefault().getID();
            }
            return rtn;
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting timezone", e);
            return null;
        }
    }

    /**
     * Returns the latest known properties attached to the current installation object stored by WonderPush.
     *
     * <p>Returns an empty {@code JSONObject} if called without required user consent.</p>
     */
    public static JSONObject getProperties() {
        try {
            return sApiImpl.getProperties();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting properties", e);
            return new JSONObject();
        }
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
        try {
            sApiImpl.putProperties(properties);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting properties", e);
        }
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
        try {
            sApiImpl.setProperty(field, value);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting property", e);
        }
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
        try {
            sApiImpl.unsetProperty(field);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while unsetting property", e);
        }
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
        try {
            sApiImpl.addProperty(field, value);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while adding property", e);
        }
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
        try {
            sApiImpl.removeProperty(field, value);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while removing property", e);
        }
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
        try {
            return sApiImpl.getPropertyValue(field);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting property value", e);
            return null;
        }
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
        try {
            return sApiImpl.getPropertyValues(field);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting property values", e);
            return new ArrayList<>();
        }
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
        try {
            return sApiImpl.getInstallationCustomProperties();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting installation custom properties", e);
            return new JSONObject();
        }
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
        try {
            sApiImpl.putInstallationCustomProperties(customProperties);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while putting installation custom properties", e);
        }
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
        try {
            sApiImpl.trackEvent(type);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while tracking event", e);
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
     * @param attributes
     *            A JSON object containing attributes to be attached to the event.
     *            Prefer using a few attributes over a plethora of event type variants.
     */
    public static void trackEvent(String type, JSONObject attributes) {
        try {
            sApiImpl.trackEvent(type, attributes);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while tracking event with attributes", e);
        }
    }

    static void trackEvent(String type, JSONObject eventData, JSONObject attributes) {
        if (type == null || type.length() == 0 || type.charAt(0) == '@') {
            throw new IllegalArgumentException("Bad event type");
        }
        _trackEvent(type, eventData, attributes, null);
    }

    static void trackInternalEvent(String type, JSONObject eventData) {
        trackInternalEvent(type, eventData, null);
    }

    static void countInternalEvent(String type, JSONObject eventData) {
        countInternalEvent(type, eventData, null);
    }

    static void countInternalEvent(String type, JSONObject eventData, JSONObject customData) {
        if (type.charAt(0) != '@') {
            throw new IllegalArgumentException("This method must only be called for internal events, starting with an '@'");
        }
        _countEvent(type, eventData, customData);
    }

    static void trackInternalEvent(String type, JSONObject eventData, JSONObject customData, Runnable sentCallback) {
        if (type.charAt(0) != '@') {
            throw new IllegalArgumentException("This method must only be called for internal events, starting with an '@'");
        }
        _trackEvent(type, eventData, customData, sentCallback);
    }

    static void trackInternalEvent(String type, JSONObject eventData, JSONObject customData) {
        trackInternalEvent(type, eventData, customData, null);
    }

    static void trackInAppEvent(String type, JSONObject eventData, JSONObject customData) {
        _trackEvent(type, eventData, customData, false, null);
    }

    private static void _trackEvent(String type, JSONObject eventData, JSONObject customData, final Runnable sentCallback) {
        _trackEvent(type, eventData, customData, true, sentCallback);
    }

    private static void _trackEvent(String type, JSONObject eventData, JSONObject customData, boolean requiresSubscription, final Runnable sentCallback) {
        if (!hasUserConsent()) {
            logError("Not tracking event without user consent. type=" + type + ", data=" + eventData + " custom=" + customData);
            return;
        }

        final JSONObject event = getEventObject(type, eventData, customData);

        // Remember
        WonderPushConfiguration.Occurrences occurrences = WonderPushConfiguration.rememberTrackedEvent(event);
        JSONObject occurrencesJSON = null;
        if (occurrences != null) {
            try {
                occurrencesJSON = occurrences.toJSON();
                event.put("occurrences", occurrencesJSON);
            } catch (JSONException e) {
                logError("Could not add occurrences to payload", e);
            }
        }

        // Broadcast locally that an event was tracked
        Intent eventTrackedIntent = new Intent(WonderPush.INTENT_EVENT_TRACKED);
        eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_EVENT_TYPE, type);
        if (customData != null) {
            eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_CUSTOM_DATA, customData.toString());
        }
        if (occurrencesJSON != null) {
            eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_OCCURRENCES, occurrencesJSON.toString());
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(eventTrackedIntent);

        getEventsBlackWhiteList((BlackWhiteList eventsBlackWhiteList, Throwable error) -> {

            // Do not send to server if blacklisted.
            if (eventsBlackWhiteList != null && !eventsBlackWhiteList.allow(type)) {
                logError("Not tracking event forbidden by config. type=" + type + ", data=" + eventData + " custom=" + customData);
                return;
            }

            Request.Params parameters = new Request.Params();
            parameters.put("body", event.toString());

            Runnable post = () -> {
                postEventually("/events/", parameters);
                if (sentCallback != null) {
                    WonderPush.safeDefer(() -> {
                        sentCallback.run();
                    }, 0);
                }
            };

            getRemoteConfigManager().read((config, error1) -> {
                if (config != null && config.getData().optBoolean(Constants.REMOTE_CONFIG_TRACK_EVENTS_FOR_NON_SUBSCRIBERS_KEY)) {
                    post.run();
                } else if (requiresSubscription) {
                    safeDeferWithSubscription(post, null);
                } else {
                    final Request request = new Request(WonderPushConfiguration.getUserId(), HttpMethod.POST, "/events/", parameters, null);
                    WonderPush.requestEventuallyWithOptionalAccessToken(request);
                }
            });

        });

    }

    private static void _countEvent(String type, JSONObject eventData, JSONObject customData) {
        if (!hasUserConsent()) {
            logError("Not tracking event without user consent. type=" + type + ", data=" + eventData + " custom=" + customData);
            return;
        }

        if (WonderPushConfiguration.getAccessToken() != null
                && WonderPushConfiguration.getOverrideNotificationReceipt() == Boolean.TRUE) {
            _trackEvent(type, eventData, customData, null);
            return;
        }

        final JSONObject event = getEventObject(type, eventData, customData);

        // Remember
        WonderPushConfiguration.Occurrences occurrences = WonderPushConfiguration.rememberTrackedEvent(event);
        JSONObject occurrencesJSON = null;
        if (occurrences != null) {
            try {
                occurrencesJSON = occurrences.toJSON();
                event.put("occurrences", occurrencesJSON);
            } catch (JSONException e) {
                logError("Could not add occurrences to payload", e);
            }
        }

        // Broadcast locally that an event was tracked
        Intent eventTrackedIntent = new Intent(WonderPush.INTENT_EVENT_TRACKED);
        eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_EVENT_TYPE, type);
        if (customData != null) {
            eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_CUSTOM_DATA, customData.toString());
        }
        if (occurrencesJSON != null) {
            eventTrackedIntent.putExtra(WonderPush.INTENT_EVENT_TRACKED_OCCURRENCES, occurrencesJSON.toString());
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(eventTrackedIntent);

        getEventsBlackWhiteList((BlackWhiteList eventsBlackWhiteList, Throwable error) -> {

            // Do not send to server if blacklisted.
            if (eventsBlackWhiteList != null && !eventsBlackWhiteList.allow(type)) {
                logError("Not tracking event forbidden by config. type=" + type + ", data=" + eventData + " custom=" + customData);
                return;
            }

            Request.Params parameters = new Request.Params();
            parameters.put("body", event.toString());

            postEventuallyWithMeasurementsApiClient("/events", parameters);
        });
    }

    private static JSONObject getEventObject(String type, JSONObject eventData, JSONObject customData) {
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
                JSONObject locationJson = new JSONObject();
                locationJson.put("lat", location.getLatitude());
                locationJson.put("lon", location.getLongitude());
                event.put("location", locationJson);
            }
            if (!event.has("actionDate")) {
                event.put("actionDate", TimeSync.getTime());
            }
            // Notification metadata
            NotificationMetadata metadata = NotificationManager.getLastClickedNotificationMetadata();
            if (metadata != null) {
                metadata.fill(event, NotificationMetadata.AttributionReason.RECENT_NOTIFICATION_OPENED);
            }
        } catch (JSONException ex) {
            WonderPush.logError("Error building event object body", ex);
        }
        return event;
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
        try {
            sApiImpl.addTag(tag);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while adding tag", e);
        }
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
        try {
            sApiImpl.removeTag(tag);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while removing tag", e);
        }
    }

    /**
     * Remove all tags from the current installation object stored by WonderPush.
     *
     * <p>Does nothing if called without required user consent.</p>
     */
    public static void removeAllTags() {
        try {
            sApiImpl.removeAllTags();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while removing all tags", e);
        }
    }

    /**
     * Returns all the tags of the current installation object stored by WonderPush.
     *
     * @return
     *      A copy of the set of tags attached to the installation.
     *      Never returns {@code null}.
     */
    public static Set<String> getTags() {
        try {
            return sApiImpl.getTags();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting tags", e);
            return new HashSet<>();
        }
    }

    /**
     * Test whether the current installation has the given tag attached to it.
     * @param tag
     *      The tag to test.
     * @return
     *      {@code true} if the given tag is attached to the installation, {@code false} otherwise.
     */
    public static boolean hasTag(String tag) {
        try {
            return sApiImpl.hasTag(tag);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while testing tag", e);
            return false;
        }
    }

    private static boolean appOpenQueued;
    protected static void injectAppOpenIfNecessary() {
        if (!hasUserConsent()) {
            logDebug("onInteraction ignored without user consent");
            return;
        }
        long lastInteractionDate = WonderPushConfiguration.getLastInteractionDate();
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

        // Non-subscribers will have an up-to-date lastInteraction time.
        // Queue an @APP_OPEN event if we've never sent one to the server and haven't already queued one.
        // This will ensure newly subscribed users have at least one @APP_OPEN event in their timeline.
        if (WonderPushConfiguration.getLastAppOpenSentDate() == 0 && !appOpenQueued) {
            shouldInjectAppOpen = true;
        }

        if (shouldInjectAppOpen) {
            // We will track a new app open event

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
            NotificationManager.setLastClickedNotificationMetadata(null);
            // Add the information of the clicked notification
            if (now - lastOpenedNotificationDate < 10 * 1000 && lastOpenedNotificationInfo.length() > 0) { // allow a few seconds between click on the notification and the call to this method
                String notificationId = JSONUtil.optString(lastOpenedNotificationInfo, "notificationId");
                String campaignId = JSONUtil.optString(lastOpenedNotificationInfo, "campaignId");
                String viewId = JSONUtil.optString(lastOpenedNotificationInfo, "viewId");
                JSONObject reporting = lastOpenedNotificationInfo.optJSONObject("reporting");
                NotificationMetadata metadata = new NotificationMetadata(campaignId, notificationId, viewId, reporting, false);
                try {
                    metadata.fill(openInfo, NotificationMetadata.AttributionReason.RECENT_NOTIFICATION_OPENED);
                } catch (JSONException e) {
                    logDebug("Failed to fill @APP_OPEN opened notification information", e);
                }
                if (campaignId != null || notificationId != null || viewId != null || reporting != null) {
                    NotificationManager.setLastClickedNotificationMetadata(metadata);
                }
            }
            // Presence
            try {
                PresenceManager manager = getPresenceManager();
                PresenceManager.PresencePayload presence = manager.isCurrentlyPresent() ? null : manager.presenceDidStart();
                if (presence != null) {
                    openInfo.put("presence", presence.toJSONObject());
                }
            } catch (InterruptedException e) {
                WonderPush.logError("Could not start presence", e);
            } catch (JSONException e) {
                WonderPush.logError("Could not serialize presence", e);
            }

            if (!WonderPush.isSubscriptionStatusOptIn()) {
                WonderPush.countInternalEvent("@VISIT",openInfo,null);
                try {
                    openInfo.put("doNotSynthesizeVisit", true);
                } catch (JSONException e) {
                    WonderPush.logError("Could not add doNotSynthesizeVisit", e);
                }
            }

            trackInternalEvent("@APP_OPEN", openInfo, null, () -> {
                WonderPushConfiguration.setLastAppOpenSentDate(TimeSync.getTime());
            });
            appOpenQueued = true;
            WonderPushConfiguration.setLastAppOpenDate(now);
            WonderPushConfiguration.setLastAppOpenInfoJson(openInfo);
        }
    }

    /**
     * Whether {@link #initialize(Context, String, String)} has been called.
     * @return {@code true} if the SDK is initialized, {@code false} otherwise.
     */
    public static boolean isInitialized() {
        return sIsInitialized;
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

                sApplicationContext = context.getApplicationContext();
                sClientId = clientId;
                sClientSecret = clientSecret;
                sBaseURL = PRODUCTION_API_URL;
                OkHttpRemoteConfigFetcher fetcher = new OkHttpRemoteConfigFetcher(clientId, WonderPush::safeDefer, WonderPush::getUserAgent);
                SharedPreferencesRemoteConfigStorage storage = new SharedPreferencesRemoteConfigStorage(clientId, context);
                sRemoteConfigManager = new RemoteConfigManager(fetcher, storage, context);

                WonderPushConfiguration.initialize(getApplicationContext());
                safeDefer(() -> {
                    applyOverrideLogging(WonderPushConfiguration.getOverrideSetLogging());
                }, 0);
                safeDefer(() -> {
                    PushServiceManager.initialize(getApplicationContext());
                }, 0);
                setupDelegate();
                RateLimiter.initialize(WonderPushConfiguration::getSharedPreferences);
                safeDefer(WonderPushUserPreferences::initialize, 0);
                // NOTE: Do not use safeDefer on methods relying on WonderPushUserPreferences
                //       BEFORE this point.
                JSONSyncInstallation.setDisabled(true);
                ApiClient.getInstance().setDisabled(true);
                MeasurementsApiClient.setDisabled(true);
                safeDefer(JSONSyncInstallation::initialize, 0);
                // NOTE: Do not use safeDefer on methods relying on JSONSyncInstallation
                //       BEFORE this point.
                initializeInAppMessaging(context);

                // Setup a remote config handler to execute as soon as we get the config
                // and everytime the config changes.
                final RemoteConfigHandler remoteConfigHandler = new RemoteConfigHandler() {
                    @Override
                    public void handle(@javax.annotation.Nullable RemoteConfig config, @javax.annotation.Nullable Throwable error) {
                        if (config == null) return;
                        JSONObject configData = config.getData();
                        JSONSyncInstallation.setDisabled(configData.optBoolean(Constants.REMOTE_CONFIG_DISABLE_JSON_SYNC_KEY, false));
                        if (!JSONSyncInstallation.isDisabled()) JSONSyncInstallation.flushAll();
                        ApiClient.getInstance().setDisabled(configData.optBoolean(Constants.REMOTE_CONFIG_DISABLE_API_CLIENT_KEY, false));
                        MeasurementsApiClient.setDisabled(configData.optBoolean(Constants.REMOTE_CONFIG_DISABLE_MEASUREMENTS_API_CLIENT_KEY, false));

                        WonderPushConfiguration.setMaximumUncollapsedTrackedEventsAgeMs(configData.optLong(Constants.REMOTE_CONFIG_TRACKED_EVENTS_UNCOLLAPSED_MAXIMUM_AGE_MS_KEY, WonderPushConfiguration.DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_AGE_MS));
                        WonderPushConfiguration.setMaximumUncollapsedTrackedEventsCount(configData.optInt(Constants.REMOTE_CONFIG_TRACKED_EVENTS_UNCOLLAPSED_MAXIMUM_COUNT_KEY, WonderPushConfiguration.DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_COUNT));
                        WonderPushConfiguration.setMaximumCollapsedLastBuiltinTrackedEventsCount(configData.optInt(Constants.REMOTE_CONFIG_TRACKED_EVENTS_COLLAPSED_LAST_BUILTIN_MAXIMUM_COUNT_KEY, WonderPushConfiguration.DEFAULT_MAXIMUM_COLLAPSED_LAST_BUILTIN_TRACKED_EVENTS_COUNT));
                        WonderPushConfiguration.setMaximumCollapsedLastCustomTrackedEventsCount(configData.optInt(Constants.REMOTE_CONFIG_TRACKED_EVENTS_COLLAPSED_LAST_CUSTOM_MAXIMUM_COUNT_KEY, WonderPushConfiguration.DEFAULT_MAXIMUM_COLLAPSED_LAST_CUSTOM_TRACKED_EVENTS_COUNT));
                        WonderPushConfiguration.setMaximumCollapsedOtherTrackedEventsCount(configData.optInt(Constants.REMOTE_CONFIG_TRACKED_EVENTS_COLLAPSED_OTHER_MAXIMUM_COUNT_KEY, WonderPushConfiguration.DEFAULT_MAXIMUM_COLLAPSED_OTHER_TRACKED_EVENTS_COUNT));
                    }
                };

                // Read the config right away
                safeDeferWithConsent(() -> {
                    ensureConfigurationFetched(remoteConfigHandler, 10000);
                }, null);

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

                if (sBeforeInitializationUserIdSet) {
                    WonderPushConfiguration.changeUserId(sBeforeInitializationUserId);
                    deferInitForNewUser(sBeforeInitializationUserId);
                } else {
                    // Initializing using the previously used userId (which we defer reading to avoid synchronous IO during initialization)
                    safeDefer(() -> {
                        deferInitForNewUser(WonderPushConfiguration.getUserId());
                    }, 0);
                }

                sIsInitialized = true;
                hasUserConsentChanged(hasUserConsent()); // make sure to set sIsInitialized=true before
                // Ensure we get an @APP_OPEN with deferred initialization
                if (!hasUserConsent()) {
                    addUserConsentListener(new UserConsentListener() {
                        @Override
                        public void onUserConsentChanged(boolean hasUserConsent) {
                            if (hasUserConsent) {
                                injectAppOpenIfNecessary();
                            }
                        }
                    });
                }

                safeDefer(() -> {
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
                }, 0);
            }

            initializeForApplication(context);
            initializeForActivity(context);
            safeDefer(() -> {
                refreshPreferencesAndConfiguration(false);
            }, 0);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while initializing the SDK", e);
        }
    }

    private static void setupDelegate() {
        String className = WonderPushSettings.getString("WONDERPUSH_DELEGATE_CLASS", "wonderpush_delegateClass", "com.wonderpush.sdk.delegateClass");
        if (className == null) {
            WonderPush.logDebug("No delegate class found in manifest, build config or resources");
            return;
        }
        try {
            Class<?> cls = Class.forName(className);
            Object instance = cls.newInstance();
            if (!(instance instanceof WonderPushDelegate)) {
                WonderPush.logError("Delegate class '"+ className +"' is not an instance of WonderPushDelegate");
            } else if (sDelegate != null) {
                WonderPush.logError("Delegate class '"+ className +"' specified in build config / manifest / resources will not be used because there already is a delegate set up.");
            } else {
                WonderPushDelegate delegateInstance = (WonderPushDelegate) instance;
                delegateInstance.setContext(getApplicationContext());
                setDelegate(delegateInstance);
            }
        } catch (IllegalAccessException | InstantiationException |
                 ClassNotFoundException e) {
            WonderPush.logError("Could not instantiate delegate of class '" + className + "'", e);
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while instantiating delegate of class '" + className + "'", e);
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

    private static void deferInitForNewUser(final String userId) {
        WonderPush.logDebug("deferInitForNewUser(" + userId + ")");
        WonderPush.safeDefer(new Runnable() {
            @Override
            public void run() {
                refreshPreferencesAndConfiguration(false);
            }
        }, 0);
        WonderPush.safeDeferWithConsent(new Runnable() {
            @Override
            public void run() {
                ApiClient.getInstance().fetchAnonymousAccessTokenIfNeeded(userId);
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
            injectAppOpenIfNecessary();
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
        if (force) {
            boolean notificationEnabled = WonderPushConfiguration.getNotificationEnabled();
            WonderPushConfiguration.setNotificationEnabled(!notificationEnabled);
            WonderPush.setNotificationEnabled(notificationEnabled); // already calls refreshSubscriptionStatus()
        } else {
            WonderPush.refreshSubscriptionStatus();
        }
    }

    static void initializeInAppMessaging(Context context) {
        Application application = (Application)context.getApplicationContext();
        if (sInAppMessaging == null) {
            sInAppMessaging = InAppMessaging.initialize(application, new InternalEventTracker(), new InAppMessaging.InAppMessagingDelegate() {
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
                    safeDeferWithConsent(new Runnable() {
                        @Override
                        public void run() {
                            sRemoteConfigManager.read((RemoteConfig config, Throwable error) -> {
                                handler.handle(config != null ? config.getData().optJSONObject("inAppConfig") : null, error);
                            });
                        }
                    }, null);
                }

                @Override
                public void onReady(InAppMessaging.PrivateController privateController) {
                    sInAppMessagingPrivateController = privateController;
                    privateController.pause();
                }

                @Override
                public PresenceManager getPresenceManager() {
                    return WonderPush.getPresenceManager();
                }
            });
        }
        InAppMessagingDisplay.initialize(application, sInAppMessaging, WonderPush::safeDefer, WonderPush::trackInAppEvent, WonderPush::getUserAgent);
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

        String deviceBrand = InstallationManager.getDeviceBrand();
        if (deviceBrand != null && deviceBrand.toLowerCase().equals("huawei")) {
            try {
                Class.forName("com.wonderpush.sdk.push.hcm.HCMPushService");
            } catch (ClassNotFoundException e) {
                logError("Huawei device detected, but HCM support missing. Push notifications will likely NOT WORK on this device. Please follow this guide: https://docs.wonderpush.com/docs/huawei-mobile-services-hms-push-notification-support");
            }
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
                if (metaData == null) {
                    metaData = new Bundle();
                }
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

        // Create the notification channel we if are running on Android 12- or if we target Android 12-
        // A fresh install on Android 13 for an app targeting less will trigger a prompt on app start
        if (!NotificationPromptController.supportsPrompt()) {
            safeDefer(WonderPushUserPreferences::ensureDefaultAndroidNotificationChannelExists, 0);
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
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting requires-user-consent", e);
        }
    }

    /**
     * Returns whether user consent has already provided consent.
     *
     * <p>Call this method after {@link #initialize(Context)}.</p>
     */
    public static boolean getUserConsent() {
        try {
            return WonderPushConfiguration.getUserConsent();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting user consent", e);
            return false;
        }
    }

    /**
     * Returns false if user consent is required and was not provided,
     * true if user consent is not required or if it was provided.
     */
    static boolean hasUserConsent() {
        try {
            return !sRequiresUserConsent || WonderPushConfiguration.getUserConsent();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while testing user consent", e);
            return false;
        }
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
        try {
            boolean hadUserConsent = hasUserConsent();
            if (hadUserConsent && !value) {
                JSONSyncInstallation.flushAll(true); // when disabling, we use a sync flow to ensure we can withdraw user consent synchronously right after we return
            }
            WonderPushConfiguration.setUserConsent(value);
            boolean nowHasUserConsent = hasUserConsent();
            if (sIsInitialized && hadUserConsent != nowHasUserConsent) {
                hasUserConsentChanged(nowHasUserConsent);
            }
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting user consent", e);
        }
    }

    private static void hasUserConsentChanged(boolean hasUserConsent) {
        if (!sIsInitialized) logError("hasUserConsentChanged called before SDK is initialized");
        logDebug("User consent changed to " + hasUserConsent);
        sApiImpl._deactivate();
        if (hasUserConsent) {
            sApiImpl = new WonderPushImpl();
        } else {
            sApiImpl = new WonderPushNoConsentImpl();
        }
        sApiImpl._activate();

        // Iterate on a copy to let listeners de-register themselves
        // during iteration without triggering a java.util.ConcurrentModificationException
        Set<UserConsentListener> iterationSet;
        synchronized (sUserConsentListeners) {
            iterationSet = new HashSet<>(sUserConsentListeners);
        }
        for (UserConsentListener listener : iterationSet) {
            try {
                listener.onUserConsentChanged(hasUserConsent);
            } catch (Exception ex) {
                Log.e(TAG, "Unexpected error while processing user consent changed listeners", ex);
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
                        if (hasUserConsent()) { // we need a double check because things could have changed in the meantime
                            runnable.run();
                        } else {
                            sUserConsentDeferred.put(id != null ? id : UUID.randomUUID().toString(), runnable);
                        }
                    }
                }
            }
        }, 0);
    }

    static void addSubscriptionStatusListener(SubscriptionStatusListener listener) {
        synchronized (sSubscriptionStatusListeners) {
            sSubscriptionStatusListeners.add(listener);
        }
    }

    static void removeSubscriptionStatusListener(SubscriptionStatusListener listener) {
        synchronized (sSubscriptionStatusListeners) {
            sSubscriptionStatusListeners.remove(listener);
        }
    }

    static void notifySubscriptionStatus() {
        Set<SubscriptionStatusListener> iterationSet;
        synchronized (sSubscriptionStatusListeners) {
            if (!sIsInitialized) logError("subscriptionStatusChanged called before SDK is initialized");
            // Iterate on a copy to let listeners de-register themselves
            // during iteration without triggering a java.util.ConcurrentModificationException
            iterationSet = new HashSet<>(sSubscriptionStatusListeners);
        }
        SubscriptionStatus subscriptionStatus = getSubscriptionStatus();
        for (SubscriptionStatusListener listener : iterationSet) {
            try {
                listener.onSubscriptionStatus(subscriptionStatus);
            } catch (Exception ex) {
                Log.e(TAG, "Unexpected error while processing user consent changed listeners", ex);
            }
        }
    }

    /**
     * Defers code to execute once user is subscribed, using {@link #safeDefer(Runnable, long)}.
     * If user is subscribed, the code is still executed using {@link #safeDefer(Runnable, long)}.
     * @param runnable The code to execute
     * @param id - Permits to replace an old runnable still waiting with the same id.
     *           If {@code null}, a UUID is generated.
     */
    static void safeDeferWithSubscription(final Runnable runnable, @Nullable final String id) {
        safeDefer(new Runnable() {
            @Override
            public void run() {
                if (isSubscriptionStatusOptIn()) {
                    runnable.run();
                } else {
                    synchronized (sSubscriptionDeferred) {
                        if (isSubscriptionStatusOptIn()) { // we need a double check because things could have changed in the meantime
                            runnable.run();
                        } else {
                            sSubscriptionDeferred.put(id != null ? id : UUID.randomUUID().toString(), runnable);
                        }
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
     *     This method is blocking.
     * </p>
     */
    public static void downloadAllData() {
        try {
            DataManager.downloadAllData();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while downloading all data", e);
        }
    }

    /**
     * Ask the WonderPush servers to delete any event associated with the all local installations.
     */
    public static void clearEventsHistory() {
        try {
            DataManager.clearEventsHistory();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while clearing events history", e);
        }
    }

    /**
     * Ask the WonderPush servers to delete any custom data associated with the all local installations and related users.
     */
    public static void clearPreferences() {
        try {
            DataManager.clearPreferences();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while clearing preferences", e);
        }
    }

    /**
     * Remove any local storage and ask the WonderPush servers to delete any data associated with the all local installations and related users.
     */
    public static void clearAllData() {
        try {
            DataManager.clearAllData();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while clearing all data", e);
        }
    }

    /**
     * Remove any local storage and ask the WonderPush servers to delete any data associated with the all local installations and related users.
     * @deprecated
     * @see WonderPush#clearAllData()
     */
    public static void clearAll() {
        try {
            clearAllData();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while clearing all", e);
        }
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
     *   Upon changing userId, the push token is wiped, so avoid unnecessary calls, like calling with {@code null}
     *   just before calling with a user id. Successive calls with the same userId are fine.
     * </p>
     *
     * <p>
     *   Changing the user id is akin to loading a new profile. A new installation will be created and no tags,
     *   properties or events will be kept.
     *   For more information, see <a href="https://docs.wonderpush.com/docs/user-ids" target="_blank">our documentation</a>.
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

                // We're done reading state for previous user, we can now store the new one
                WonderPushConfiguration.changeUserId(userId);

                // The user id changed, we must reset the access token
                deferInitForNewUser(userId);
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
     * @see #initialize(Context)
     */
    @SuppressWarnings("unused")
    public static String getDeviceId() {
        try {
            return sApiImpl.getDeviceId();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting device identifier", e);
            return null;
        }
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
     */
    @SuppressWarnings("unused")
    public static String getInstallationId() {
        try {
            return sApiImpl.getInstallationId();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting installation identifier", e);
            return null;
        }
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
        try {
            return sApiImpl.getPushToken();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting push token", e);
            return null;
        }
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
     */
    @SuppressWarnings("unused")
    public static String getAccessToken() {
        try {
            return sApiImpl.getAccessToken();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while getting access token", e);
            return null;
        }
    }

    /**
     * Enables push notifications for the current device
     * @param fallbackToSettings Shows a dialog that leads user to the settings should he refuse the permission
     */
    public static void subscribeToNotifications(boolean fallbackToSettings) {
        try {
            sApiImpl.subscribeToNotifications(fallbackToSettings);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while subscribing to notifications", e);
        }
    }

    /**
     * Enables push notifications for the current device.
     *
     * <p>Does nothing if called without required user consent.</p>
     */
    public static void subscribeToNotifications() {
        try {
            sApiImpl.subscribeToNotifications();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while subscribing to notifications", e);
        }
    }

    /**
     * Disables push notifications for the current device.
     *
     * <p>Does nothing if called without required user consent.</p>
     */
    public static void unsubscribeFromNotifications() {
        try {
            sApiImpl.unsubscribeFromNotifications();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error unsubscribing from notifications", e);
        }
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
        try {
            return sApiImpl.isSubscribedToNotifications();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while testing subscription to notifications", e);
            return false;
        }
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
        try {
            return sApiImpl.getNotificationEnabled();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while testing notifications enabled", e);
            return false;
        }
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
        try {
            sApiImpl.setNotificationEnabled(status);
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while setting notifications enabled", e);
        }
    }

    static void refreshSubscriptionStatus() {
        try {
            sApiImpl.refreshSubscriptionStatus();
        } catch (Exception e) {
            Log.d(TAG, "Unexpected error while refreshing subscription status", e);
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

    protected static void safeDefer(final Runnable runnable, long defer) {
        sScheduledExecutor.schedule(runnable, defer, TimeUnit.MILLISECONDS);
    }

    protected static <V> Future<V> safeDefer(final Callable<V> callable, long defer) {
        return sScheduledExecutor.schedule(callable, defer, TimeUnit.MILLISECONDS);
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

    private interface EventsBlackWhiteListCallback {
        void call(BlackWhiteList list, Throwable error);
    }

    private static BlackWhiteList parseEventsBlackWhiteList(RemoteConfig config) {
        if (config == null) return null;
        BlackWhiteList list = null;
        if (config != null) {
            JSONArray blackWhiteListRules = config.getData().optJSONArray(REMOTE_CONFIG_EVENTS_BLACK_WHITE_LIST_KEY);
            if (blackWhiteListRules != null) {
                List<String> rules = new ArrayList<>();
                for (int i = 0; i < blackWhiteListRules.length(); i++) {
                    String rule = blackWhiteListRules.optString(i);
                    if (rule != null) rules.add(rule);
                }
                list = new BlackWhiteList(rules);
            }
        }
        return list;
    }

    private static void getEventsBlackWhiteList(EventsBlackWhiteListCallback callback) {
        getRemoteConfigManager().read((RemoteConfig config, Throwable error) -> {
            if (callback != null) callback.call(parseEventsBlackWhiteList(config), error);
        });
    }

    enum SubscriptionStatus {
        OPT_IN("optIn"),
        OPT_OUT("optOut");
        public final String slug;
        SubscriptionStatus(String slug) {
            this.slug = slug;
        }
    }

    static SubscriptionStatus getSubscriptionStatus() {
        JSONSyncInstallation installation = JSONSyncInstallation.forCurrentUser();
        try {
            String subscriptionStatus = installation.optSdkStateStringForPath(null, "preferences", "subscriptionStatus");
            if (subscriptionStatus == null) return null;

            if (subscriptionStatus.equals(SubscriptionStatus.OPT_OUT.slug)) {
                return SubscriptionStatus.OPT_OUT;
            }
            if (subscriptionStatus.equals(SubscriptionStatus.OPT_IN.slug)) {
                return SubscriptionStatus.OPT_IN;
            }
        } catch (JSONException e) {
        }
        return null;
    }

    static boolean isSubscriptionStatusOptIn() {
        return getSubscriptionStatus() == SubscriptionStatus.OPT_IN;
    }

    protected static String getUserAgent() {
        String packageName = WonderPush.getApplicationContext().getPackageName();
        String versionName = null;
        long versionCode = 0;
        try {
            PackageInfo packageInfo = WonderPush.getApplicationContext().getPackageManager().getPackageInfo(packageName, 0);
            versionName = packageInfo.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            WonderPush.logDebug("Could not retrieve version name");
        }
        return "WonderPushSDK/" + WonderPush.SDK_VERSION + " (package:" + packageName + "; appVersion:" + versionName + "; appVersionCode:" + versionCode + "; clientId:" + WonderPush.getClientId() + ") okhttp/" + OkHttp.VERSION + " Android/" + Build.VERSION.RELEASE + " AndroidAPILevel/" + Build.VERSION.SDK_INT + " " + System.getProperty("http.agent");
    }

}
