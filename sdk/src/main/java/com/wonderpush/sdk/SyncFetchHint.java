package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The {@code known*} head-hint subset echoed back to the server on the explicit fetch request.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncFetchHint} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-processor.ts} (FetchHint).
 */
class SyncFetchHint {

    Number knownVersion;
    Object knownVersionId;   // Number | String | null
    Number knownReadDate;

    /** Minimal dictionary, omitting unset fields. */
    JSONObject toJSON() {
        JSONObject dict = new JSONObject();
        try {
            if (knownVersion != null) dict.put("knownVersion", knownVersion);
            if (knownVersionId != null) dict.put("knownVersionId", knownVersionId);
            if (knownReadDate != null) dict.put("knownReadDate", knownReadDate);
        } catch (JSONException e) {
            // Constant keys, JSON-safe values; cannot happen.
        }
        return dict;
    }

    @Override
    public String toString() {
        return "SyncFetchHint" + toJSON();
    }
}
