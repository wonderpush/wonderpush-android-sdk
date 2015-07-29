package com.wonderpush.sdk;

import java.io.IOException;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.wonderpush.sdk.NotificationModel.NotTargetedForThisInstallationException;

/**
 * A class that handles all the messages form Google Cloud Messaging service
 */
class WonderPushGcmClient {

    private static final String TAG = WonderPush.TAG;

    static final String WONDERPUSH_NOTIFICATION_EXTRA_KEY = "_wp";
    private static GoogleCloudMessaging mGcm;

    private static void storeRegistrationIdToWonderPush(String registrationId) {
        try {
            JSONObject properties = new JSONObject();
            JSONObject pushToken = new JSONObject();
            pushToken.put("data", registrationId);
            properties.put("pushToken", pushToken);

            if (System.currentTimeMillis() - WonderPushConfiguration.getCachedGCMRegistrationIdDate() > WonderPush.CACHED_REGISTRATION_ID_DURATION
                    || registrationId == null && WonderPushConfiguration.getGCMRegistrationId() != null
                    || registrationId != null && !registrationId.equals(WonderPushConfiguration.getGCMRegistrationId())) {
                WonderPush.updateInstallation(properties, false, null);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update push token to WonderPush", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while updating push token to WonderPush", e);
        }
    }

    protected static String getRegistrationId() {
        String registrationId = WonderPushConfiguration.getGCMRegistrationId();
        if (TextUtils.isEmpty(registrationId)) {
            return null;
        }

        int registeredVersion = WonderPushConfiguration.getGCMRegistrationAppVersion();
        int currentVersion = WonderPush.getApplicationVersionCode();
        if (registeredVersion != currentVersion) {
            return null;
        }

        // This function deliberately does not check for cases that should cause unregistration (senderIds change)

        return registrationId;
    }

    protected static boolean checkForUnregistrationNeed(Context c, String pushSenderIds) {
        String registeredSenderIds = WonderPushConfiguration.getGCMRegistrationSenderIds();
        return !(
                registeredSenderIds == null // there is no previous pushToken to unregister
                || registeredSenderIds.equals(pushSenderIds) // change of senderIds
        );
    }

    protected static void storeRegistrationId(String senderIds, String registrationId) {
        WonderPushConfiguration.setGCMRegistrationId(registrationId);
        WonderPushConfiguration.setGCMRegistrationSenderIds(senderIds);
        WonderPushConfiguration.setGCMRegistrationAppVersion(WonderPush.getApplicationVersionCode());
    }

    protected static PendingIntent buildPendingIntent(NotificationModel notif, Intent pushIntent, boolean fromUserInteraction,
            Bundle extrasOverride, Context context, Class<? extends Activity> activity) {
        Intent resultIntent = new Intent();
        if (WonderPushService.isProperlySetup()) {
            resultIntent.setClass(context, WonderPushService.class);
        } else {
            // Fallback to blindly launching the configured activity
            resultIntent.setClass(context, activity);
            resultIntent = new Intent(context, activity);
        }
        resultIntent.putExtra("activity", activity.getCanonicalName());
        resultIntent.putExtra("receivedPushNotificationIntent", pushIntent);
        resultIntent.putExtra("fromUserInteraction", fromUserInteraction);
        if (extrasOverride != null) {
            resultIntent.putExtras(extrasOverride);
        }

        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_SINGLE_TOP);

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

        NotificationModel notif = null;
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
            WonderPush.ensureInitialized(context);
            WonderPush.trackInternalEvent("@NOTIFICATION_RECEIVED", trackData);

            WonderPushConfiguration.setLastReceivedNotificationInfoJson(trackData);

            boolean allowAutomaticOpen = notif.getType() != NotificationModel.Type.SIMPLE && notif.getType() != NotificationModel.Type.DATA;

            boolean automaticallyOpened = false;
            Activity currentActivity = WonderPush.getCurrentActivity();
            boolean appInForeground = currentActivity != null && !currentActivity.isFinishing();
            if (allowAutomaticOpen && appInForeground) {
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
                    String campaignId = notif.getCampaignId();
                    if (campaignId == null) campaignId = "MISSING CAMPAIGN ID";
                    int localNotificationId = campaignId.hashCode();
                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(localNotificationId, notification);
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

    private static void unregister(Context context) throws IOException {
        if (mGcm == null) {
            mGcm = GoogleCloudMessaging.getInstance(context);
        }
        mGcm.unregister();
    }

    private static String register(Context context, String senderId) throws IOException {
        if (mGcm == null) {
            mGcm = GoogleCloudMessaging.getInstance(context);
        }
        return mGcm.register(senderId.split(","));
    }

    private static void registerInBackground(final String senderIds, final Context activity) {
        // Get off the main UI thread for using GCM
        new AsyncTask<Object, Object, String>() {
            @Override
            protected String doInBackground(Object... dummy) {
                try {
                    if (checkForUnregistrationNeed(activity, senderIds)) {
                        unregister(activity);
                    }
                    final String regid = register(activity, senderIds);
                    if (regid == null) {
                        WonderPush.logError("Device could not register");
                    } else {
                        WonderPush.logDebug("Device registered, registration ID=" + regid);
                    }
                    return regid;
                } catch (IOException ex) {
                    Log.w(TAG, "Could not register for push notifications", ex);
                } catch (Exception ex) {
                    Log.w(TAG, "Could not register for push notifications", ex);
                }
                return null;
            }
            @Override
            protected void onPostExecute(String regid) {
                // We're back on the main thread, this is required for potential deferring due to OpenUDID not being ready
                if (regid != null) {
                    storeRegistrationId(senderIds, regid);
                    storeRegistrationIdToWonderPush(regid);
                }
            }
        }.execute();
    }

    /**
     * Start the registration process for GCM.
     *
     * @param context
     *            A valid context
     */
    static void registerForPushNotification(Context context) {
        String pushSenderId = null;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            pushSenderId = bundle.getString("GCMSenderId");
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Could not get GCMSenderId meta data from your manifest. Did you add: <meta-data android:name=\"GCMSenderId\" android:value=\"@string/push_sender_ids\"/> under <application> in your AndroidManifest.xml?");
        } catch (NullPointerException e) {
            Log.e(TAG, "Could not get GCMSenderId meta data from your manifest. Did you add: <meta-data android:name=\"GCMSenderId\" android:value=\"@string/push_sender_ids\"/> under <application> in your AndroidManifest.xml?");
        }

        if (pushSenderId == null) {
            return;
        }

        String regid = getRegistrationId();

        if (checkForUnregistrationNeed(context, pushSenderId)
                || TextUtils.isEmpty(regid)
                || System.currentTimeMillis() - WonderPushConfiguration.getCachedGCMRegistrationIdDate() > WonderPush.CACHED_REGISTRATION_ID_DURATION) {
            registerInBackground(pushSenderId, context);
        } // already pushed by WonderPush.updateInstallationCoreProperties()
        return;
    }

}
