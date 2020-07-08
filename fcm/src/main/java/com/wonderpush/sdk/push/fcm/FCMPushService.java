package com.wonderpush.sdk.push.fcm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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
    static final String FIREBASE_DEFAULT_PROJECT_ID = "wonderpush-shared-project";
    static final String FIREBASE_DEFAULT_APPLICATION_ID = "1:416361470460:android:fc011131a2bdecf97eba79";
    static final String FIREBASE_DEFAULT_API_KEY = "AIzaSyBzwZ5fRJbAohI154TVG1ouVIKkK83oOOU";
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
    public String getName() {
        return "Firebase Cloud Messaging";
    }

    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(Context context) {
        if (sContext != null) {
            Log.w(TAG, "Skipping second initialization");
            return;
        }
        sContext = context;
        if (WonderPush.getLogging()) Log.d(TAG, "Initializing FirebaseApp…");

        sSenderId = WonderPushSettings.getString("WONDERPUSH_SENDER_ID", "wonderpush_senderId", "com.wonderpush.sdk.senderId");
        boolean senderIdExplicitlySet = !TextUtils.isEmpty(sSenderId);
        if (!TextUtils.isEmpty(sSenderId)) {
            if (WonderPush.getLogging()) Log.d(TAG, "Applying configuration: senderId: " + sSenderId);
        } else {
            sSenderId = getDefaultSenderId();
            if (WONDERPUSH_DEFAULT_SENDER_ID.equals(sSenderId)) {
                Log.w(TAG, "Using WonderPush own Firebase FCM Sender ID " + sSenderId + ". Your push tokens will not be portable. Please refer to the documentation.");
            } else {
                if (WonderPush.getLogging()) Log.d(TAG, "Using senderId from Firebase: " + sSenderId);
            }
        }

        // Note about Firebase initialization:
        //     Since Cloud Messaging version 20.1.1 Firebase uses the Firebase Installations SDK,
        //     which requires a valid Application ID, Project ID and API key.
        //     See https://firebase.google.com/support/privacy/init-options.
        //     We can use this triplet independently of the Sender ID.
        //     We can also reuse this triplet independently of the actual Android application.
        //     We use this capacity to streamline the compatibility with these newer FCM library versions.

        String firebaseApplicationId = WonderPushSettings.getString("WONDERPUSH_FIREBASE_APPLICATION_ID", "wonderpush_firebase_applicationId", "com.wonderpush.sdk.firebaseApplicationId");
        String firebaseProjectId = WonderPushSettings.getString("WONDERPUSH_FIREBASE_PROJECT_ID", "wonderpush_firebase_projectId", "com.wonderpush.sdk.firebaseProjectId");
        String firebaseApiKey = WonderPushSettings.getString("WONDERPUSH_FIREBASE_API_KEY", "wonderpush_firebase_apiKey", "com.wonderpush.sdk.firebaseApiKey");
        if (!TextUtils.isEmpty(firebaseApplicationId) || !TextUtils.isEmpty(firebaseProjectId) || !TextUtils.isEmpty(firebaseApiKey)) {
            if (TextUtils.isEmpty(firebaseApplicationId) || TextUtils.isEmpty(firebaseProjectId) || TextUtils.isEmpty(firebaseApiKey)) {
                Log.e(TAG, "Some but not all FirebaseApp credentials were given. Ignoring them.");
                if (TextUtils.isEmpty(firebaseApplicationId)) {
                    Log.e(TAG, "Missing Application ID");
                }
                if (TextUtils.isEmpty(firebaseProjectId)) {
                    Log.e(TAG, "Missing Project ID");
                }
                if (TextUtils.isEmpty(firebaseApiKey)) {
                    Log.e(TAG, "Missing API key");
                }
                firebaseApplicationId = null;
                firebaseProjectId = null;
                firebaseApiKey = null;
            }
        }

        // Use the provided credentials, if available
        if (!TextUtils.isEmpty(firebaseApplicationId) && !TextUtils.isEmpty(firebaseProjectId) && !TextUtils.isEmpty(firebaseApiKey)) {
            try {
                if (WonderPush.getLogging()) Log.d(TAG, "Initializing FirebaseApp programmatically using applicationId=" + firebaseApplicationId + " projectId=" + firebaseProjectId + " senderId=" + sSenderId);
                sFirebaseApp = FirebaseApp.initializeApp(
                        sContext,
                        new FirebaseOptions.Builder()
                                .setApplicationId(firebaseApplicationId)
                                .setProjectId(firebaseProjectId)
                                .setApiKey(firebaseApiKey)
                                .setGcmSenderId(sSenderId)
                                .build(),
                        FIREBASE_APP_NAME + "-1"
                );
                if (WonderPush.getLogging()) Log.d(TAG, "Initialized FirebaseApp");
            } catch (IllegalStateException ex) {
                Log.e(TAG, "Failed to initialize FirebaseApp programmatically using given credentials", ex);
            }
        }
        // If ready, trigger further verification
        if (sFirebaseApp != null) {
            try {
                if (WonderPush.getLogging()) Log.d(TAG, "Checking FirebaseApp initialization");
                FirebaseInstanceId.getInstance(sFirebaseApp);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Failed to check FirebaseApp initialization", ex);
                // We'll try to get the default instance
                //sFirebaseApp.delete(); // do not delete or we'll get a FATAL EXCEPTION from an FCM background thread
                sFirebaseApp = null;
            }
        }
        // If still not ready, use shared credentials
        if (sFirebaseApp == null) {
            if (WonderPush.getLogging()) Log.d(TAG, "Using the default FirebaseApp");
            try {
                sFirebaseApp = FirebaseApp.getInstance();
            } catch (IllegalStateException defaultFirebaseAppDoesNotExistEx) {
                if (WonderPush.getLogging()) Log.d(TAG, "No default FirebaseApp");
            }
            if (sFirebaseApp != null && senderIdExplicitlySet && !sSenderId.equals(sFirebaseApp.getOptions().getGcmSenderId())) {
                // Instead of using another Sender ID than explicitly configured, reuse the options of the default FirebaseApp with the desired SenderID
                if (WonderPush.getLogging()) Log.d(TAG, "The default FirebaseApp uses Sender ID " + sFirebaseApp.getOptions().getGcmSenderId() + " whereas WonderPush SDK was instructed to use " + sSenderId + ".");
                if (WonderPush.getLogging()) Log.d(TAG, "We will reconstruct a FirebaseApp with the same options except for the Sender ID");
                FirebaseOptions options = sFirebaseApp.getOptions();
                try {
                    sFirebaseApp = FirebaseApp.initializeApp(
                            sContext,
                            new FirebaseOptions.Builder()
                                    .setApplicationId(options.getApplicationId())
                                    .setProjectId(options.getProjectId())
                                    .setApiKey(options.getApiKey())
                                    .setGcmSenderId(sSenderId)
                                    .build(),
                            FIREBASE_APP_NAME + "-2" // we must use another app name if the first attempt failed validation because we can't delete that FirebaseApp
                    );
                    if (WonderPush.getLogging()) Log.d(TAG, "Initialized FirebaseApp");
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "Failed to reconfigure default FirebaseApp with another Sender ID", ex);
                    Log.e(TAG, "WonderPush might not be able to send push notifications. Check your application configuration, then the FCM Server Key in your dashboard.");
                }
            }
        }
        // If still not ready, use shared credentials
        if (sFirebaseApp == null) {
            if (WonderPush.getLogging()) Log.d(TAG, "Using shared FirebaseApp credentials");
            try {
                if (WonderPush.getLogging()) Log.d(TAG, "Initializing FirebaseApp programmatically using shared credentials");
                sFirebaseApp = FirebaseApp.initializeApp(
                        sContext,
                        new FirebaseOptions.Builder()
                                .setApplicationId(FIREBASE_DEFAULT_APPLICATION_ID)
                                .setProjectId(FIREBASE_DEFAULT_PROJECT_ID)
                                .setApiKey(FIREBASE_DEFAULT_API_KEY)
                                .setGcmSenderId(sSenderId)
                                .build(),
                        FIREBASE_APP_NAME + "-3"
                );
                if (WonderPush.getLogging()) Log.d(TAG, "Initialized FirebaseApp");
            } catch (IllegalStateException ex) {
                Log.e(TAG, "Failed to initialize FirebaseApp with shared credentials", ex);
                Log.e(TAG, "Push notifications will not work");
            }
        }

        // Make sure we get the right Sender ID
        if (sFirebaseApp != null) {
            sSenderId = sFirebaseApp.getOptions().getGcmSenderId();
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
        if (getFirebaseApp() == null) {
            Log.w(TAG, "FirebaseInstanceId.getInstanceId() cannot proceed, FirebaseApp was not initialized.");
            return;
        }
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
        String rtn = null;
        int firebaseSenderIdRes = sContext.getResources().getIdentifier("gcm_defaultSenderId", "string", sContext.getPackageName());
        if (firebaseSenderIdRes != 0) {
            rtn = sContext.getResources().getString(firebaseSenderIdRes);
        }
        if (TextUtils.isEmpty(rtn)) {
            rtn = WONDERPUSH_DEFAULT_SENDER_ID;
        }
        return rtn;
    }

    static String getSenderId() {
        return sSenderId;
    }

    static FirebaseApp getFirebaseApp() {
        return sFirebaseApp;
    }

}
