package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NotificationManager {

    static final String TAG = WonderPush.TAG;

    private static WeakReference<Intent> sLastHandledIntentRef;

    protected static void onReceivedNotification(Context context, Intent intent, int iconResource, Class<? extends Activity> activity, NotificationModel notif) {
        String loggedInstallationId = WonderPushConfiguration.getInstallationId();
        if (notif.getTargetedInstallation() != null && !notif.getTargetedInstallation().equals(loggedInstallationId)) {
            WonderPush.logDebug("Received notification is not targetted at the current installation (" + notif.getTargetedInstallation() + " does not match current installation " + loggedInstallationId + ")");
            return;
        }

        try {
            final JSONObject trackData = new JSONObject();
            trackData.put("campaignId", notif.getCampaignId());
            trackData.put("notificationId", notif.getNotificationId());
            trackData.put("actionDate", TimeSync.getTime());
            if (notif.getReceipt()) {
                WonderPush.trackInternalEvent("@NOTIFICATION_RECEIVED", trackData);
            }
            WonderPushConfiguration.setLastReceivedNotificationInfoJson(trackData);
        } catch (JSONException ex) {
            WonderPush.logError("Unexpected error while tracking notification received", ex);
        }

        boolean automaticallyHandled = false;
        Activity currentActivity = ActivityLifecycleMonitor.getCurrentActivity();
        boolean appInForeground = currentActivity != null && !currentActivity.isFinishing();
        String tag = generateLocalNotificationTag(notif);
        int localNotificationId = generateLocalNotificationId(tag);
        PendingIntentBuilder pendingIntentBuilder = new PendingIntentBuilder(notif, localNotificationId, intent, context, activity);
        if (notif.getAlert().forCurrentSettings(appInForeground).getAutoDrop()) {
            WonderPush.logDebug("Automatically dropping");
            automaticallyHandled = true;
        } else if (notif.getAlert().forCurrentSettings(appInForeground).getAutoOpen()) {
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
            // We should use a notification to warn the user, and wait for him to click it
            // before showing the notification (i.e.: the pending intent being sent)
            WonderPush.logDebug("Building notification");
            Notification notification = buildNotification(notif, context, iconResource, pendingIntentBuilder);

            if (notification == null) {
                WonderPush.logDebug("No notification is to be displayed");
                // Fire an Intent to notify the application anyway (especially for `data` notifications)
                try {
                    pendingIntentBuilder.buildForWillOpenBroadcast().send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(WonderPush.TAG, "Could not broadcast the notification will open intent", e);
                }
            } else {
                notify(context, tag, localNotificationId, notification);
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
        private final Class<? extends Activity> activity;

        public PendingIntentBuilder(NotificationModel notif, int localNotificationId, Intent pushIntent, Context context, Class<? extends Activity> activity) {
            this.notif = notif;
            this.localNotificationId = localNotificationId;
            this.pushIntent = pushIntent;
            this.context = context;
            this.activity = activity;
        }

        public PendingIntent buildForAutoOpen() {
            return buildPendingIntent(false, null, null);
        }

        public PendingIntent buildForDefault() {
            return buildPendingIntent(true, null, null);
        }

        public PendingIntent buildForButton(int buttonIndex) {
            // The button index cannot be an extra or the PendingIntent of each action will be deduplicated
            // @see Intent#filterEquals(android.content.Intent)
            Map<String, String> extraQueryParams = new HashMap<>(1);
            extraQueryParams.put(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER_BUTTON_INDEX, String.valueOf(buttonIndex));

            Bundle extrasOverride = new Bundle();
            String targetUrl = notif.getAlert().getButtons().get(buttonIndex).targetUrl;
            if (targetUrl != null) {
                extrasOverride.putString("overrideTargetUrl", targetUrl);
            }

            return buildPendingIntent(true, extrasOverride, extraQueryParams);
        }

        public PendingIntent buildForWillOpenBroadcast() {
            Bundle extrasOverride = new Bundle();
            extrasOverride.putString("overrideTargetUrl",
                    WonderPush.INTENT_NOTIFICATION_SCHEME + "://" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_AUTHORITY
                            + "/" + WonderPush.INTENT_NOTIFICATION_WILL_OPEN_PATH_BROADCAST);
            return buildPendingIntent(false, extrasOverride, null);
        }

        private PendingIntent buildPendingIntent(boolean fromUserInteraction, Bundle extrasOverride, Map<String, String> extraQueryParams) {
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

            Uri.Builder dataUriBuilder = new Uri.Builder()
                    .scheme(WonderPush.INTENT_NOTIFICATION_SCHEME)
                    .authority(WonderPush.INTENT_NOTIFICATION_AUTHORITY)
                    .appendQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER, notif.getInputJSONString())
                    .appendQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER_LOCAL_NOTIFICATION_ID, String.valueOf(localNotificationId))
                    ;
            if (extraQueryParams != null) {
                for (Map.Entry<String, String> extraQueryParamEntry : extraQueryParams.entrySet()) {
                    dataUriBuilder.appendQueryParameter(extraQueryParamEntry.getKey(), extraQueryParamEntry.getValue());
                }
            }
            Uri dataUri = dataUriBuilder.build();
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

    }

    protected static Notification buildNotification(NotificationModel notif, Context context, int defaultIconResource,
                                                    PendingIntentBuilder pendingIntentBuilder) {
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
        // Apply defaults
        if (alert.getTitle() == null) {
            final PackageManager pm = context.getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(context.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                ai = null;
            } catch (NullPointerException e) {
                ai = null;
            }
            alert.setTitle((String) (ai != null ? pm.getApplicationLabel(ai) : null));
        }
        if (defaultIconResource == 0) {
            defaultIconResource = R.drawable.ic_notifications_white_24dp;
        }

        boolean canVibrate = context.getPackageManager().checkPermission(android.Manifest.permission.VIBRATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        int defaults = Notification.DEFAULT_ALL;
        if (!canVibrate) defaults &= ~Notification.DEFAULT_VIBRATE;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(pendingIntentBuilder.buildForDefault())
                .setAutoCancel(true)
                .setContentTitle(alert.getTitle())
                .setContentText(alert.getText())
                .setSubText(alert.getSubText())
                .setContentInfo(alert.getInfo())
                .setTicker(alert.getTicker())
                .setPriority(alert.getPriority())
                .setColor(alert.getColor())
                .setSmallIcon(alert.hasSmallIcon() && alert.getSmallIcon() != 0 ? alert.getSmallIcon() : defaultIconResource)
                .setLargeIcon(alert.getLargeIcon())
                .setCategory(alert.getCategory())
                .setGroup(alert.getGroup())
                //.setGroupSummary(alert.getGroupSummary())
                .setSortKey(alert.getSortKey())
                .setOngoing(alert.getOngoing())
                ;
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
            builder.setLights(alert.getLightsColor(), alert.getLightsOn(), alert.getLightsOff());
            defaults &= ~Notification.DEFAULT_LIGHTS;
        }
        if (alert.hasLights()) {
            if (alert.getLights()) {
                defaults |= Notification.DEFAULT_LIGHTS;
            } else {
                defaults &= ~Notification.DEFAULT_LIGHTS;
            }
        }
        if (canVibrate) {
            if (alert.getVibratePattern() != null) {
                builder.setVibrate(alert.getVibratePattern());
                defaults &= ~Notification.DEFAULT_VIBRATE;
            }
            if (alert.hasVibrate()) {
                if (alert.getVibrate()) {
                    defaults |= Notification.DEFAULT_VIBRATE;
                } else {
                    defaults &= ~Notification.DEFAULT_VIBRATE;
                }
            }
        }
        if (alert.getSoundUri() != null) {
            builder.setSound(alert.getSoundUri());
            defaults &= ~Notification.DEFAULT_SOUND;
        }
        if (alert.hasSound()) {
            if (alert.getSound()) {
                defaults |= Notification.DEFAULT_SOUND;
            } else {
                defaults &= ~Notification.DEFAULT_SOUND;
            }
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
                        .bigLargeIcon(alertBigPicture.getBigLargeIcon() != null ? alertBigPicture.getBigLargeIcon() : alert.getLargeIcon())
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

    public static void ensureNotificationDismissed(Context context, Intent intent, NotificationModel notif) {
        // Manually dismiss the notification and close the system drawer, when an action button is clicked.
        // May be a kind of bug, or may be a feature when the associated PendingIntent resolves a Service instead of an Activity.
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
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
        if (containsExplicitNotification(intent) || containsWillOpenNotificationAutomaticallyOpenable(intent)) {
            final NotificationModel notif = NotificationModel.fromLocalIntent(intent);
            if (notif == null) {
                Log.e(TAG, "Failed to extract notification object");
                return false;
            }

            sLastHandledIntentRef = new WeakReference<>(intent);

            if (!WonderPushService.isProperlySetup()) {
                handleOpenedNotificationFromService(context, intent, notif);
            }

            InAppManager.handleInApp(context, notif);
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

    public static void handleOpenedNotificationFromService(Context context, Intent intent, NotificationModel notif) {
        ensureNotificationDismissed(context, intent, notif);

        boolean fromUserInteraction = intent.getBooleanExtra("fromUserInteraction", true);
        Intent receivedPushNotificationIntent = intent.getParcelableExtra("receivedPushNotificationIntent");
        String buttonIndexStr = intent.getData().getQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER_BUTTON_INDEX);
        int buttonIndex = -1;
        try {
            if (buttonIndexStr != null) {
                buttonIndex = Integer.parseInt(buttonIndexStr);
            }
        } catch (Exception ignored) { // NumberFormatException
            WonderPush.logError("Failed to parse buttonIndex " + buttonIndexStr, ignored);
        }

        WonderPush.logDebug("Handling opened notification: " + notif.getInputJSONString());
        try {
            JSONObject trackData = new JSONObject();
            trackData.put("campaignId", notif.getCampaignId());
            trackData.put("notificationId", notif.getNotificationId());
            trackData.put("actionDate", TimeSync.getTime());
            if (buttonIndex >= 0 && notif.getAlert() != null && notif.getAlert().getButtons() != null && buttonIndex < notif.getAlert().getButtons().size()) {
                NotificationButtonModel button = notif.getAlert().getButtons().get(buttonIndex);
                trackData.put("buttonLabel", button.label);
            }
            WonderPush.trackInternalEvent("@NOTIFICATION_OPENED", trackData);

            WonderPushConfiguration.setLastOpenedNotificationInfoJson(trackData);

            // Notify the application that the notification has been opened
            Intent notificationOpenedIntent = new Intent(WonderPush.INTENT_NOTIFICATION_OPENED);
            notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_FROM_USER_INTERACTION, fromUserInteraction);
            notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_RECEIVED_PUSH_NOTIFICATION, receivedPushNotificationIntent);
            notificationOpenedIntent.putExtra(WonderPush.INTENT_NOTIFICATION_OPENED_EXTRA_BUTTON_INDEX, buttonIndex);
            LocalBroadcastManager.getInstance(WonderPush.getApplicationContext()).sendBroadcast(notificationOpenedIntent);

            handleOpenedNotification(context, notif, buttonIndex);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse notification JSON object", e);
        }
    }

    private static void handleOpenedNotification(Context context, NotificationModel notif, int clickedButtonIndex) {
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
        handleNotificationActions(context, notif, actions);
    }

    protected static void handleNotificationActions(Context context, NotificationModel notif, List<ActionModel> actions) {
        if (actions == null)
            return;

        try {
            for (ActionModel action : actions) {
                handleAction(context, notif, action);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error while handling actions", ex);
        }
    }

    protected static void handleAction(Context context, NotificationModel notif, ActionModel action) {
        try {
            if (action == null || action.getType() == null) {
                // Skip unrecognized action types
                return;
            }
            switch (action.getType()) {
                case CLOSE:
                    // Noop
                    break;
                case MAP_OPEN:
                    handleMapOpenAction(context, notif, action);
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
        WonderPush.trackEvent(event.optString("type", null), event.optJSONObject("custom"));
    }

    protected static void handleUpdateInstallationAction(ActionModel action) {
        JSONObject custom = action.getCustom();
        if (custom == null) {
            Log.e(TAG, "Got no installation custom properties to update for a " + ActionModel.Type.UPDATE_INSTALLATION + " action");
            return;
        }
        if (custom.length() == 0) {
            WonderPush.logDebug("Empty installation custom properties for an update, for a " + ActionModel.Type.UPDATE_INSTALLATION + " action");
            return;
        }
        InstallationManager.putInstallationCustomProperties(custom);
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

    private static void handleMapOpenAction(Context context, NotificationModel notif, ActionModel action) {
        try {
            NotificationMapModel.Place place;
            try {
                place = ((NotificationMapModel) notif).getMap().getPlace();
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
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setData(geo.build());
            if (open.resolveActivity(context.getPackageManager()) != null) {
                WonderPush.logDebug("Will open location " + open.getDataString());
                context.startActivity(open);
            } else {
                WonderPush.logDebug("No activity can open location " + open.getDataString());
                WonderPush.logDebug("Falling back to regular URL");
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
                    WonderPush.logDebug("Opening URL " + open.getDataString());
                    context.startActivity(open);
                } else {
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

}
