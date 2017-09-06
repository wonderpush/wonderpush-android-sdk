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
            WonderPush.ensureInitialized(this);
            // Fetch updated Instance ID token and notify our app's server of any changes.
            WonderPushRegistrationJobIntentService.enqueueWork(this, new Intent());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while handling InstanceID token refresh", e);
        }
    }

}
