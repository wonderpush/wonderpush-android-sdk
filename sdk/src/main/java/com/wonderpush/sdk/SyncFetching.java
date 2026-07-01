package com.wonderpush.sdk;

import org.json.JSONObject;

/**
 * The fetch-triggering surface the orchestrator depends on (lets it be tested with a fake fetcher).
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncFetching}.
 */
interface SyncFetching {

    interface Completion {
        /** @param attempted whether the fetch was actually attempted (false = skipped by a guard/mutex). */
        void onComplete(boolean attempted);
    }

    void fetchSource(String source, String userId, String deviceId, JSONObject identifiers,
                     SyncKnobs knobs, boolean weak, SyncFetchHint hint, Completion completion);
}
