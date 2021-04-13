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
import android.os.Parcelable;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

// We don't use an IntentService because of
// https://github.com/loopj/android-async-http/pull/492 and
// https://github.com/loopj/android-async-http/pull/496
// We could also update android-async-http >= 1.4.5.

/**
 * WonderPush service for handling notification clicks.
 */
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

        NotificationModel notif = NotificationModel.fromLocalIntent(intent, getApplicationContext());
        if (notif == null) {
            Log.e(TAG, "handleOpenFromNotificationCenter() could not extract notification from intent: " + intent);
            return;
        }

        NotificationManager.handleOpenedNotificationFromService(getApplicationContext(), intent, notif);

        String targetUrl = notif.getTargetUrl();
        if (intent.hasExtra("overrideTargetUrl")) {
            targetUrl = intent.getStringExtra("overrideTargetUrl");
        }

        if (targetUrl == null) {
            targetUrl = WonderPush.INTENT_NOTIFICATION_WILL_OPEN_SCHEME + "://" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY + "/" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_DEFAULT;
        }

        if (!launchSuccessful) {
            targetUrl = WonderPush.delegateUrlForDeepLink(new DeepLinkEvent(this, targetUrl));
            if (targetUrl == null) {
                launchSuccessful = true;
            }
        }

        if (!launchSuccessful && (
                fromUserInteraction || NotificationModel.Type.DATA.equals(notif.getType())
        )) {
            WonderPush.logDebug("Handing targetUrl of opened notification: " + targetUrl);

            try {
                // Launch the activity associated with the given target url
                Intent activityIntent = new Intent();
                activityIntent.setAction(Intent.ACTION_VIEW);
                activityIntent.setData(Uri.parse(targetUrl));
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                        (Parcelable) intent.getParcelableExtra("receivedPushNotificationIntent"));
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL,
                        // this extra must be removed if handled outside the app,
                        // or we'll get ClassNotFoundException: E/Parcel: Class not found when unmarshalling: com.wonderpush.sdk.Notification*Model
                        notif);
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
                            WonderPush.logDebug("Delivered opened notification to the WonderPushService");
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
                        WonderPush.logDebug("Target URL resolved to internal activity: " + resolvedActivity.getClassName());

                        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
                        if (activity == null) {
                            activity = ActivityLifecycleMonitor.getLastStoppedActivity();
                        }
                        if (activity != null && !activity.isFinishing()) {
                            WonderPush.logDebug("Delivered opened notification on top of the last/current activity: " + activity.getClass().getCanonicalName());
                            // We have a current activity stack, keep it
                            activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // avoid duplicating the top activity
                            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);   // reuse any task started with a matching intent (as we don't use FLAG_ACTIVITY_MULTIPLE_TASK)
                            getApplicationContext().startActivity(activityIntent);
                            if (resolvedActivity.getClassName().equals(activity.getClass().getCanonicalName())) {
                                WonderPush.logDebug("Was already the last activity, showing notification on top of the current activity");
                                WonderPush.showPotentialNotification(activity, activityIntent);
                            }
                        } else {
                            // We must start a new task
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                            stackBuilder.addNextIntentWithParentStack(activityIntent);
                            if (stackBuilder.getIntentCount() == 1) {
                                // The target activity has no parent
                                Intent launchIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getApplicationContext().getPackageName());
                                ComponentName defaultActivity = launchIntent == null ? null : launchIntent.resolveActivity(getApplicationContext().getPackageManager());
                                if (defaultActivity != null) {
                                    Class<? extends Activity> defaultActivityClass = Class.forName(defaultActivity.getClassName()).asSubclass(Activity.class);
                                    if (!resolvedActivity.getClassName().equals(defaultActivity.getClassName())) {
                                        WonderPush.logDebug("Injecting the default activity as parent to the orphan target activity to avoid closing app on the user pressing back");
                                        // Add the default activity as parent of the target activity
                                        // it has otherwise no parent and pressing back would close the application
                                        Intent defaultActivityIntent = new Intent(this, defaultActivityClass);
                                        stackBuilder = TaskStackBuilder.create(this);
                                        stackBuilder.addNextIntentWithParentStack(defaultActivityIntent);
                                        stackBuilder.addNextIntent(activityIntent);
                                    } // the target activity is already the default activity, don't add anything to the parent stack
                                }
                            }
                            WonderPush.logDebug("Delivered opened notification using a new task");
                            stackBuilder.startActivities();
                        }
                        launchSuccessful = true;

                    }
                }
                if (!launchSuccessful) {
                    // Try as a service within the same package
                    try {
                        if (startService(activityIntent) != null) {
                            WonderPush.logDebug("Delivered opened notification to a service within the application");
                            launchSuccessful = true;
                        }
                    } catch (Exception ex) {
                        WonderPush.logDebug("Failed to try notification deep link as a service", ex);
                    }
                }

                // Remove restriction within this application
                if (!launchSuccessful) {
                    activityIntent.setPackage(null);
                    // Avoid ClassNotFoundException E/Parcel: Class not found when unmarshalling: com.wonderpush.sdk.Notification*Model
                    // Besides, this extra is not useful when the intent is not handled by the application itself.
                    activityIntent.removeExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL);
                }

                if (!launchSuccessful) {
                    // Try as an activity
                    if (activityIntent.resolveActivity(getPackageManager()) != null) {
                        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
                        if (activity == null) {
                            activity = ActivityLifecycleMonitor.getLastStoppedActivity();
                        }
                        if (activity != null && !activity.isFinishing()) {
                            WonderPush.logDebug("Delivered opened notification to an activity outside the app on top of last/current activity: " + activity.getClass().getCanonicalName());
                            // We have a current activity stack, keep it
                            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(activityIntent);
                            // Show the potential in-app when the user comes back on the application
                            WonderPush.showPotentialNotification(activity, intent);
                        } else {
                            WonderPush.logDebug("Delivered opened notification using a new task to an activity outside the app");
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
                            WonderPush.logDebug("Delivered opened notification to a service outside the application");
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
                launchSuccessful = openNotificationDefaultBehavior(intent, notif);
            } catch (Exception ex) {
                Log.e(TAG, "Unexpected error while opening notification using default behavior", ex);
            }

        }

        if (!launchSuccessful) {
            Log.e(TAG, "Failed to open notification properly");
        }
    }

    private boolean openNotificationDefaultBehavior(Intent intent) {
        NotificationModel notif = NotificationModel.fromLocalIntent(intent, getApplicationContext());
        if (notif == null) {
            Log.e(TAG, "openNotificationDefaultBehavior() could not extract notification from intent: " + intent);
            return false;
        }
        return openNotificationDefaultBehavior(intent, notif);
    }

    private boolean openNotificationDefaultBehavior(Intent intent, NotificationModel notif) {
        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
        Activity lastStoppedActivity = ActivityLifecycleMonitor.getLastStoppedActivity();
        if (activity != null && !activity.isFinishing()) {

            WonderPush.logDebug("Show notification on top of current activity: " + activity.getClass().getCanonicalName());
            WonderPush.showPotentialNotification(ActivityLifecycleMonitor.getCurrentActivity(), intent);

        } else if (lastStoppedActivity != null && !lastStoppedActivity.isFinishing()) {

            WonderPush.logDebug("Bringing last activity to front and show notification: " + lastStoppedActivity.getClass().getCanonicalName());
            // We have a current activity stack, keep it
            // Merely bring the last activity back to front
            // Do like getPackageManager().getLaunchIntentForPackage(getPackageName()),
            // but specifies the desired activity and avoids any null return value issue
            Intent activityIntent = new Intent();
            activityIntent.setPackage(getPackageName());
            activityIntent.setClass(lastStoppedActivity, lastStoppedActivity.getClass());
            // Do not deliver the notification this way, the SDK can't monitor onNewIntent()
            // and would rely on it calling setIntent() for onResume() to be able to automatically show the notification
            //     activityIntent.setDataAndType(intent.getData(), intent.getType());
            activityIntent.setAction(Intent.ACTION_MAIN);
            activityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // avoid duplicating the top activity, just get it back to front
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);   // reuse any task started with a matching intent (as we don't use FLAG_ACTIVITY_MULTIPLE_TASK)

            getApplicationContext().startActivity(activityIntent);
            // We must display the notification ourselves
            ActivityLifecycleMonitor.onNextResume(new ActivityLifecycleMonitor.ResumeListener() {
                @Override
                public void onResume(Activity activity) {
                    WonderPush.showPotentialNotification(lastStoppedActivity, intent);
                }
            });

        } else {

            Intent activityIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getApplicationContext().getPackageName());
            if (activityIntent == null) {
                Log.e(WonderPush.TAG, "Cannot launch application: no default launch intent. Make sure to have an activity with action MAIN and category LAUNCHER in your manifest.");
            } else {
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                        (Parcelable) intent.getParcelableExtra("receivedPushNotificationIntent"));
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL,
                        notif);
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE,
                        notif.getType().toString());
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION,
                        intent.getBooleanExtra("fromUserInteraction", true));
                activityIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN,
                        true);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntentWithParentStack(activityIntent);
                WonderPush.logDebug("Starting new task");
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
