package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Log;

// We don't use an IntentService because of
// https://github.com/loopj/android-async-http/pull/492 and
// https://github.com/loopj/android-async-http/pull/496
// We could also update android-async-http >= 1.4.5.
public class WonderPushService extends Service {

    private static Boolean sIsProperlySetup;

    protected static boolean isProperlySetup() {
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
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (WonderPush.containsNotification(intent)) {
            Activity activity = WonderPush.getCurrentActivity();
            if (activity != null && !activity.isFinishing()) {
                WonderPush.showPotentialNotification(WonderPush.getCurrentActivity(), intent);
            } else {
                Intent activityIntent = new Intent();
                activityIntent.setClassName(getApplicationContext(), intent.getExtras().getString("activity"));
                activityIntent.fillIn(intent, 0);
                activityIntent.setFlags(activityIntent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityIntent);
            }
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }

}
