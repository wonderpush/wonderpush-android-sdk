package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

// We don't use an IntentService because of
// https://github.com/loopj/android-async-http/pull/492 and
// https://github.com/loopj/android-async-http/pull/496
// We could also update android-async-http >= 1.4.5.
public class WonderPushService extends Service {

    private static String TAG = WonderPush.TAG;

    private static Boolean sIsProperlySetup;

    protected static boolean isProperlySetup() {
        try {
            if (sIsProperlySetup != null) {
                return sIsProperlySetup;
            }

            Context context = WonderPush.getApplicationContext();
            if (context == null) {
                Log.w(WonderPush.TAG, "Could not reliably tell whether " + WonderPushService.class.getSimpleName() + " is well setup: WonderPush is not initialized");
                return false;
            }
            sIsProperlySetup = false;

            ServiceInfo info = null;
            try {
                info = context.getPackageManager().getServiceInfo(new ComponentName(context, WonderPushService.class), 0);
            } catch (NameNotFoundException e) {
                Log.e(WonderPush.TAG, "Could not find service " + WonderPushService.class.getSimpleName() + ". Please add the following inside your <application> tag in your AndroidManifest.xml: <service android:name=\"com.wonderpush.sdk.WonderPushService\" android:enabled=\"true\" android:exported=\"false\" android:label=\"Push Notification service\"></service>");
            }
            if (info != null) {
                if (info.exported) {
                    Log.e(WonderPush.TAG, "Service " + WonderPushService.class.getSimpleName() + " should not be set to exported in your AndroidManifest.xml");
                } else if (!info.enabled) {
                    Log.e(WonderPush.TAG, "Service " + WonderPushService.class.getSimpleName() + " is not be set as enabled in your AndroidManifest.xml");
                } else if (!info.applicationInfo.enabled) {
                    Log.e(WonderPush.TAG, "Service " + WonderPushService.class.getSimpleName() + " is set as enabled, but not its enclosing <application> in your AndroidManifest.xml");
                } else {
                    // Hooray!
                    sIsProperlySetup = true;
                }
            }
            if (!sIsProperlySetup) {
                WonderPush.logDebug("Falling back to degraded mode");
            }
            return sIsProperlySetup;
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while checking proper setup", e);
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (WonderPush.containsExplicitNotification(intent)) {
                handleOpenFromNotificationCenter(intent);
            } else if (containsNotificationWillOpen(intent)) {
                handleWillOpen(intent);
            } else {
                Log.e(TAG, "Called with unknown intent: " + intent);
            }
        } catch (Exception e) {
            Log.e(WonderPush.TAG, "Unexpected error while responding to command " + intent, e);
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void handleOpenFromNotificationCenter(Intent intent) {
        boolean launchSuccessful = false;
        NotificationModel notif = NotificationModel.fromLocalIntent(intent);

        String targetUrl = notif.getTargetUrl();
        if (intent.hasExtra("overrideTargetUrl")) {
            targetUrl = intent.getStringExtra("overrideTargetUrl");
        }

        if (targetUrl != null) {

            try {
                // Launch the activity associated with the given target url
                Intent activityIntent = new Intent();
                activityIntent.setAction(Intent.ACTION_VIEW);
                activityIntent.setData(Uri.parse(targetUrl));
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                        intent.getParcelableExtra("receivedPushNotificationIntent"));
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE,
                        notif.getType().toString());
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION,
                        intent.getBooleanExtra("fromUserInteraction", true));
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN,
                        true);

                if (targetUrl.toLowerCase().startsWith(WonderPush.INTENT_NOTIFICATION_SCHEME + ":")) {
                    // Restrict to this application, as the wonderpush:// scheme may not be unique
                    activityIntent.setPackage(getApplicationContext().getPackageName());
                    // Add some internal extra fields
                    activityIntent.putExtra("openPushNotificationIntent", intent);
                }

                // Try as an activity
                if (activityIntent.resolveActivity(getPackageManager()) != null) {

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                    stackBuilder.addNextIntentWithParentStack(activityIntent);
                    stackBuilder.startActivities();
                    launchSuccessful = true;

                } else if (activityIntent.getPackage() != null || activityIntent.getComponent() != null) {
                    // startService() fails if the intent is not specified enough, hence the above checks

                    // Try as a service
                    try {
                        if (startService(activityIntent) != null) {
                            launchSuccessful = true;
                        }
                    } catch (Exception ex) {
                        WonderPush.logDebug("Failed to try notification deep link as a service", ex);
                    }

                }

                if (!launchSuccessful) {
                    Log.e(TAG, "Error when opening the target url: Could not resolve intent: " + activityIntent);
                }

            } catch (Exception ex) {
                Log.e(TAG, "Unexpected error while trying to open the notification deep link", ex);
            }

            if (!launchSuccessful) {
                Log.w(TAG, "Fall back to default activity");
            }

        }

        if (!launchSuccessful) {

            try {
                launchSuccessful = openNotificationDefaultBehavior(intent);
            } catch (Exception ex) {
                Log.e(TAG, "Unexpected error while opening notification using default behavior", ex);
            }

        }

        if (!launchSuccessful) {
            Log.e(TAG, "Failed to open notification properly");
        }
    }

    private boolean openNotificationDefaultBehavior(Intent intent) {
        Activity activity = WonderPush.getCurrentActivity();
        if (activity != null && !activity.isFinishing()) {

            WonderPush.showPotentialNotification(WonderPush.getCurrentActivity(), intent);

        } else {

            Intent activityIntent = new Intent();
            activityIntent.setClassName(getApplicationContext(), intent.getExtras().getString("activity"));
            activityIntent.fillIn(intent, 0);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(activityIntent);
            stackBuilder.startActivities();

        }
        return true;
    }

    private boolean containsNotificationWillOpen(Intent intent) {
        return intent.getData() != null
                && WonderPush.INTENT_NOTIFICATION_SCHEME.equals(intent.getData().getScheme())
                && WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY.equals(intent.getData().getAuthority())
                && intent.hasExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION)
                ;
    }

    private void handleWillOpen(Intent intent) {
        // Read any private extra and clean them out
        Intent notificationOpenedIntent = intent.getParcelableExtra("openPushNotificationIntent");
        intent.removeExtra("openPushNotificationIntent");

        String singlePath = null;
        if (intent.getData().getPathSegments().size() == 1) {
            singlePath = intent.getData().getPathSegments().get(0);
        }
        if (WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_DEFAULT_ACTIVITY.equals(singlePath)) {

            // Broadcast locally that a notification is to be opened, and don't do anything else
            openNotificationDefaultBehavior(notificationOpenedIntent);

        } else if (WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_BROADCAST.equals(singlePath)) {

            // Broadcast locally that a notification is to be opened, and don't do anything else
            Intent notificationWillOpenIntent = new Intent(WonderPush.INTENT_NOTIFICATION_WILL_OPEN);
            notificationWillOpenIntent.putExtras(intent.getExtras());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationWillOpenIntent);

        } else {
            Log.w(TAG, "Unhandled intent: " + intent);
        }
    }

}
