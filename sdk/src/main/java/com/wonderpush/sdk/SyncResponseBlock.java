package com.wonderpush.sdk;

import org.json.JSONObject;

/**
 * A single source's response block. Same shape on the opportunistic path (nested under
 * {@code _<source>Sync.…}) and the explicit path (at the response root).
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncResponseBlock} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-processor.ts} (the ResponseBlock interface).
 * Representation: sdk-sync/docs/docs/sync/algorithm.md:169-179.
 *
 * Presence matters to the processor (a missing field differs from a field set to null): the
 * accessors distinguish "absent" ({@link #hasKey}) from an explicit JSON null
 * ({@link JSONObject#NULL}). Numeric fields are exposed as nullable {@link Number}; the union fields
 * (versionId, data, delta, knownVersionId) are exposed as {@code Object} with a companion {@code hasX} flag.
 */
class SyncResponseBlock {

    private static final String[] RECOGNIZED = {
            "meta", "version", "versionId", "readDate", "data", "delta",
            "knownVersion", "knownVersionId", "knownReadDate",
    };

    private final JSONObject raw;

    private SyncResponseBlock(JSONObject raw) {
        this.raw = raw;
    }

    /**
     * null when the block itself is absent. An empty {@code {}} block yields a non-null instance
     * whose {@link #isEmpty} is true.
     */
    static SyncResponseBlock fromJSON(JSONObject dict) {
        if (dict == null) return null;
        return new SyncResponseBlock(dict);
    }

    /** A recognized field is present when its key exists, regardless of value (including null). */
    private boolean hasKey(String key) {
        return raw.has(key);
    }

    private Number numberForKey(String key) {
        Object value = raw.opt(key);
        return value instanceof Number ? (Number) value : null;
    }

    /**
     * True when the block carries none of the recognized fields (the empty {@code {}}
     * "try asking explicitly" signal on the opportunistic path).
     */
    boolean isEmpty() {
        for (String key : RECOGNIZED) {
            if (hasKey(key)) return false;
        }
        return true;
    }

    /** Opaque metadata blob (stored + echoed only). null when absent or not an object. */
    JSONObject meta() {
        Object value = raw.opt("meta");
        return value instanceof JSONObject ? (JSONObject) value : null;
    }

    Number version() { return numberForKey("version"); }
    Number readDate() { return numberForKey("readDate"); }
    Number knownVersion() { return numberForKey("knownVersion"); }
    Number knownReadDate() { return numberForKey("knownReadDate"); }

    boolean hasVersionId() { return hasKey("versionId"); }
    Object versionId() { return raw.opt("versionId"); }        // Number | String | JSONObject.NULL

    boolean hasData() { return hasKey("data"); }
    Object data() { return raw.opt("data"); }                  // full object or full list (or NULL)

    boolean hasDelta() { return hasKey("delta"); }
    Object delta() { return raw.opt("delta"); }                // partial patch or array of items

    boolean hasKnownVersionId() { return hasKey("knownVersionId"); }
    Object knownVersionId() { return raw.opt("knownVersionId"); }

    @Override
    public String toString() {
        return "SyncResponseBlock" + raw;
    }
}
