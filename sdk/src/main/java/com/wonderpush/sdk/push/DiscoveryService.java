package com.wonderpush.sdk.push;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushInitializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** Container for push service definition in <meta-data/>. */
public class DiscoveryService extends Service {

    private static final String TAG = "WonderPush.Push." + DiscoveryService.class.getSimpleName();
    private static final String METADATA_NAME_PREFIX = "com.wonderpush.sdk.push:";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static Collection<PushService> instantiatePushServices(Context context) {
        Bundle metaData = null;
        try {
            // Note: Using the class' name programmatically requires the matching ProGuard rule
            ComponentName myService = new ComponentName(context, DiscoveryService.class);
            metaData = context.getPackageManager().getServiceInfo(myService, PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to read " + DiscoveryService.class.getCanonicalName() + " meta-data", e);
        }
        if (metaData == null) {
            Log.w(TAG, "Found no meta-data for " + DiscoveryService.class.getCanonicalName());
            return Collections.emptyList();
        }

        ArrayList<PushService> rtn = new ArrayList<>();
        for (String key : metaData.keySet()) {
            //             <meta-data
            //                android:name="com.wonderpush.sdk.push:com.wonderpush.sdk.push.fcm.FCMPushService"
            //                android:value="com.wonderpush.sdk.push.PushService" />
            if (!key.startsWith(METADATA_NAME_PREFIX)) continue;
            String value = metaData.getString(key);
            // Note: Using the class' name programmatically requires the matching ProGuard rule
            if (!value.equals(PushService.class.getCanonicalName())) continue;
            String clazzName = key.substring(METADATA_NAME_PREFIX.length());
            if (WonderPush.getLogging()) Log.d(TAG, "Loading push service using class " + clazzName);
            try {
                // Note: Using the class' name programmatically requires the matching ProGuard rule
                Class<? extends PushService> initializerClass = Class.forName(clazzName).asSubclass(PushService.class);
                PushService pushService = initializerClass.newInstance();
                rtn.add(pushService);
            } catch (ClassNotFoundException ex) {
                Log.e(TAG, "Could not load PushService class " + clazzName, ex);
            } catch (IllegalAccessException ex) {
                Log.e(TAG, "Could not load PushService class " + clazzName, ex);
            } catch (InstantiationException ex) {
                Log.e(TAG, "Could not load PushService class " + clazzName, ex);
            }
        }

        return rtn;
    }

}
