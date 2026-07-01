package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Single-object store transforms for the {@code contact} sync source.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncContactStore}. Reuses {@link JSONUtil#merge} (with
 * null-field-removes) and {@link JSONUtil#deepCopy} so the stored contact never aliases the response
 * payload. Covered by the contact-store conformance vectors.
 */
class SyncContactStore {

    /** Full reset: replace the entire object (old fields dropped). Non-object data clears to null. */
    static JSONObject applyContactData(JSONObject current, Object data) {
        if (!(data instanceof JSONObject)) return null;
        try {
            return JSONUtil.deepCopy((JSONObject) data);
        } catch (JSONException e) {
            return null;
        }
    }

    /** Patch: deep-merge the delta into the current object; null fields in the delta remove keys. */
    static JSONObject applyContactDelta(JSONObject current, Object delta) {
        try {
            JSONObject base = current != null ? JSONUtil.deepCopy(current) : new JSONObject();
            if (delta instanceof JSONObject) {
                JSONUtil.merge(base, (JSONObject) delta, true);
            }
            return base;
        } catch (JSONException e) {
            return current;
        }
    }

    /** Empty-reset: wipe to null. */
    static JSONObject clearContact() {
        return null;
    }

    private SyncContactStore() {}
}
