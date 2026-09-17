package com.wonderpush.sdk;

import java.util.Map;

/**
 * Performs the actual explicit-fetch GET. {@code success} reflects the HTTP outcome (the response
 * body is consumed by the incoming interceptor separately). Completion may run on any thread.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncFetchTransport}.
 */
interface SyncFetchTransport {

    interface Completion {
        void onComplete(boolean success);
    }

    void fetch(String source, String userId, String path, Map<String, Object> params, Completion completion);
}
