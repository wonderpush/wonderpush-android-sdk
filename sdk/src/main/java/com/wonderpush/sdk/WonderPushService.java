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

    private static final String TAG = WonderPush.TAG;

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
        WonderPush.ensureInitialized(getApplicationContext());
        try {
            if (NotificationManager.containsExplicitNotification(intent)) {
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
        boolean fromUserInteraction = intent.getBooleanExtra("fromUserInteraction", true);

        NotificationModel notif = NotificationModel.fromLocalIntent(intent);
        if (notif == null) {
            Log.e(TAG, "handleOpenFromNotificationCenter() could not extract notification from intent: " + intent);
            return;
        }

        NotificationManager.handleOpenedNotificationFromService(getApplicationContext(), intent, notif);

        String targetUrl = notif.getTargetUrl();
        if (intent.hasExtra("overrideTargetUrl")) {
            targetUrl = intent.getStringExtra("overrideTargetUrl");
        }

        if (targetUrl != null && (
                fromUserInteraction || NotificationModel.Type.DATA.equals(notif.getType())
        )) {

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

                // Restrict first to this application
                activityIntent.setPackage(getApplicationContext().getPackageName());

                if (targetUrl.toLowerCase().startsWith(WonderPush.INTENT_NOTIFICATION_SCHEME + ":")) {
                    // Use the WonderPush service within this application
                    // MUST be done before: activityIntent.setPackage(getApplicationContext().getPackageName());
                    // Add some internal extra fields
                    activityIntent.putExtra("openPushNotificationIntent", intent);
                    try {
                        if (startService(activityIntent) != null) {
                            launchSuccessful = true;
                        }
                    } catch (Exception ex) {
                        WonderPush.logDebug("Failed to try notification deep link as a service", ex);
                    }
                    if (!launchSuccessful) {
                        activityIntent.removeExtra("openPushNotificationIntent");
                    }
                }
                if (!launchSuccessful) {
                    // Try as an activity within the same package
                    ComponentName resolvedActivity = activityIntent.resolveActivity(getPackageManager());
                    if (resolvedActivity != null) {

                        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
                        if (activity == null) {
                            activity = ActivityLifecycleMonitor.getLastStoppedActivity();
                        }
                        if (activity != null && !activity.isFinishing()) {
                            // We have a current activity stack, keep it
                            activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // avoid duplicating the top activity
                            startActivity(activityIntent);
                        } else {
                            // We must start a new task
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                            stackBuilder.addNextIntentWithParentStack(activityIntent);
                            if (stackBuilder.getIntentCount() == 1) {
                                // The target activity has no parent
                                String defaultActivityCanonicalName = intent.getExtras().getString("activity");
                                Class<? extends Activity> defaultActivityClass = Class.forName(defaultActivityCanonicalName).asSubclass(Activity.class);
                                if (!resolvedActivity.getClassName().equals(defaultActivityCanonicalName)) {
                                    // Add the default activity as parent of the target activity
                                    // it has otherwise no parent and pressing back would close the application
                                    Intent defaultActivityIntent = new Intent(this, defaultActivityClass);
                                    stackBuilder = TaskStackBuilder.create(this);
                                    stackBuilder.addNextIntentWithParentStack(defaultActivityIntent);
                                    stackBuilder.addNextIntent(activityIntent);
                                } // the target activity is already the default activity, don't add anything to the parent stack
                            }
                            stackBuilder.startActivities();
                        }
                        launchSuccessful = true;

                    }
                }
                if (!launchSuccessful) {
                    // Try as a service within the same package
                    try {
                        if (startService(activityIntent) != null) {
                            launchSuccessful = true;
                        }
                    } catch (Exception ex) {
                        WonderPush.logDebug("Failed to try notification deep link as a service", ex);
                    }
                }

                // Remove restriction within this application
                if (!launchSuccessful) {
                    activityIntent.setPackage(null);
                }

                if (!launchSuccessful) {
                    // Try as an activity
                    if (activityIntent.resolveActivity(getPackageManager()) != null) {
                        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
                        if (activity == null) {
                            activity = ActivityLifecycleMonitor.getLastStoppedActivity();
                        }
                        if (activity != null && !activity.isFinishing()) {
                            // We have a current activity stack, keep it
                            activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // avoid duplicating the top activity
                            startActivity(activityIntent);
                        } else {
                            // We must start a new task
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                            stackBuilder.addNextIntentWithParentStack(activityIntent);
                            stackBuilder.startActivities();
                        }
                        launchSuccessful = true;
                    }
                }
                if (!launchSuccessful) {
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
        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
        if (activity != null && !activity.isFinishing()) {

            WonderPush.showPotentialNotification(ActivityLifecycleMonitor.getCurrentActivity(), intent);

        } else {

            String desiredActivity = intent.getExtras().getString("activity");
            if (desiredActivity != null) {
                Intent activityIntent = new Intent();
                activityIntent.setClassName(getApplicationContext(), desiredActivity);
                activityIntent.fillIn(intent, 0);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntentWithParentStack(activityIntent);
                stackBuilder.startActivities();
            }

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
        if (WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_DEFAULT.equals(singlePath)) {

            openNotificationDefaultBehavior(notificationOpenedIntent);

        } else if (WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_BROADCAST.equals(singlePath)) {

            // Broadcast locally that a notification is to be opened, and don't do anything else
            Intent notificationWillOpenIntent = new Intent(WonderPush.INTENT_NOTIFICATION_WILL_OPEN);
            notificationWillOpenIntent.putExtras(intent.getExtras());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationWillOpenIntent);

        } else if (WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_NOOP.equals(singlePath)) {

            // Noop!

        } else {
            Log.w(TAG, "Unhandled intent: " + intent);
        }
    }

}
