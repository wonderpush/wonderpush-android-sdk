package com.wonderpush.sdk;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

public class WonderPushInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = WonderPush.TAG;

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        try {
            // Fetch updated Instance ID token and notify our app's server of any changes.
            Intent intent = new Intent(this, WonderPushRegistrationIntentService.class);
            startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling InstanceID token refresh", e);
        }
    }

}
