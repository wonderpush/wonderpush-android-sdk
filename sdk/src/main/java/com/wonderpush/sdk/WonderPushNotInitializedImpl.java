package com.wonderpush.sdk;

import android.util.Log;

/**
 * Implementation of {@link IWonderPush} that does nothing but log an error and return a default value.
 *
 * This implementation is used when user consent is required and not provided.
 */
class WonderPushNotInitializedImpl extends WonderPushLogErrorImpl {

    static class WonderPushNotInitializedException extends RuntimeException {
        WonderPushNotInitializedException(String message) {
            super(message);
        }
    }

    @Override
    protected void log(String method) {
        String msg = "Cannot call WonderPush." + method + " before initialization. Please call WonderPush.initialize() first.";
        Log.e(WonderPush.TAG, msg, new WonderPushNotInitializedException(msg));
    }

}
