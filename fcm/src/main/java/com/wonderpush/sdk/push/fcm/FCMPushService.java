package com.wonderpush.sdk.push.fcm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushSettings;
import com.wonderpush.sdk.push.PushServiceManager;
import com.wonderpush.sdk.push.PushServiceResult;
import com.wonderpush.sdk.push.fcm.BuildConfig;
import com.wonderpush.sdk.push.PushService;

public class FCMPushService implements PushService {

    private static final String TAG = "WonderPush.Push.FCM." + FCMPushService.class.getSimpleName();

    public static final String IDENTIFIER = "FCM"; // This key serves for ordering in case multiple push services are available
    static final String WONDERPUSH_DEFAULT_SENDER_ID = "1023997258979";
    static final String FIREBASE_APP_NAME = "WonderPushFirebaseApp";
    static Context sContext;
    private static FirebaseApp sFirebaseApp;
    private static String sSenderId;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(Context context) {
        sContext = context;
        if (WonderPush.getLogging()) Log.d(TAG, "Initializing FirebaseApp…");

        sSenderId = WonderPushSettings.getString("WONDERPUSH_SENDER_ID", "wonderpush_senderId", "com.wonderpush.sdk.senderId");
        if (sSenderId != null) {
            if (WonderPush.getLogging()) Log.d(TAG, "Applying configuration: senderId: " + sSenderId);
        } else {
            sSenderId = getDefaultSenderId();
            if (WONDERPUSH_DEFAULT_SENDER_ID.equals(sSenderId)) {
                Log.w(TAG, "Using WonderPush own Firebase FCM Sender ID " + sSenderId + ". Your push tokens will not be portable. Please refer to the documentation.");
            } else {
                if (WonderPush.getLogging()) Log.d(TAG, "Using senderId from Firebase: " + sSenderId);
            }
        }

        try {
            sFirebaseApp = FirebaseApp.initializeApp(
                    sContext,
                    new FirebaseOptions.Builder()
                            .setApplicationId("NONE")
                            .setApiKey("NONE")
                            .setGcmSenderId(sSenderId)
                            .build(),
                    FIREBASE_APP_NAME
            );
            if (WonderPush.getLogging()) Log.d(TAG, "Initialized FirebaseApp");
        } catch (IllegalStateException alreadyInitialized) {
            if (WonderPush.getLogging()) Log.d(TAG, "FirebaseApp already initialized", alreadyInitialized);
            sFirebaseApp = FirebaseApp.getInstance(FIREBASE_APP_NAME);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(sContext);
            if (resultCode == ConnectionResult.SUCCESS) {
                return true;
            }
            Log.w(TAG, "This device does not support Google Play Services: " + googleApiAvailability.getErrorString(resultCode));
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "The Google Play Services have not been added to the application", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while checking the Google Play Services", e);
        }
        return false;
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
            iconId = metaData.getInt("com.google.firebase.messaging.default_notification_icon");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while getting notification icon", e);
        }
        if (iconId == 0) {
            // Default to an embedded icon
            iconId = R.drawable.ic_notifications_white_24dp;
        }
        return iconId;
    }

    @Override
    public int getNotificationColor() {
        int color = 0;
        try {
            Bundle metaData = sContext.getPackageManager().getApplicationInfo(sContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
            int resId = metaData.getInt("com.google.firebase.messaging.default_notification_color");
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
        if (WonderPush.getLogging()) Log.d(TAG, "FirebaseInstanceId.getInstanceId() called…");
        FirebaseInstanceId.getInstance(getFirebaseApp()).getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Could not get Firebase InstanceId", task.getException());
                            return;
                        }
                        InstanceIdResult result = task.getResult();
                        if (result == null) {
                            if (WonderPush.getLogging()) Log.d(TAG, "FirebaseInstanceId.getInstanceId() = null");
                            return;
                        }
                        if (WonderPush.getLogging()) Log.d(TAG, "FirebaseInstanceId.getInstanceId() = " + result.getToken());
                        storeRegistrationId(sContext, getSenderId(), result.getToken());
                    }
                });
    }

    static void storeRegistrationId(Context context, String senderIds, String registrationId) {
        PushServiceResult result = new PushServiceResult();
        result.setService("FCM");
        result.setData(registrationId);
        result.setSenderIds(senderIds);
        PushServiceManager.onResult(result);
    }

    static String getDefaultSenderId() {
        int firebaseSenderIdRes = sContext.getResources().getIdentifier("gcm_defaultSenderId", "string", sContext.getPackageName());
        if (firebaseSenderIdRes == 0) {
            return WONDERPUSH_DEFAULT_SENDER_ID;
        } else {
            return sContext.getResources().getString(firebaseSenderIdRes);
        }
    }

    static String getSenderId() {
        return sSenderId;
    }

    static FirebaseApp getFirebaseApp() {
        return sFirebaseApp;
    }

}
