package com.wonderpush.sdk.push.hcm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushSettings;
import com.wonderpush.sdk.push.PushService;
import com.wonderpush.sdk.push.PushServiceManager;
import com.wonderpush.sdk.push.PushServiceResult;
import com.wonderpush.sdk.push.hcm.BuildConfig;

public class HCMPushService implements PushService {

    private static final String TAG = "WonderPush.Push.HCM." + HCMPushService.class.getSimpleName();

    public static final String IDENTIFIER = "HCM"; // This key serves for ordering in case multiple push services are available
    static Context sContext;
    private static String sHCMAppId;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getName() {
        return "Huawei Push Kit";
    }

    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(Context context) {
        sContext = context;

        sHCMAppId = WonderPushSettings.getString("WONDERPUSH_HCM_APP_ID", "wonderpush_hcmAppId", "com.wonderpush.sdk.hcmAppId");
        if (sHCMAppId != null) {
            if (WonderPush.getLogging()) Log.d(TAG, "Applying configuration: HCM AppId: " + sHCMAppId);
        } else {
            sHCMAppId = HCMPushService.getDefaultAppId();
            if (sHCMAppId == null) {
                Log.w(TAG, "No HCM App ID " + sHCMAppId + ". Check your HMS integration. Please refer to the documentation.");
            } else {
                if (WonderPush.getLogging()) Log.d(TAG, "Using App Id from HMS: " + sHCMAppId);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            if (HuaweiApiAvailability.getInstance().isHuaweiMobileNoticeAvailable(sContext) != 0) {
                Log.w(TAG, "HMS is not available");
                return false;
            }
            return sHCMAppId != null;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to check for HMS availability", ex);
            return false;
        }
    }

    @Override
    public void execute() {
        fetchInstanceId();
    }

    @Override
    public int getNotificationIcon() {
        int iconId = 0;
        try {
            Bundle metaData = sContext.getPackageManager().getApplicationInfo(sContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
            iconId = metaData.getInt("com.huawei.messaging.default_notification_icon");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while getting notification icon", e);
        }
        if (iconId == 0) {
            // Default to an embedded icon
            iconId = com.wonderpush.sdk.R.drawable.ic_notifications_white_24dp;
        }
        return iconId;
    }

    @Override
    public int getNotificationColor() {
        int color = 0;
        try {
            Bundle metaData = sContext.getPackageManager().getApplicationInfo(sContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
            int resId = metaData.getInt("com.huawei.messaging.default_notification_color");
            if (resId != 0) {
                color = contextCompatGetColor(sContext, resId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while getting notification color", e);
        }
        return color;
    }

    @SuppressWarnings("deprecation")
    public static int contextCompatGetColor(Context context, int id) {
        return Build.VERSION.SDK_INT >= 23 ? context.getColor(id) : context.getResources().getColor(id);
    }

    public static void fetchInstanceId() {
        final String appId = getHCMAppId();
        if (WonderPush.getLogging()) Log.d(TAG, "HmsInstanceId.getToken(\"" + appId + "\", \"HCM\") will be called in the background…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (WonderPush.getLogging()) Log.d(TAG, "HmsInstanceId.getToken(\"" + appId + "\", \"HCM\") called…");
                    String pushToken = HmsInstanceId.getInstance(sContext).getToken(appId, "HCM");
                    if (TextUtils.isEmpty(pushToken)) {
                        if (WonderPush.getLogging()) Log.d(TAG, "HmsInstanceId.getToken() = null");
                    } else {
                        if (WonderPush.getLogging()) Log.d(TAG, "HmsInstanceId.getToken() = " + pushToken);
                        storeRegistrationId(sContext, getHCMAppId(), pushToken);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Could not get HMS InstanceId", ex);
                }
            }
        }, "WonderPush.Push.HCM.HuaweiMessagingService.fetchInstanceId").start();
    }

    static void storeRegistrationId(Context context, String senderIds, String registrationId) {
        PushServiceResult result = new PushServiceResult();
        result.setService("HCM");
        result.setData(registrationId);
        result.setSenderIds(senderIds);
        PushServiceManager.onResult(result);
    }

    static String getDefaultAppId() {
        String appId = AGConnectServicesConfig.fromContext(sContext).getString("client/app_id");
        if (TextUtils.isEmpty(appId)) {
            return null;
        }
        return appId;
    }

    static String getHCMAppId() {
        return sHCMAppId;
    }

}
