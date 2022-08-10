package com.wonderpush.sdk;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.HashMap;

public class PromptActivity extends Activity {

    interface Callback {
        void onAccept();
        void onReject(boolean fallbackToSettings);
    }

    private static final int DELAY_TIME_CALLBACK_CALL = 500;
    private static final int WONDERPUSH_PERMISSION_REQUEST_CODE = 2;

    private static boolean waiting, fallbackToSettings, askNoMore;

    private static final String INTENT_EXTRA_PERMISSION_TYPE = "INTENT_EXTRA_PERMISSION_TYPE";
    private static final String INTENT_EXTRA_ANDROID_PERMISSION_STRING = "INTENT_EXTRA_ANDROID_PERMISSION_STRING";
    private static final String INTENT_EXTRA_CALLBACK_CLASS = "INTENT_EXTRA_CALLBACK_CLASS";

    private String permissionRequestType;

    private String androidPermissionString;

    private static final HashMap<String, Callback> callbackMap = new HashMap<>();

    public static void registerCallback(
            @NonNull String permissionType,
            @NonNull Callback callback
    ) {
        callbackMap.put(permissionType, callback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleExtras(getIntent().getExtras());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleExtras(intent.getExtras());
    }

    private void handleExtras(Bundle extras) {
        if (Build.VERSION.SDK_INT < 23) {
            finish();
            overridePendingTransition(R.anim.wonderpush_fade_in, R.anim.wonderpush_fade_out);
            return;
        }

        registerCallbacks(extras);

        permissionRequestType = extras.getString(INTENT_EXTRA_PERMISSION_TYPE);
        androidPermissionString = extras.getString(INTENT_EXTRA_ANDROID_PERMISSION_STRING);
        requestPermission(androidPermissionString);
    }

    // Required if the app was killed while this prompt was showing
    private void registerCallbacks(Bundle extras) {
        String className = extras.getString(INTENT_EXTRA_CALLBACK_CLASS);
        try {
            // Loads class into memory so it's static initialization block runs
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find callback class: " + className);
        }
    }

    private void requestPermission(String androidPermissionString) {
        if (!waiting) {
            waiting = true;
            askNoMore = !ActivityCompat.shouldShowRequestPermissionRationale(PromptActivity.this, androidPermissionString);
            ActivityCompat.requestPermissions(this, new String[]{androidPermissionString}, WONDERPUSH_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull final int[] grantResults) {
        waiting = false;

        if (requestCode == WONDERPUSH_PERMISSION_REQUEST_CODE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    Callback callback = callbackMap.get(permissionRequestType);
                    if (callback == null)
                        throw new RuntimeException("Missing handler for request type: " + permissionRequestType);

                    if (granted)
                        callback.onAccept();
                    else {
                        callback.onReject(showSettings());
                    }
                }
            }, DELAY_TIME_CALLBACK_CALL);
        }

        finish();
        overridePendingTransition(R.anim.wonderpush_fade_in, R.anim.wonderpush_fade_out);
    }

    private boolean showSettings() {
        return fallbackToSettings
                && askNoMore
                && !ActivityCompat.shouldShowRequestPermissionRationale(PromptActivity.this, androidPermissionString);
    }

    static void prompt(
            boolean fallbackCondition,
            String permissionRequestType,
            String androidPermissionString,
            Class<?> callbackClass
    ) {
        if (PromptActivity.waiting)
            return;

        fallbackToSettings = fallbackCondition;
        Activity activity = ActivityLifecycleMonitor.getCurrentActivity();
        if (activity != null && !activity.getClass().equals(PromptActivity.class)) {
            Intent intent = new Intent(activity, PromptActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra(INTENT_EXTRA_PERMISSION_TYPE, permissionRequestType)
                    .putExtra(INTENT_EXTRA_ANDROID_PERMISSION_STRING, androidPermissionString)
                    .putExtra(INTENT_EXTRA_CALLBACK_CLASS, callbackClass.getName());
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.wonderpush_fade_in, R.anim.wonderpush_fade_out);
        } else {
            Log.e(WonderPush.TAG, "Could not start permission activity");
        }
    }
}