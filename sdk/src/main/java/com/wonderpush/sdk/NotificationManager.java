package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.wonderpush.sdk.push.PushServiceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NotificationManager {

    static final String TAG = WonderPush.TAG;

    private static WeakReference<Intent> sLastHandledIntentRef;

    static final String LAST_RECEIVED_NOTIFICATION_CHECK_DATE_PROPERTY = "lastReceivedNotificationCheckDate";

    private static NotificationMetadata sLastClickedNotificationMetadata;

    static NotificationMetadata getLastClickedNotificationMetadata() {
        return sLastClickedNotificationMetadata;
    }
    static void setLastClickedNotificationMetadata(NotificationMetadata metadata) {
        sLastClickedNotificationMetadata = metadata;
    }

    public static void onReceivedNotification(Context context, Intent intent, NotificationModel notif) {
        String loggedInstallationId = WonderPushConfiguration.getInstallationId();
        if (notif.getTargetedInstallation() != null && !notif.getTargetedInstallation().equals(loggedInstallationId)) {
            WonderPush.logDebug("Received notification is not targeted at the current installation (" + notif.getTargetedInstallation() + " does not match current installation " + loggedInstallationId + ")");
            return;
        }

        handleActions(context, new NotificationMetadata(notif), notif.getReceiveActions());

        try {
            final JSONObject trackData = new JSONObject();
            trackData.put("campaignId", notif.getCampaignId());
            trackData.put("notificationId", notif.getNotificationId());
            trackData.put("actionDate", TimeSync.getTime());
            boolean notifReceipt = notif.getReceipt();
            boolean receiptUsingMeasurements = notif.getReceiptUsingMeasurements();
            Boolean overrideNotificationReceipt = WonderPushConfiguration.getOverrideNotificationReceipt();
            if (overrideNotificationReceipt != null) {
                notifReceipt = overrideNotificationReceipt;
            }
            if (receiptUsingMeasurements) {
                WonderPush.countInternalEvent("@NOTIFICATION_RECEIVED", trackData);
            } else if (notifReceipt) {
                WonderPush.trackInternalEvent("@NOTIFICATION_RECEIVED", trackData);
            }
            WonderPushConfiguration.setLastReceivedNotificationInfoJson(trackData);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Unexpected error while tracking notification received", ex);
        }

        // Track lastReceivedNotificationCheckDate
        try {
            JSONSyncInstallation installation = JSONSyncInstallation.forCurrentUser();
            if (installation != null) {
                long lastReceivedNotificationCheckDate = installation.getSdkState().optLong(LAST_RECEIVED_NOTIFICATION_CHECK_DATE_PROPERTY, -1);
                long now = TimeSync.getTime();
                boolean reportLastReceivedNotificationCheckDate =
                        lastReceivedNotificationCheckDate == -1
                        || ((now - lastReceivedNotificationCheckDate) > notif.getLastReceivedNotificationCheckDelay());

                if (reportLastReceivedNotificationCheckDate) {
                    JSONObject diff = new JSONObject();
                    diff.put(LAST_RECEIVED_NOTIFICATION_CHECK_DATE_PROPERTY, now);
                    installation.put(diff);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting _wp data from notification", e);
        }

        boolean automaticallyHandled = false;
        Activity currentActivity = ActivityLifecycleMonitor.getCurrentActivity();
        boolean appInForeground = currentActivity != null && !currentActivity.isFinishing();
        String tag = generateLocalNotificationTag(notif);
        int localNotificationId = generateLocalNotificationId(tag);
        PendingIntentBuilder pendingIntentBuilder = new PendingIntentBuilder(notif, localNotificationId, intent, context);
        AlertModel alert = notif.getAlert() == null ? null : notif.getAlert().forCurrentSettings(appInForeground);
        if (alert != null && alert.getAutoDrop()) {
            WonderPush.logDebug("Automatically dropping");
            automaticallyHandled = true;
        } else if (alert != null && alert.getAutoOpen()) {
            WonderPush.logDebug("Automatically opening");
            // We can show the notification (send the pending intent) right away
            try {
                pendingIntentBuilder.buildForAutoOpen().send();
                automaticallyHandled = true;
            } catch (PendingIntent.CanceledException e) {
                Log.e(WonderPush.TAG, "Could not show notification", e);
            }
        }
        if (!automaticallyHandled) {
            WonderPushResourcesService.Work work =
                    new WonderPushResourcesService.Work(
                            notif, tag, localNotificationId, intent);
            if (shouldWorkInBackground(notif)) {
                WonderPush.logDebug("Fetching resources and displaying notification asynchronously");
                WonderPushResourcesService.enqueueWork(context, work);
            } else {
                WonderPush.logDebug("Fetching resources and displaying notification");
                fetchResourcesAndDisplay(context, work, WonderPushResourcesService.TIMEOUT_MS);
            }
        }
    }

    private static boolean shouldWorkInBackground(NotificationModel notif) {
        return notif.getAlert() != null && !notif.getAlert().getResourcesToFetch().isEmpty();
    }

    protected static void fetchResourcesAndDisplay(Context context, WonderPushResourcesService.Work work, long timeoutMs) {
        NotificationModel notif = work.getNotif();
        if (notif == null) return;

        if (notif.getAlert() != null && !notif.getAlert().getResourcesToFetch().isEmpty()) {
            WonderPush.logDebug("Start fetching resources");
            long start = SystemClock.elapsedRealtime();
            Collection<Future<File>> tasks = new ArrayList<>(notif.getAlert().getResourcesToFetch().size());
            int i = 0;
            for (CacheUtil.FetchWork fetchWork : notif.getAlert().getResourcesToFetch()) {
                ++i;
                tasks.add(WonderPush.safeDefer(fetchWork::execute, 0));
            }
            i = 0;
            for (Future<File> task : tasks) {
                ++i;
                try {
                    task.get(Math.max(0, start + timeoutMs - SystemClock.elapsedRealtime()), TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    WonderPush.logDebug("Failed to fetch resource " + i, e);
                }
            }
            // Now we must reparse the notification to have it pick up the fetched resources
            WonderPush.logDebug("Inserting resources inside the notification");
            try {
                JSONObject json = new JSONObject(notif.getInputJSONString());
                notif = NotificationModel.fromNotificationJSONObject(json);
                if (notif == null) return;
            } catch (NotificationModel.NotTargetedForThisInstallationException | JSONException ex) {
                Log.e(TAG, "Unexpected error while reparsing notification", ex);
            }
        }

        if (notif.getAlert() != null) {
            AlertModel alternative = notif.getAlert().getAlternativeIfNeeded();
            if (alternative != null) {
                WonderPush.logDebug("Using an alternative alert");
                notif.setAlert(alternative);
                // Do not try to fetch resources for the new alternative,
                // we are likely to choose it because one resource fetch was interrupted,
                // so ignore potential resources that can stay on the alternative
                // as it might block for a very long time.
            }
        }

        WonderPush.logDebug("Building notification");
        Notification notification = buildNotification(notif, context, work.getPendingIntentBuilder(context));

        if (notification == null) {
            WonderPush.logDebug("No notification is to be displayed");
            // Fire an Intent to notify the application anyway (especially for `data` notifications)
            try {
                if (notif.getType() == NotificationModel.Type.DATA) {
                    // Broadcast locally that a notification is to be opened, and don't do anything else
                    Intent localIntent = work.getPendingIntentBuilder(context).buildIntentForDataNotificationWillOpenLocalBroadcast();
                    LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
                } else {
                    work.getPendingIntentBuilder(context).buildForWillOpenBroadcast().send();
                }
            } catch (PendingIntent.CanceledException e) {
                Log.e(WonderPush.TAG, "Could not broadcast the notification will open intent", e);
            }
        } else {
            notify(context, work.getTag(), work.getLocalNotificationId(), notification);
            // Display group summary notification, if any
            Notification groupSummaryNotification = buildNotificationGroupSummary(notif, context, work.getPendingIntentBuilder(context));
            if (groupSummaryNotification != null) {
                String tag = null; // should not stay null, but null is a legal value anyway
                if (notif.getAlert() != null && notif.getAlert().getGroup() != null) {
                    tag = notif.getAlert().getGroup() + "-GROUP_SUMMARY_NOTIFICATION";
                }
                notify(context, tag, 0, groupSummaryNotification);
            }
        }
    }

    protected static String generateLocalNotificationTag(NotificationModel notif) {
        return notif.getAlert() != null && notif.getAlert().hasTag()
                ? notif.getAlert().getTag() : notif.getCampaignId();
    }

    protected static int generateLocalNotificationId(String tag) {
        if (tag != null) {
            return 0;
        } else {
            return WonderPushConfiguration.getNextTaglessNotificationManagerId();
        }
    }

    protected static void notify(Context context, String tag, int localNotificationId, Notification notification) {
        try {
            WonderPush.logDebug("Showing notification with tag " + (tag == null ? "(null)" : "\"" + tag + "\"") + " and id " + localNotificationId);
            android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(tag, localNotificationId, notification);
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Failed to show the notification", ex);
        }
    }

    protected static void cancel(Context context, String tag, int localNotificationId) {
        try {
            android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(tag, localNotificationId);
        } catch (Exception ex) {
            WonderPush.logError("Failed to cancel the notification", ex);
        }
    }

    protected static class PendingIntentBuilder {

        private final NotificationModel notif;
        private final int localNotificationId;
        private final Intent pushIntent;
        private final Context context;

        public PendingIntentBuilder(NotificationModel notif, int localNotificationId, Intent pushIntent, Context context) {
            this.notif = notif;
            this.localNotificationId = localNotificationId;
            this.pushIntent = pushIntent;
            this.context = context;
        }

        public PendingIntent buildForAutoOpen() {
            return buildPendingIntent(false, null, null);
        }

        public PendingIntent buildForDefault() {
            return buildPendingIntent(true, null, null);
        }

        public PendingIntent buildForButton(int buttonIndex) {
            Bundle extrasOverride = new Bundle();
            extrasOverride.putInt(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX, buttonIndex);
            String targetUrl = notif.getAlert().getButtons().get(buttonIndex).targetUrl;
            return buildPendingIntent(true, extrasOverride, targetUrl);
        }

        public Intent buildIntentForDataNotificationWillOpenLocalBroadcast() {
            Intent localIntent = new Intent();
            localIntent.setAction(WonderPush.INTENT_NOTIFICATION_WILL_OPEN);
            localIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                    pushIntent);
            localIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL,
                    notif);
            localIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE,
                    notif.getType().toString());
            localIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION,
                    false);
            localIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN,
                    true);
            localIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX,
                    -1);

            // Restrict to this application
            localIntent.setPackage(context.getPackageName());

            return localIntent;
        }

        public PendingIntent buildForWillOpenBroadcast() {
            String targetUrl =
                    WonderPush.INTENT_NOTIFICATION_SCHEME + "://" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY
                            + "/" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_BROADCAST;
            return buildPendingIntent(false, null, targetUrl);
        }

        public PendingIntent buildForGroupSummary() {
            if (this.notif.getAlert() == null || !this.notif.getAlert().hasGroupTargetUrl()) {
                return this.buildForDefault();
            }
            if (this.notif.getAlert().getGroupTargetUrl() == null) {
                return null;
            }
            String targetUrl = this.notif.getAlert().getGroupTargetUrl();
            return buildPendingIntent(true, null, targetUrl);
        }

        private PendingIntent buildPendingIntent(boolean fromUserInteraction, Bundle extrasOverride, String targetUrlOverride) {
            String targetUrl = notif.getTargetUrl();
            if (targetUrl == null) {
                targetUrl = WonderPush.INTENT_NOTIFICATION_WILL_OPEN_SCHEME + "://" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY + "/" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_DEFAULT;
            }
            if (targetUrlOverride != null) {
                targetUrl = targetUrlOverride;
            }

            // Construct the WonderPush tracking intent
            Intent wpTrackingIntent = new Intent();
            wpTrackingIntent.setPackage(context.getPackageName());
            wpTrackingIntent.setClass(context, WonderPushNotificationTrackingReceiver.class);
            wpTrackingIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                    pushIntent);
            wpTrackingIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION,
                    fromUserInteraction);
            wpTrackingIntent.putExtra("targetUrl", targetUrl);
            if (extrasOverride != null) {
                wpTrackingIntent.putExtras(extrasOverride);
            }

            Uri.Builder dataUriBuilder = new Uri.Builder()
                    .scheme(WonderPush.INTENT_NOTIFICATION_SCHEME)
                    .authority(WonderPush.INTENT_NOTIFICATION_AUTHORITY)
                    .appendQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER, notif.getInputJSONString())
                    .appendQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER_LOCAL_NOTIFICATION_ID, String.valueOf(localNotificationId))
                    // Add cache busting so that the group summary notification intent is different from the one of its last notification (extras are ignored for this comparison)
                    // and the groupTargetUrl override (an extra-only difference) is either ignored or overrides that last notification too (depending on whether PendingIntent.FLAG_UPDATE_CURRENT is used or not)
                    // We could add the notification's tag as query parameter too, but it's more cumbersome to pass it down here.
                    .appendQueryParameter("_cacheBuster", "" + System.currentTimeMillis() + "-" + Math.random())
                    ;
            Uri dataUri = dataUriBuilder.build();
            wpTrackingIntent.setDataAndType(dataUri, WonderPush.INTENT_NOTIFICATION_TYPE);

            // Construct the destination intent
            Intent destinationIntent;
            targetUrl = WonderPush.delegateUrlForDeepLink(new DeepLinkEvent(context, targetUrl));
            WonderPush.logDebug("Notification target URL: " + targetUrl);

            if (targetUrl == null) {
                // No targetUrl
                destinationIntent = null;
            } else {

                // Handle targetUrl
                Uri parsedTargetUrl = Uri.parse(targetUrl);
                if (WonderPush.INTENT_NOTIFICATION_SCHEME.equals(parsedTargetUrl.getScheme())
                        && WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY.equals(parsedTargetUrl.getAuthority())
                ) {

                    // wonderpush://notificationOpen/* URLs
                    if (parsedTargetUrl.getPathSegments().size() == 1 && WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_DEFAULT.equals(parsedTargetUrl.getLastPathSegment())) {

                        // wonderpush://notificationOpen/default: Start the application as a launcher would
                        destinationIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        if (destinationIntent != null) {
                            // A launcher would have used Intent.setPackage(null), needed on pre Android-11 to avoid duplicating the top activity
                            // We previously used Intent.FLAG_ACTIVITY_SINGLE_TOP but as we add our tracking activity on top, the situation is different
                            destinationIntent.setPackage(null);
                            destinationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this is actually already added by PackageManager.getLaunchIntentForPackage()
                            destinationIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                                    (Parcelable) pushIntent);
                            destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL,
                                    // We can use this because we are restricting to the the current package,
                                    // otherwise we'd get ClassNotFoundException: E/Parcel: Class not found when unmarshalling: com.wonderpush.sdk.Notification*Model
                                    notif);
                            destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE,
                                    notif.getType().toString());
                            destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION,
                                    fromUserInteraction);
                            destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN,
                                    true);
                        }

                    } else { // broadcast or noop will both still need the tracking activity

                        destinationIntent = null;

                    }

                } else {

                    // Non wonderpush://notificationOpen/* URLs

                    destinationIntent = new Intent();
                    destinationIntent.setAction(Intent.ACTION_VIEW);
                    destinationIntent.setData(parsedTargetUrl);
                    destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION,
                            (Parcelable) pushIntent);
                    destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL,
                            // this extra must be removed if handled outside the app,
                            // or we'll get ClassNotFoundException: E/Parcel: Class not found when unmarshalling: com.wonderpush.sdk.Notification*Model
                            notif);
                    destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE,
                            notif.getType().toString());
                    destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION,
                            fromUserInteraction);
                    destinationIntent.putExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN,
                            true);

                    // Restrict first to this application
                    // NOTE: Using Intent.resolveActivity() is thus allowed in this case.
                    destinationIntent.setPackage(context.getPackageName());

                    ComponentName resolvedActivity = destinationIntent.resolveActivity(context.getPackageManager());
                    if (resolvedActivity == null) {
                        // Clean for delivery outside this application
                        destinationIntent.setPackage(null);
                        // Avoid a ClassNotFoundException: E/Parcel: Class not found when unmarshalling: com.wonderpush.sdk.Notification*Model
                        destinationIntent.removeExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL);
                    }

                }

            }

            // Create the composite PendingIntent
            Intent[] intents;
            if (destinationIntent == null) {
                wpTrackingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); // avoid bringing the background app to the front
                intents = new Intent[] { wpTrackingIntent };
            } else {
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntentWithParentStack(destinationIntent);
                if (destinationIntent.getPackage() != null && stackBuilder.getIntentCount() == 1) {
                    // The target activity has no parent
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                    ComponentName defaultActivity = launchIntent == null ? null : launchIntent.resolveActivity(context.getPackageManager());
                    if (defaultActivity != null) {
                        ComponentName resolvedActivity = destinationIntent.resolveActivity(context.getPackageManager());
                        if (!resolvedActivity.getClassName().equals(defaultActivity.getClassName())) {
                            WonderPush.logDebug("Injecting the default activity as parent to the orphan target activity to avoid closing app on the user pressing back");
                            // Add the default activity as parent of the target activity
                            // it has otherwise no parent and pressing back would close the application
                            stackBuilder = TaskStackBuilder.create(context);
                            stackBuilder.addNextIntentWithParentStack(launchIntent);
                            stackBuilder.addNextIntent(destinationIntent);
                        } // else: the target activity is already the default activity, don't add anything to the parent stack
                    }
                }
                stackBuilder.addNextIntent(wpTrackingIntent);
                intents = stackBuilder.getIntents();
                // Clear the first intent of any flags added by TaskStackBuilder
                if (intents.length > 0) {
                    intents[0].setFlags(intents[0].getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                }
            }
            return PendingIntent.getActivities(context, 0, intents,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT
                    | WonderPushCompatibilityHelper.getPendingIntentFlagImmutable());
        }

    }

    protected static Notification buildNotificationGroupSummary(NotificationModel notif, Context context,
                                                                PendingIntentBuilder pendingIntentBuilder) {
        return buildNotification_inner(notif, context, pendingIntentBuilder, true);
    }

    protected static Notification buildNotification(NotificationModel notif, Context context,
                                                    PendingIntentBuilder pendingIntentBuilder) {
        return buildNotification_inner(notif, context, pendingIntentBuilder, false);
    }

    protected static Notification buildNotification_inner(NotificationModel notif, Context context,
                                                          PendingIntentBuilder pendingIntentBuilder,
                                                          boolean buildGroupSummary) {
        if (NotificationModel.Type.DATA.equals(notif.getType())) {
            return null;
        }
        // Read notification content override if application is foreground
        Activity currentActivity = ActivityLifecycleMonitor.getCurrentActivity();
        boolean appInForeground = currentActivity != null && !currentActivity.isFinishing();
        AlertModel alert = notif.getAlert() == null ? null : notif.getAlert().forCurrentSettings(appInForeground);
        if (alert == null || (alert.getTitle() == null && alert.getText() == null)) {
            // Nothing to display, don't create a notification
            return null;
        }
        // Special group considerations
        if (buildGroupSummary) {
            // No group summary necessary
            if (alert.getGroup() == null) {
                return null;
            }
            // Old Android version that does not handle grouping
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // On pre Android-7.0 (N, API 24) we prefer to let each notification appear individually,
                // rather than creating a group notification that would hide them all.
                return null;
            }
        }
        // Apply defaults
        if (alert.getTitle() == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            final PackageManager pm = context.getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(context.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                ai = null;
            } catch (NullPointerException e) {
                ai = null;
            }
            alert.setTitle(ai != null ? pm.getApplicationLabel(ai) : null);
        }
        int defaultIconResource = PushServiceManager.getNotificationIcon();
        int defaultColor = PushServiceManager.getNotificationColor();

        WonderPushChannel channel = WonderPushUserPreferences.channelToUseForNotification(alert.getChannel());
        boolean canVibrate = context.getPackageManager().checkPermission(android.Manifest.permission.VIBRATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        boolean lights = true;
        boolean lightsCustomized = false;
        boolean vibrates = true;
        boolean vibratesCustomPattern = false;
        boolean noisy = true;
        boolean noisyCustomUri = false;
        int defaults = Notification.DEFAULT_ALL;
        if (!canVibrate) {
            defaults &= ~Notification.DEFAULT_VIBRATE;
            vibrates = false;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel.getId())
                .setContentIntent(buildGroupSummary ? pendingIntentBuilder.buildForGroupSummary() : pendingIntentBuilder.buildForDefault())
                .setAutoCancel(true)
                .setContentTitle(alert.getTitle())
                .setContentText(alert.getText())
                .setSubText(alert.getSubText())
                .setContentInfo(alert.getInfo())
                .setTicker(alert.getTicker())
                .setSmallIcon(alert.hasSmallIcon() && alert.getSmallIcon() != 0 ? alert.getSmallIcon() : defaultIconResource)
                .setLargeIcon(alert.getLargeIcon())
                .setCategory(alert.getCategory())
                .setGroup(alert.getGroup())
                .setGroupSummary(buildGroupSummary)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setSortKey(alert.getSortKey())
                .setOngoing(alert.getOngoing())
                ;
        if (alert.hasPriority()) {
            builder.setPriority(alert.getPriority());
        }
        if (alert.hasColor()) {
            builder.setColor(alert.getColor());
        } else if (defaultColor != 0) {
            builder.setColor(defaultColor);
        }
        if (alert.hasLocalOnly()) {
            builder.setLocalOnly(alert.getLocalOnly());
        }
        if (alert.hasNumber()) {
            builder.setNumber(alert.getNumber());
        }
        if (alert.hasOnlyAlertOnce()) {
            builder.setOnlyAlertOnce(alert.getOnlyAlertOnce());
        }
        if (alert.hasWhen()) {
            builder.setWhen(alert.getWhen());
        }
        if (alert.hasShowWhen()) {
            builder.setShowWhen(alert.getShowWhen());
        }
        if (alert.hasUsesChronometer()) {
            builder.setUsesChronometer(alert.getUsesChronometer());
        }
        if (alert.hasVisibility()) {
            builder.setVisibility(alert.getVisibility());
        }
        if (alert.getPersons() != null) {
            for (String person : alert.getPersons()) {
                builder.addPerson(person);
            }
        }
        if (alert.hasProgress()) {
            builder.setProgress(alert.getProgressMax(), alert.getProgress(), alert.isProgressIndeterminate());
        }

        if (alert.hasLightsColor() || alert.hasLightsOn() || alert.hasLightsOff()) {
            lights = true;
            lightsCustomized = true;
            builder.setLights(alert.getLightsColor(), alert.getLightsOn(), alert.getLightsOff());
            defaults &= ~Notification.DEFAULT_LIGHTS;
        } else if (alert.hasLights()) {
            if (alert.getLights()) {
                lights = true;
                lightsCustomized = false;
                defaults |= Notification.DEFAULT_LIGHTS;
            } else {
                lights = false;
                defaults &= ~Notification.DEFAULT_LIGHTS;
            }
        }
        if (canVibrate) {
            if (alert.getVibratePattern() != null) {
                vibrates = true;
                vibratesCustomPattern = true;
                builder.setVibrate(alert.getVibratePattern());
                defaults &= ~Notification.DEFAULT_VIBRATE;
            } else if (alert.hasVibrate()) {
                if (alert.getVibrate()) {
                    vibrates = true;
                    defaults |= Notification.DEFAULT_VIBRATE;
                } else {
                    vibrates = false;
                    defaults &= ~Notification.DEFAULT_VIBRATE;
                }
            }
        }
        if (alert.getSoundUri() != null) {
            noisy = true;
            noisyCustomUri = true;
            builder.setSound(alert.getSoundUri());
            defaults &= ~Notification.DEFAULT_SOUND;
        } else if (alert.hasSound()) {
            if (alert.getSound()) {
                noisy = true;
                defaults |= Notification.DEFAULT_SOUND;
            } else {
                noisy = false;
                defaults &= ~Notification.DEFAULT_SOUND;
            }
        }
        // TODO Enable when switching to androidx, where the NotificationCompat.Builder handles it
        //if (alert.getAllowSystemGeneratedContextualActions() != null) {
        //    builder.setAllowSystemGeneratedContextualActions(alert.getAllowSystemGeneratedContextualActions());
        //}
        if (alert.getBadgeIconType() != null) {
            builder.setBadgeIconType(alert.getBadgeIconTypeInt());
        }
        // TODO Enable when switching to androidx, where the NotificationCompat.Builder handles it
        //if (alert.getChronometerCountDown() != null) {
        //    builder.setChronometerCountDown(alert.getChronometerCountDown());
        //}
        if (alert.getColorized() != null) {
            builder.setColorized(true);
        }
        if (alert.getExtras() != null) {
            builder.addExtras(JSONUtil.toBundle(alert.getExtras()));
        }
        if (alert.getTimeoutAfter() != null) {
            builder.setTimeoutAfter(alert.getTimeoutAfter());
        }

        // Apply channel options for importance
        if (channel.getImportance() != null) {
            switch (channel.getImportance()) {
                case NotificationManagerCompat.IMPORTANCE_MAX:
                    builder.setPriority(NotificationCompat.PRIORITY_MAX);
                    break;
                case NotificationManagerCompat.IMPORTANCE_HIGH:
                    builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                    break;
                case NotificationManagerCompat.IMPORTANCE_DEFAULT:
                    builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    break;
                case NotificationManagerCompat.IMPORTANCE_LOW:
                    builder.setPriority(NotificationCompat.PRIORITY_LOW);
                    break;
                case NotificationManagerCompat.IMPORTANCE_MIN:
                case NotificationManagerCompat.IMPORTANCE_NONE:
                    builder.setPriority(NotificationCompat.PRIORITY_MIN);
                    break;
                case NotificationManagerCompat.IMPORTANCE_UNSPECIFIED:
                    // noop
                    break;
                default:
                    if (channel.getImportance() < NotificationManagerCompat.IMPORTANCE_NONE) {
                        builder.setPriority(NotificationCompat.PRIORITY_MIN);
                    } else if (channel.getImportance() > NotificationManagerCompat.IMPORTANCE_MAX) {
                        builder.setPriority(NotificationCompat.PRIORITY_MAX);
                    }
                    break;
            }
        }

        // Apply channel options for lights
        if (channel.getLights() != null) {
            if (channel.getLights()) {
                if (!lights) {
                    lights = true;
                    lightsCustomized = false;
                    defaults |= Notification.DEFAULT_LIGHTS;
                }
            } else {
                if (lights) {
                    lights = false;
                    builder.setLights(Color.TRANSPARENT, 0, 0);
                    defaults &= ~Notification.DEFAULT_LIGHTS;
                }
            }
        }
        if (lights && channel.getLightColor() != null) {
            if (channel.getLightColor() == Color.TRANSPARENT) {
                lights = false;
                builder.setLights(Color.TRANSPARENT, 0, 0);
                defaults &= ~Notification.DEFAULT_LIGHTS;
            } else {
                lightsCustomized = true;
                builder.setLights(channel.getLightColor(), alert.getLightsOn(), alert.getLightsOff());
                defaults &= ~Notification.DEFAULT_LIGHTS;
            }
        }

        // Apply channel options for vibration
        if (canVibrate) {
            if (channel.getVibrate() != null) {
                if (channel.getVibrate()) {
                    if (!vibrates) {
                        vibrates = true;
                        defaults |= Notification.DEFAULT_VIBRATE;
                    }
                } else {
                    if (vibrates) {
                        vibrates = false;
                        vibratesCustomPattern = false;
                        defaults &= ~Notification.DEFAULT_VIBRATE;
                        builder.setVibrate(null);
                    }
                }
            }
            if (vibrates && channel.getVibrationPattern() != null) {
                vibrates = true;
                vibratesCustomPattern = true;
                defaults &= ~Notification.DEFAULT_VIBRATE;
                builder.setVibrate(channel.getVibrationPattern());
            }
        }

        // Apply channel options for sound
        if (channel.getSound() != null) {
            if (channel.getSound()) {
                if (!noisy) {
                    noisy = true;
                    defaults |= Notification.DEFAULT_SOUND;
                }
            } else {
                if (noisy) {
                    noisy = false;
                    defaults &= ~Notification.DEFAULT_SOUND;
                    builder.setSound(null);
                }
            }
        }
        if (noisy && channel.getSoundUri() != null) {
            defaults &= ~Notification.DEFAULT_SOUND;
            builder.setSound(channel.getSoundUri());
        }

        // Apply channel options for vibration in silent mode
        if (channel.getVibrateInSilentMode() != null) {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                if (channel.getVibrateInSilentMode()) {
                    if (!vibrates && !noisy) {
                        vibrates = true;
                        defaults |= Notification.DEFAULT_VIBRATE;
                    }
                } else {
                    if (vibrates || noisy) {
                        vibrates = false;
                        noisy = false;
                        defaults &= ~Notification.DEFAULT_VIBRATE;
                        defaults &= ~Notification.DEFAULT_SOUND;
                        builder.setSound(null);
                        builder.setVibrate(null);
                    }
                }
            }
        }

        // Apply channel options for lockscreen visibility
        if (channel.getLockscreenVisibility() != null) {
            builder.setVisibility(channel.getLockscreenVisibility());
        }

        // Apply channel options for color
        if (channel.getColor() != null) {
            builder.setColor(channel.getColor());
        }

        // Apply channel options for local only
        if (channel.getLocalOnly() != null) {
            builder.setLocalOnly(channel.getLocalOnly());
        }

        builder.setDefaults(defaults);

        if (alert.getButtons() != null) {
            int i = 0;
            for (NotificationButtonModel button : alert.getButtons()) {
                PendingIntent buttonPendingIntent = pendingIntentBuilder.buildForButton(i);
                int icon = button.icon;
                if (icon == 0) {
                    icon = android.R.color.transparent;
                }
                builder.addAction(
                        new NotificationCompat.Action.Builder(icon, button.label, buttonPendingIntent)
                                .build());
                ++i;
            }
        }

        switch (alert.getType()) {
            case NONE:
                // Explicitly no particular style
                builder.setStyle(null);
                break;
            default:
                Log.e(TAG, "Unhandled notification type " + alert.getType());
                // $FALLTHROUGH
            case NULL:
                // No specific style configured
                builder.setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(alert.getText()));
                break;
            case BIG_TEXT:
                AlertBigTextModel alertBigText = (AlertBigTextModel) alert;
                builder.setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(alertBigText.getBigTitle()) // automatically falls back to alert.getTitle()
                        .bigText(alertBigText.getBigText() != null ? alertBigText.getBigText() : alert.getText())
                        .setSummaryText(alertBigText.getSummaryText())
                );
                break;
            case BIG_PICTURE:
                AlertBigPictureModel alertBigPicture = (AlertBigPictureModel) alert;
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigLargeIcon(alertBigPicture.hasBigLargeIcon() ? alertBigPicture.getBigLargeIcon() : alert.getLargeIcon())
                        .bigPicture(alertBigPicture.getBigPicture())
                        .setBigContentTitle(alertBigPicture.getBigTitle()) // automatically falls back to alert.getTitle()
                        .setSummaryText(alertBigPicture.getSummaryText() != null ? alertBigPicture.getSummaryText() : alert.getText())
                );
                break;
            case INBOX:
                AlertInboxModel alertInbox = (AlertInboxModel) alert;
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
                        .setBigContentTitle(alertInbox.getBigTitle()) // automatically falls back to alert.getTitle()
                        .setSummaryText(alertInbox.getSummaryText());
                if (alertInbox.getLines() != null) {
                    for (CharSequence line : alertInbox.getLines()) {
                        style.addLine(line);
                    }
                } else {
                    // We could split the text by lines, but a CharSequence (in HTML mode) is impractical
                    style.addLine(alert.getText());
                }
                builder.setStyle(style);
                break;
        }

        return builder.build();
    }

    @TargetApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private static void closeSystemDialogs(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
    }

    public static void ensureNotificationDismissed(Context context, Intent intent, NotificationModel notif) {
        // Manually dismiss the notification and close the system drawer, when an action button is clicked.
        // May be a kind of bug, or may be a feature when the associated PendingIntent resolves a Service instead of an Activity.
        NotificationManager.closeSystemDialogs(context);
        String localNotificationIdStr = intent.getData().getQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER_LOCAL_NOTIFICATION_ID);
        int localNotificationId = -1;
        try {
            if (localNotificationIdStr != null) {
                localNotificationId = Integer.parseInt(localNotificationIdStr);
            }
        } catch (Exception ignored) { // NumberFormatException
            WonderPush.logError("Failed to parse localNotificationId " + localNotificationIdStr, ignored);
        }
        cancel(context, generateLocalNotificationTag(notif), localNotificationId);
    }

    public static boolean showPotentialNotification(Context context, Intent intent) {
        boolean isDataNotification = WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE_DATA.equals(
                intent.getStringExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_TYPE));
        if (containsExplicitNotification(intent) || (isDataNotification && containsWillOpenNotificationAutomaticallyOpenable(intent))) {
            final NotificationModel notif = NotificationModel.fromLocalIntent(intent, context);
            if (notif == null) {
                Log.e(TAG, "Failed to extract notification object");
                return false;
            }

            sLastHandledIntentRef = new WeakReference<>(intent);

            if (isDataNotification) {
                // Track data notification opens, and display any in-app
                handleOpenedManuallyDisplayedDataNotification(context, intent, notif);
            }

            InAppManager.handleInApp(context, notif);
            return true;
        }
        return false;
    }

    protected static boolean containsExplicitNotification(Intent intent) {
        return  intent != null
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
                && (sLastHandledIntentRef == null || intent != sLastHandledIntentRef.get())
                && WonderPush.INTENT_NOTIFICATION_TYPE.equals(intent.getType())
                && intent.getData() != null
                && WonderPush.INTENT_NOTIFICATION_SCHEME.equals(intent.getData().getScheme())
                && WonderPush.INTENT_NOTIFICATION_AUTHORITY.equals(intent.getData().getAuthority())
                ;
    }

    protected static boolean containsWillOpenNotification(Intent intent) {
        return  intent != null
                // action may or may not be INTENT_NOTIFICATION_WILL_OPEN
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
                && (sLastHandledIntentRef == null || intent != sLastHandledIntentRef.get())
                && intent.hasExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION)
                ;
    }

    protected static boolean containsWillOpenNotificationAutomaticallyOpenable(Intent intent) {
        return  containsWillOpenNotification(intent)
                && intent.hasExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN) // makes it default to false if removed
                && intent.getBooleanExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_AUTOMATIC_OPEN, false)
                ;
    }

    protected static void handleOpenedNotificationFromTrackingActivity(Context context, Intent intent, NotificationModel notif) {
        ensureNotificationDismissed(context, intent, notif);

        WonderPush.logDebug("Handling opened notification: " + notif.getInputJSONString());
        trackOpenedNotification(intent, notif);
        notifyNotificationOpened(intent, notif);
        handleOpenedNotification(context, intent, notif);
    }

    protected static void handleOpenedManuallyDisplayedDataNotification(Context context, Intent intent, NotificationModel notif) {
        WonderPush.logDebug("Handling opened manually displayed data notification: " + notif.getInputJSONString());
        trackOpenedNotification(intent, notif);
        notifyNotificationOpened(intent, notif);
        handleOpenedNotification(context, intent, notif);
    }

    private static void trackOpenedNotification(Intent intent, NotificationModel notif) {
        int clickedButtonIndex = intent.getIntExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX, -1);
        try {
            JSONObject trackData = new JSONObject();
            trackData.put("campaignId", notif.getCampaignId());
            trackData.put("notificationId", notif.getNotificationId());
            trackData.put("actionDate", TimeSync.getTime());
            if (clickedButtonIndex >= 0 && notif.getAlert() != null && notif.getAlert().getButtons() != null && clickedButtonIndex < notif.getAlert().getButtons().size()) {
                NotificationButtonModel button = notif.getAlert().getButtons().get(clickedButtonIndex);
                trackData.put("buttonLabel", button.label);
            }
            NotificationMetadata metadata = new NotificationMetadata(notif);
            sLastClickedNotificationMetadata = metadata;
            if (WonderPush.isSubscriptionStatusOptIn()) {
                WonderPush.trackInternalEvent("@NOTIFICATION_OPENED", trackData);
            } else {
                WonderPush.countInternalEvent("@NOTIFICATION_OPENED", trackData);
            }

            WonderPushConfiguration.setLastOpenedNotificationInfoJson(trackData);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse notification JSON object", e);
        }
    }

    private static void notifyNotificationOpened(Intent intent, NotificationModel notif) {
        boolean fromUserInteraction = intent.getBooleanExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_FROM_USER_INTERACTION, true);
        Intent receivedPushNotificationIntent = intent.getParcelableExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION);
        int buttonIndex = intent.getIntExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX, -1);

        // Notify the application that the notification has been opened
        Intent notificationOpenedIntent = new Intent(WonderPush.INTENT_NOTIFICATION_OPENED);
        notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_FROM_USER_INTERACTION, fromUserInteraction);
        notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION, receivedPushNotificationIntent);
        notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_NOTIFICATION_MODEL, notif);
        notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_BUTTON_INDEX, buttonIndex);
        LocalBroadcastManager.getInstance(WonderPush.getApplicationContext()).sendBroadcast(notificationOpenedIntent);
    }

    private static void handleOpenedNotification(Context context, Intent intent, NotificationModel notif) {
        int clickedButtonIndex = intent.getIntExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_BUTTON_INDEX, -1);
        List<ActionModel> actions = null;
        if (clickedButtonIndex < 0) {
            // Notification opened actions
            actions = notif.getActions();
        } else if (
                notif.getAlert() != null
                && notif.getAlert().getButtons() != null
                && clickedButtonIndex < notif.getAlert().getButtons().size()
        ) {
            // Notification button-specific actions
            actions = notif.getAlert().getButtons().get(clickedButtonIndex).actions;
        }
        handleActions(context, new NotificationMetadata(notif), actions);
    }

    public static void handleActions(Context context, NotificationMetadata notificationMetadata, List<ActionModel> actions) {
        if (actions == null)
            return;

        try {
            for (ActionModel action : actions) {
                handleAction(context, notificationMetadata, action);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling actions", ex);
        }
    }

    protected static void handleAction(Context context, NotificationMetadata notificationMetadata, ActionModel action) {
        try {
            if (action == null || action.getType() == null) {
                // Skip unrecognized action types
                return;
            }
            WonderPush.logDebug("Running action " + action.getType());
            switch (action.getType()) {
                case CLOSE:
                    // Noop
                    break;
                case MAP_OPEN:
                    handleMapOpenAction(context, action);
                    break;
                case LINK:
                    handleLinkAction(context, action);
                    break;
                case RATING:
                    handleRatingAction(context, action);
                    break;
                case TRACK_EVENT:
                    handleTrackEventAction(notificationMetadata, action);
                    break;
                case UPDATE_INSTALLATION:
                    handleUpdateInstallationAction(action);
                    break;
                case ADD_PROPERTY:
                    handleAddPropertyAction(action);
                    break;
                case REMOVE_PROPERTY:
                    handleRemovePropertyAction(action);
                    break;
                case RESYNC_INSTALLATION:
                    handleResyncInstallationAction(action);
                    break;
                case ADD_TAG:
                    handleAddTagAction(action);
                    break;
                case REMOVE_TAG:
                    handleRemoveTagAction(action);
                    break;
                case SUBSCRIBE_TO_NOTIFICATIONS:
                    handleSubscribeToNotifications(action);
                    break;
                case UNSUBSCRIBE_FROM_NOTIFICATIONS:
                    handleUnsubscribeFromNotifications(action);
                    break;
                case REMOVE_ALL_TAGS:
                    handleRemoveAllTagsAction(action);
                    break;
                case METHOD:
                    handleMethodAction(action);
                    break;
                case CLOSE_NOTIFICATIONS:
                    handleCloseNotifications(context, action);
                    break;
                case _DUMP_STATE:
                    handleDumpStateAction(action);
                    break;
                case _OVERRIDE_SET_LOGGING:
                    handleOverrideSetLoggingAction(action);
                    break;
                case _OVERRIDE_NOTIFICATION_RECEIPT:
                    handleOverrideNotificationReceiptAction(action);
                    break;
                default:
                    Log.w(TAG, "Unhandled action \"" + action.getType() + "\"");
                    break;
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling action " + action, ex);
        }
    }

    protected static void handleLinkAction(Context context, ActionModel action) {
        try {
            String url = action.getUrl();
            if (url == null) {
                Log.e(TAG, "No url in a " + ActionModel.Type.LINK + " action!");
                return;
            }

            url = WonderPush.delegateUrlForDeepLink(new DeepLinkEvent(context, url));
            if (url == null) return;

            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No service for intent " + intent, e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform a " + ActionModel.Type.LINK + " action", e);
        }
    }

    protected static void handleRatingAction(Context context, ActionModel action) {
        try {
            String url = "market://details?id=" + context.getPackageName();

            url = WonderPush.delegateUrlForDeepLink(new DeepLinkEvent(context, url));
            if (url == null) return;

            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No service for intent " + intent, e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform a " + ActionModel.Type.RATING + " action", e);
        }
    }

    protected static void handleTrackEventAction(NotificationMetadata notificationMetadata, ActionModel action) {
        JSONObject event = action.getEvent();
        if (event == null) {
            Log.e(TAG, "Got no event to track for a " + ActionModel.Type.TRACK_EVENT + " action");
            return;
        }
        if (!event.has("type") || JSONUtil.getString(event, "type") == null) {
            Log.e(TAG, "Got no type in the event to track for a " + ActionModel.Type.TRACK_EVENT + " action");
            return;
        }
        JSONObject trackingData = new JSONObject();
        try {
            trackingData.putOpt("campaignId", notificationMetadata.getCampaignId());
            trackingData.putOpt("notificationId", notificationMetadata.getNotificationId());
        } catch (JSONException ex) {
            Log.e(TAG, "Unexpected error while adding notification tracking info in trackEvent", ex);
        }
        WonderPush.trackEvent(JSONUtil.getString(event, "type"), trackingData, event.optJSONObject("custom"));
    }

    protected static void handleUpdateInstallationAction(ActionModel action) throws JSONException {
        JSONObject installation = action.getInstallation();
        JSONObject custom = installation != null ? installation.optJSONObject("custom") : action.getCustom();
        if (installation == null && custom != null) {
            installation = new JSONObject();
            installation.put("custom", custom);
        }
        if (installation == null) return;
        try {
            if (action.getAppliedServerSide(false)) {
                WonderPush.logDebug("Received server custom properties diff: " + custom);
                JSONSyncInstallation.forCurrentUser().receiveDiff(installation);
            } else {
                WonderPush.logDebug("Putting custom properties diff: " + custom);
                JSONSyncInstallation.forCurrentUser().put(installation);
            }
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to handle action " + ActionModel.Type.UPDATE_INSTALLATION, ex);
        }
    }

    protected static void handleAddPropertyAction(ActionModel action) {
        JSONObject installation = action.getInstallation();
        JSONObject custom = installation != null ? installation.optJSONObject("custom") : action.getCustom();
        if (custom == null) {
            Log.e(TAG, "Got no installation custom properties to update for a " + ActionModel.Type.ADD_PROPERTY + " action");
            return;
        }
        if (custom.length() == 0) {
            WonderPush.logDebug("Empty installation custom properties for an update, for a " + ActionModel.Type.ADD_PROPERTY + " action");
            return;
        }
        try {
            Iterator<String> it = custom.keys();
            while (it.hasNext()) {
                String field = it.next();
                Object value = custom.get(field);
                WonderPush.addProperty(field, value);
            }
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to handle action " + ActionModel.Type.ADD_PROPERTY, ex);
        }
    }

    protected static void handleRemovePropertyAction(ActionModel action) {
        JSONObject installation = action.getInstallation();
        JSONObject custom = installation != null ? installation.optJSONObject("custom") : action.getCustom();
        if (custom == null) {
            Log.e(TAG, "Got no installation custom properties to update for a " + ActionModel.Type.REMOVE_PROPERTY + " action");
            return;
        }
        if (custom.length() == 0) {
            WonderPush.logDebug("Empty installation custom properties for an update, for a " + ActionModel.Type.REMOVE_PROPERTY + " action");
            return;
        }
        try {
            Iterator<String> it = custom.keys();
            while (it.hasNext()) {
                String field = it.next();
                Object value = custom.get(field);
                WonderPush.removeProperty(field, value);
            }
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to handle action " + ActionModel.Type.REMOVE_PROPERTY, ex);
        }
    }

    protected static void handleResyncInstallationAction(final ActionModel action) {
        if (action.getInstallation() != null) {
            handleResyncInstallationAction_inner(action);
        } else {
            WonderPush.logDebug("Fetching installation for action " + ActionModel.Type.RESYNC_INSTALLATION);
            WonderPush.get("/installation", null, new ResponseHandler() {
                @Override
                public void onFailure(Throwable ex, Response errorResponse) {
                    Log.e(WonderPush.TAG, "Failed to fetch installation for running action " + ActionModel.Type.RESYNC_INSTALLATION + ", got " + errorResponse, ex);
                }

                @Override
                public void onSuccess(Response response) {
                    if (response.isError()) {
                        Log.e(WonderPush.TAG, "Failed to fetch installation for running action " + ActionModel.Type.RESYNC_INSTALLATION + ", got " + response);
                    } else {
                        ActionModel enrichedAction = null;
                        try {
                            enrichedAction = (ActionModel) action.clone();
                        } catch (CloneNotSupportedException ex) {
                            WonderPush.logError("Failed to clone action " + action, ex);
                            enrichedAction = action;
                        }
                        JSONObject installation = response.getJSONObject();
                        Iterator<String> it = installation.keys();
                        while (it.hasNext()) {
                            String key = it.next();
                            if (key.startsWith("_")) {
                                it.remove();
                            }
                        }
                        WonderPush.logDebug("Got installation: " + installation);
                        enrichedAction.setInstallation(installation);
                        handleResyncInstallationAction_inner(enrichedAction);
                    }
                }
            });
        }
    }

    private static void handleResyncInstallationAction_inner(ActionModel action) {
        JSONObject installation = action.getInstallation();
        if (installation == null) installation = new JSONObject();

        // Take or reset custom
        try {
            if (action.getReset(false)) {
                JSONSyncInstallation.forCurrentUser().receiveState(installation, action.getForce(false));
            } else {
                JSONSyncInstallation.forCurrentUser().receiveServerState(installation);
            }
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to resync installation", ex);
        }

        WonderPush.refreshPreferencesAndConfiguration(true);
    }

    private static void handleAddTagAction(ActionModel action) {
        JSONArray actionTags = action.getTags();
        if (actionTags == null) return;
        ArrayList<String> tags = new ArrayList<>(actionTags.length());
        for (int i = 0, e = actionTags.length(); i < e; ++i) {
            try {
                Object item = actionTags.get(i);
                if (item instanceof String) {
                    tags.add((String) item);
                }
            } catch (JSONException ex) {
                Log.e(WonderPush.TAG, "Unexpected error while getting an item of the tags array for the addTag action", ex);
            }
        }
        WonderPush.addTag(tags.toArray(new String[0]));
    }

    private static void handleSubscribeToNotifications(ActionModel action) {
        WonderPush.subscribeToNotifications();
    }

    private static void handleUnsubscribeFromNotifications(ActionModel action) {
        WonderPush.unsubscribeFromNotifications();
    }

    private static void handleRemoveTagAction(ActionModel action) {
        JSONArray actionTags = action.getTags();
        if (actionTags == null) return;
        ArrayList<String> tags = new ArrayList<>(actionTags.length());
        for (int i = 0, e = actionTags.length(); i < e; ++i) {
            try {
                Object item = actionTags.get(i);
                if (item instanceof String) {
                    tags.add((String) item);
                }
            } catch (JSONException ex) {
                Log.e(WonderPush.TAG, "Unexpected error while getting an item of the tags array for the addTag action", ex);
            }
        }
        WonderPush.removeTag(tags.toArray(new String[0]));
    }

    private static void handleRemoveAllTagsAction(ActionModel action) {
        WonderPush.removeAllTags();
    }

    protected static void handleMethodAction(ActionModel action) {
        String method = action.getMethod();
        String arg = action.getMethodArg();
        if (method == null) {
            Log.e(TAG, "Got no method to call for a " + ActionModel.Type.METHOD + " action");
            return;
        }
        Intent intent = new Intent();
        intent.setPackage(WonderPush.getApplicationContext().getPackageName());
        intent.setAction(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_ACTION);
        intent.setData(new Uri.Builder()
                .scheme(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_SCHEME)
                .authority(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_AUTHORITY)
                .appendPath(method)
                .build());
        intent.putExtra(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_METHOD, method);
        intent.putExtra(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_ARG, arg);
        LocalBroadcastManager.getInstance(WonderPush.getApplicationContext()).sendBroadcast(intent);
    }

    private static void handleMapOpenAction(Context context, ActionModel action) {
        try {
            NotificationMapModel.Place place;
            try {
                place = action.getMap().getPlace();
            } catch (Exception e) {
                Log.e(NotificationManager.TAG, "Could not get the place from the map", e);
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

            String url = geo.build().toString();
            url = WonderPush.delegateUrlForDeepLink(new DeepLinkEvent(context, url));
            if (url == null) return;

            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setData(Uri.parse(url));
            try {
                WonderPush.logDebug("Will open location " + open.getDataString());
                context.startActivity(open);
            } catch (ActivityNotFoundException e1) {
                WonderPush.logDebug("No activity can open location " + open.getDataString());
                WonderPush.logDebug("Falling back to regular URL");
                geo = new Uri.Builder();
                geo.scheme("https");
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
                try {
                    WonderPush.logDebug("Opening URL " + open.getDataString());
                    context.startActivity(open);
                } catch (ActivityNotFoundException e2) {
                    WonderPush.logDebug("No activity can open URL " + open.getDataString());
                    Log.w(NotificationManager.TAG, "Cannot open map!");
                    Toast.makeText(context, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(NotificationManager.TAG, "Unexpected error while opening map", e);
            Toast.makeText(context, R.string.wonderpush_could_not_open_location, Toast.LENGTH_SHORT).show();
        }
    }

    protected static void handleCloseNotifications(Context context, ActionModel action) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
                Notification notification = sbn.getNotification();
                // Filter notification channel
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (action.hasChannel()) {
                        if (action.getChannel() == null && !(notification.getChannelId() == null || notification.getChannelId().equals(WonderPushUserPreferences.getDefaultChannelId()))) {
                            continue;
                        } else if (action.getChannel() != null && !action.getChannel().equals(notification.getChannelId())) {
                            continue;
                        }
                    }
                }
                // Filter group
                if (action.hasGroup()) {
                    if (action.getGroup() == null && notification.getGroup() != null
                            || action.getGroup() != null && !action.getGroup().equals(notification.getGroup())) {
                        continue;
                    }
                }
                // Filter tag
                if (action.hasTag()) {
                    if (action.getTag() == null && sbn.getTag() != null
                            || action.getTag() != null && !action.getTag().equals(sbn.getTag())) {
                        continue;
                    }
                }
                // Filter category
                if (action.hasCategory()) {
                    if (action.getCategory() == null && notification.category != null
                            || action.getCategory() != null && !action.getCategory().equals(notification.category)) {
                        continue;
                    }
                }
                // Filter sort key
                if (action.hasSortKey()) {
                    if (action.getSortKey() == null && notification.getSortKey() != null
                            || action.getSortKey() != null && !action.getSortKey().equals(notification.getSortKey())) {
                        continue;
                    }
                }
                // Filter on extras
                Bundle extras = JSONUtil.toBundle(action.getExtras());
                if (extras != null && !extras.isEmpty()) {
                    if (notification.extras == null) {
                        continue;
                    }
                    boolean remove = true;
                    for (String key : extras.keySet()) {
                        Object value = extras.get(key);
                        if (value == null && (notification.extras.containsKey(key) || notification.extras.get(key) != null)) {
                            remove = false;
                        // TODO Proper per-type comparison
                        } else if (value != null && (notification.extras.get(key) == null || !value.toString().equals(notification.extras.get(key).toString()))) {
                            remove = false;
                        }
                        if (!remove) {
                            break;
                        }
                    }
                    if (!remove) {
                        continue;
                    }
                }
                // All filters passed, cancel this notification
                notificationManager.cancel(sbn.getTag(), sbn.getId());
            }
        }
    }

    private static void handleDumpStateAction(ActionModel action) {
        JSONObject stateDump = WonderPushConfiguration.dumpState();
        Log.d(WonderPush.TAG, "STATE DUMP: " + stateDump);
        if (stateDump == null) stateDump = new JSONObject();
        JSONObject custom = new JSONObject();
        try {
            custom.put("ignore_sdkStateDump", stateDump);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to add state dump to event custom", ex);
        }
        WonderPush.trackInternalEvent("@DEBUG_DUMP_STATE", null, custom);
    }

    private static void handleOverrideSetLoggingAction(ActionModel action) {
        Boolean value = action.getForce();
        Log.d(WonderPush.TAG, "OVERRIDE setLogging: " + value);
        WonderPushConfiguration.setOverrideSetLogging(value);
        WonderPush.applyOverrideLogging(value);
    }

    private static void handleOverrideNotificationReceiptAction(ActionModel action) {
        Boolean value = action.getForce();
        Log.d(WonderPush.TAG, "OVERRIDE notification receipt: " + value);
        WonderPushConfiguration.setOverrideNotificationReceipt(value);
    }

}
