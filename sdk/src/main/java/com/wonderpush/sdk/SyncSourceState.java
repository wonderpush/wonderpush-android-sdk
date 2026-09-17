package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Per-source persistent state for the sdk-sync channel.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncSourceState} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-state.ts}.
 * Field semantics: sdk-sync/docs/docs/sync/algorithm.md:30-33 (stored state) and :87-91
 * (the five fields echoed to the server on every opportunistic call).
 *
 * {@code lastSyncMeta} is OPAQUE (algorithm.md:339): we store and echo it, never inspect it.
 */
class SyncSourceState {

    /** Last _serverTime at which the SDK was given affirmative information. */
    long lastSyncDate;
    /** Opaque blob handed back to the server on every call (null == JS null). */
    JSONObject lastSyncMeta;
    /** Update date of the most recently updated object (its version). */
    long lastVersion;
    /** Id of the most recently updated object: Number | String | null (null-missing). */
    Object lastVersionId;
    /** Last date the information was read from the underlying storage. */
    long lastReadDate;
    /** Local: last time an explicit fetch was attempted. */
    long lastFetchAttemptedDate;
    /** Local: consecutive failures, drives exponential backoff. */
    int lastFetchUnsuccessfulAttemptCount;
    /** Source-owned payload: JSONObject (single-object), JSONArray (multi-object), or null. */
    Object data;

    /** The empty state: all zeros / nulls, matching emptyState() in sync-state.ts. */
    static SyncSourceState emptyState() {
        return new SyncSourceState();
    }

    private static Object valueOrNull(JSONObject dict, String key) {
        Object v = dict.opt(key);
        return (v == null || v == JSONObject.NULL) ? null : v;
    }

    private static long readLong(JSONObject dict, String key) {
        Object v = valueOrNull(dict, key);
        return v instanceof Number ? ((Number) v).longValue() : 0;
    }

    /**
     * Round-trips with the JSON shape persisted on disk and used by the conformance vectors.
     * Missing keys and JSON null map to the empty-state default for that field.
     */
    static SyncSourceState fromJSON(JSONObject dict) {
        SyncSourceState state = new SyncSourceState();
        if (dict == null) return state;
        state.lastSyncDate = readLong(dict, "lastSyncDate");
        Object meta = valueOrNull(dict, "lastSyncMeta");
        state.lastSyncMeta = meta instanceof JSONObject ? (JSONObject) meta : null;
        state.lastVersion = readLong(dict, "lastVersion");
        state.lastVersionId = valueOrNull(dict, "lastVersionId");
        state.lastReadDate = readLong(dict, "lastReadDate");
        state.lastFetchAttemptedDate = readLong(dict, "lastFetchAttemptedDate");
        state.lastFetchUnsuccessfulAttemptCount = (int) readLong(dict, "lastFetchUnsuccessfulAttemptCount");
        state.data = valueOrNull(dict, "data");
        return state;
    }

    JSONObject toJSON() {
        JSONObject dict = new JSONObject();
        try {
            dict.put("lastSyncDate", lastSyncDate);
            dict.put("lastSyncMeta", lastSyncMeta != null ? lastSyncMeta : JSONObject.NULL);
            dict.put("lastVersion", lastVersion);
            dict.put("lastVersionId", lastVersionId != null ? lastVersionId : JSONObject.NULL);
            dict.put("lastReadDate", lastReadDate);
            dict.put("lastFetchAttemptedDate", lastFetchAttemptedDate);
            dict.put("lastFetchUnsuccessfulAttemptCount", lastFetchUnsuccessfulAttemptCount);
            dict.put("data", data != null ? data : JSONObject.NULL);
        } catch (JSONException e) {
            // The keys are constant and the values are JSON-safe; this cannot happen.
        }
        return dict;
    }

    /**
     * Write this state's outbound wire params into {@code params}, each key prefixed by
     * {@code prefix}: always the int64 trio (lastSyncDate/lastVersion/lastReadDate); lastSyncMeta
     * JSON-encoded and lastVersionId only when set. Used by both opportunistic injection
     * (prefix {@code "_<source>Sync."}) and explicit fetch (prefix {@code ""}).
     */
    void writeWireParams(String prefix, Map<String, Object> params) {
        String p = prefix != null ? prefix : "";
        params.put(p + "lastSyncDate", lastSyncDate);
        params.put(p + "lastVersion", lastVersion);
        params.put(p + "lastReadDate", lastReadDate);
        if (lastSyncMeta != null) {
            params.put(p + "lastSyncMeta", lastSyncMeta.toString());
        }
        if (lastVersionId != null) {
            params.put(p + "lastVersionId", lastVersionId);
        }
    }

    SyncSourceState copy() {
        SyncSourceState c = new SyncSourceState();
        c.lastSyncDate = lastSyncDate;
        c.lastSyncMeta = lastSyncMeta;
        c.lastVersion = lastVersion;
        c.lastVersionId = lastVersionId;
        c.lastReadDate = lastReadDate;
        c.lastFetchAttemptedDate = lastFetchAttemptedDate;
        c.lastFetchUnsuccessfulAttemptCount = lastFetchUnsuccessfulAttemptCount;
        c.data = data;
        return c;
    }

    private static boolean nilSafeEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return JSONUtil.equals(a, b);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SyncSourceState)) return false;
        SyncSourceState o = (SyncSourceState) object;
        return lastSyncDate == o.lastSyncDate
                && lastVersion == o.lastVersion
                && lastReadDate == o.lastReadDate
                && lastFetchAttemptedDate == o.lastFetchAttemptedDate
                && lastFetchUnsuccessfulAttemptCount == o.lastFetchUnsuccessfulAttemptCount
                && nilSafeEqual(lastSyncMeta, o.lastSyncMeta)
                && nilSafeEqual(lastVersionId, o.lastVersionId)
                && nilSafeEqual(data, o.data);
    }

    @Override
    public int hashCode() {
        return (int) (lastSyncDate ^ lastVersion ^ lastReadDate
                ^ lastFetchAttemptedDate ^ lastFetchUnsuccessfulAttemptCount);
    }

    @Override
    public String toString() {
        return "SyncSourceState" + toJSON();
    }
}
