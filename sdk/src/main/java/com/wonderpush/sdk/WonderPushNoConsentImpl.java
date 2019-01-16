package com.wonderpush.sdk;

import android.util.Log;

/**
 * Implementation of {@link IWonderPush} that does nothing but log an error and return a default value.
 *
 * This implementation is used when user consent is required and not provided.
 */
class WonderPushNoConsentImpl extends WonderPushLogErrorImpl {

    static class WonderPushRequiresConsentException extends RuntimeException {
        WonderPushRequiresConsentException(String message) {
            super(message);
        }
    }

    @Override
    protected void log(String method) {
        String msg = "Cannot call WonderPush." + method + " without user consent. Consider calling WonderPush.setUserConsent(true) after prompting the user.";
        Log.e(WonderPush.TAG, msg, new WonderPushRequiresConsentException(msg));
    }

}
