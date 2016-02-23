package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.wonderpush.sdk.NotificationModel.NotTargetedForThisInstallationException;

/**
 * A class that handles all the messages form Google Cloud Messaging service
 */
class WonderPushGcmClient {

    private static final String TAG = WonderPush.TAG;

    static final String WONDERPUSH_NOTIFICATION_EXTRA_KEY = "_wp";

    protected static PendingIntent buildPendingIntent(NotificationModel notif, Intent pushIntent, boolean fromUserInteraction,
            Bundle extrasOverride, Context context, Class<? extends Activity> activity) {
        Intent resultIntent = new Intent();
        if (WonderPushService.isProperlySetup()) {
            resultIntent.setClass(context, WonderPushService.class);
        } else if (activity != null) {
            // Fallback to blindly launching the configured activity
            resultIntent.setClass(context, activity);
            resultIntent = new Intent(context, activity);
        } // else We have nothing to propose!
        if (activity != null) {
            resultIntent.putExtra("activity", activity.getCanonicalName());
        }
        resultIntent.putExtra("receivedPushNotificationIntent", pushIntent);
        resultIntent.putExtra("fromUserInteraction", fromUserInteraction);
        if (extrasOverride != null) {
            resultIntent.putExtras(extrasOverride);
        }

        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | WonderPushCompatibilityHelper.getIntentFlagActivityNewDocument() | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Uri dataUri = new Uri.Builder()
                .scheme(WonderPush.INTENT_NOTIFICATION_SCHEME)
                .authority(WonderPush.INTENT_NOTIFICATION_AUTHORITY)
                .appendQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER, notif.getInputJSONString())
                .build();
        resultIntent.setDataAndType(dataUri, WonderPush.INTENT_NOTIFICATION_TYPE);

        PendingIntent resultPendingIntent;
        if (WonderPushService.isProperlySetup()) {
            resultPendingIntent = PendingIntent.getService(context, 0, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        } else {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            resultPendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        }

        return resultPendingIntent;
    }

    protected static Notification buildNotification(NotificationModel notif, Context context, int iconResource,
            PendingIntent pendingIntent) {
        final PackageManager pm = context.getApplicationContext().getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            ai = null;
        } catch (NullPointerException e) {
            ai = null;
        }

        // Read notification content override if application is foreground
        Activity currentActivity = WonderPush.getCurrentActivity();
        boolean appInForeground = currentActivity != null && !currentActivity.isFinishing();
        AlertModel alert = notif.getAlert() == null ? null : notif.getAlert().forCurrentSettings(appInForeground);
        if (alert == null || (alert.getTitle() == null && alert.getText() == null)) {
            // Nothing to display, don't create a notification
            return null;
        }
        // Apply defaults
        if (alert.getTitle() == null) {
            alert.setTitle((String) (ai != null ? pm.getApplicationLabel(ai) : null));
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(alert.getTitle())
                .setContentText(alert.getText())
                .setPriority(alert.getPriority())
                .setSmallIcon(iconResource);

        mBuilder.setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        if (context.getPackageManager().checkPermission(android.Manifest.permission.VIBRATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        return notification;
    }

    protected static boolean onBroadcastReceived(Context context, Intent intent, int iconResource, Class<? extends Activity> activity) {
        WonderPush.ensureInitialized(context);

        NotificationModel notif;
        try {
            notif = NotificationModel.fromGCMBroadcastIntent(intent);
        } catch (NotTargetedForThisInstallationException ex) {
            WonderPush.logDebug(ex.getMessage());
            return true;
        }
        if (notif == null) {
            return false;
        }

        try {
            String loggedInstallationId = WonderPushConfiguration.getInstallationId();
            if (notif.getTargetedInstallation() != null && !notif.getTargetedInstallation().equals(loggedInstallationId)) {
                WonderPush.logDebug("Received notification is not targetted at the current installation (" + notif.getTargetedInstallation() + " does not match current installation " + loggedInstallationId + ")");
                return true;
            }

            final JSONObject trackData = new JSONObject();
            trackData.put("campaignId", notif.getCampaignId());
            trackData.put("notificationId", notif.getNotificationId());
            trackData.put("actionDate", WonderPush.getTime());
            if (notif.getReceipt()) {
                WonderPush.trackInternalEvent("@NOTIFICATION_RECEIVED", trackData);
            }
            WonderPushConfiguration.setLastReceivedNotificationInfoJson(trackData);

            if (!NotificationModel.Type.DATA.equals(notif.getType())) {

                boolean automaticallyOpened = false;
                Activity currentActivity = WonderPush.getCurrentActivity();
                boolean appInForeground = currentActivity != null && !currentActivity.isFinishing();
                if (notif.getAlert().forCurrentSettings(appInForeground).getAutoOpen()) {
                    WonderPush.logDebug("Automatically opening");
                    // We can show the notification (send the pending intent) right away
                    try {
                        PendingIntent pendingIntent = buildPendingIntent(notif, intent, false, null, context, activity);
                        pendingIntent.send();
                        automaticallyOpened = true;
                    } catch (CanceledException e) {
                        Log.e(WonderPush.TAG, "Could not show notification", e);
                    }
                }
                if (!automaticallyOpened) {
                    // We should use a notification to warn the user, and wait for him to click it
                    // before showing the notification (i.e.: the pending intent being sent)
                    WonderPush.logDebug("Building notification");
                    PendingIntent pendingIntent = buildPendingIntent(notif, intent, true, null, context, activity);
                    Notification notification = buildNotification(notif, context, iconResource, pendingIntent);

                    if (notification == null) {
                        WonderPush.logDebug("No notification is to be displayed");
                        // Fire an Intent to notify the application anyway (especially for `data` notifications)
                        try {
                            Bundle extrasOverride = new Bundle();
                            extrasOverride.putString("overrideTargetUrl",
                                    WonderPush.INTENT_NOTIFICATION_SCHEME + "://" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY
                                            + "/" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_BROADCAST);
                            PendingIntent broadcastPendingIntent = buildPendingIntent(notif, intent, false, extrasOverride, context, activity);
                            broadcastPendingIntent.send();
                        } catch (CanceledException e) {
                            Log.e(WonderPush.TAG, "Could not broadcast the notification will open intent", e);
                        }
                    } else {
                        String tag = notif.getAlert() != null && notif.getAlert().hasTag()
                                ? notif.getAlert().getTag() : notif.getCampaignId();
                        int localNotificationId = tag != null ? 0 : WonderPushConfiguration.getNextTaglessNotificationManagerId();
                        WonderPush.logDebug("Showing notification with tag " + (tag == null ? "(null)" : "\"" + tag + "\"") + " and id " + localNotificationId);
                        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(tag, localNotificationId, notification);
                    }
                }

            }

            return true;
        } catch (JSONException e) {
            WonderPush.logDebug("data is not a well-formed JSON object", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while receiving a notification with intent " + intent, e);
        }
        return false;
    }

    static void registerForPushNotification(final Context context) {
        // Get off the main UI thread for using GCM
        Intent intent = new Intent(context, WonderPushRegistrationIntentService.class);
        context.startService(intent);
    }

}
