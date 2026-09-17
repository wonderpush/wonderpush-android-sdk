package com.wonderpush.sdk;

import java.util.Map;

/**
 * Issues the actual explicit-fetch HTTP GET on behalf of the sync fetcher. Supplied at SDK init
 * (wraps {@link ApiClient}), so the sync stack stays decoupled from the network layer.
 * Ported from wonderpush-ios-sdk {@code WPSyncAPIRequestSender}.
 */
interface SyncApiRequestSender {

    interface Completion {
        /** @param success a completed HTTP call with no error (the body is applied by the interceptor). */
        void onComplete(boolean success);
    }

    void send(String userId, String path, Map<String, Object> params, Completion completion);
}
