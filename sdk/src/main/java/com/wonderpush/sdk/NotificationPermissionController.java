package com.wonderpush.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashSet;
import java.util.Set;


public class NotificationPermissionController implements PermissionsActivity.PermissionCallback {
    private static final String PERMISSION_TYPE = "NOTIFICATION";
    private static final String ANDROID_PERMISSION_STRING = "android.permission.POST_NOTIFICATIONS";
    public interface PromptForPushNotificationPermissionResponseHandler {
        void response(boolean accepted);
    }

    private static NotificationPermissionController sInstance = new NotificationPermissionController();

    private NotificationPermissionController() {
        PermissionsActivity.registerAsCallback(PERMISSION_TYPE, this);
    }

    private Set<PromptForPushNotificationPermissionResponseHandler> callbacks = new HashSet<>();
    private boolean awaitingForReturnFromSystemSettings = false;

    /**
     * Returns true when running on Android 13+ with targetSdkVersion >= 33
     * @return
     */
    protected static boolean supportsNativePrompt() {
        if (Build.VERSION.SDK_INT <= 32) return false;

        Context context = WonderPush.getApplicationContext();
        if (context == null) return false;

        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return applicationInfo.targetSdkVersion > 32;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(WonderPush.TAG, "Could not determine targetSdkVersion", e);
        }
        return false;
    }

    private boolean notificationsEnabled() {
        Context context = WonderPush.getApplicationContext();
        if (context == null) return false;
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public void prompt(boolean fallbackToSettings, @Nullable PromptForPushNotificationPermissionResponseHandler callback) {
        if (callback != null) callbacks.add(callback);
        boolean areNotificationsEnabled = notificationsEnabled();
        if (areNotificationsEnabled) {
            fireCallBacks(true);
            return;
        }
        if (!supportsNativePrompt()) {
            if (fallbackToSettings) {
                showFallbackAlertDialog();
            } else {
                fireCallBacks(false);
            }
            return;
        }
        PermissionsActivity.startPrompt(
                fallbackToSettings,
                PERMISSION_TYPE,
                ANDROID_PERMISSION_STRING,
                getClass()
        );
    }

    // Fires callbacks and clears them to ensure each is only called once.
    private void fireCallBacks(boolean accepted) {
        for (PromptForPushNotificationPermissionResponseHandler callback : callbacks) {
            callback.response(accepted);
        }
        callbacks.clear();
    }

    public static NotificationPermissionController getInstance() {
        return sInstance;
    }

    @Override
    public void onAccept() {
        WonderPush.refreshSubscriptionStatus();
        fireCallBacks(true);
    }

    @Override
    public void onReject(boolean fallbackToSettings) {
        WonderPush.refreshSubscriptionStatus();
        boolean fallbackShown = fallbackToSettings && showFallbackAlertDialog();
        if (!fallbackShown) {
            fireCallBacks(false);
        }
    }

    private boolean showFallbackAlertDialog() {
        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
        if (activity == null) return false;
        String titleTemplate = activity.getString(R.string.wonderpush_android_sdk_permission_not_available_title);
        String title = String.format(titleTemplate, activity.getString(R.string.wonderpush_android_sdk_notification_permission_name_for_title));

        String messageTemplate = activity.getString(R.string.wonderpush_android_sdk_permission_not_available_message);
        String message = String.format(messageTemplate, activity.getString(R.string.wonderpush_android_sdk_notification_permission_settings_message));
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.wonderpush_android_sdk_permission_not_available_open_settings_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        //for Android 5-7
                        intent.putExtra("app_package", activity.getPackageName());
                        intent.putExtra("app_uid", activity.getApplicationInfo().uid);

                        // for Android 8 and above
                        intent.putExtra("android.provider.extra.APP_PACKAGE", activity.getPackageName());
                        activity.startActivity(intent);
                        awaitingForReturnFromSystemSettings = true;
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
        return true;
    }

    public void onAppForegrounded() {
        if (!awaitingForReturnFromSystemSettings) return;
        awaitingForReturnFromSystemSettings = false;
        fireCallBacks(notificationsEnabled());
    }
}
