package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The canonical knob set for the sdk-sync channel.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncKnobs} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-knobs.ts}.
 * Spec-tunable parameters: sdk-sync/docs/docs/sync/algorithm.md:349-355.
 *
 * Age caps default to +Infinity (no forcing), so those fields are {@code double}.
 */
class SyncKnobs {

    /** WEAK_SYNC_SIGNAL_DEBOUNCE: minimum delay since last fetch for a weak-signal-triggered fetch. */
    double weakSyncSignalDebounceMs;
    /** Forces a fetch when lastSyncDate is older than this (may be +Infinity == no forcing). */
    double maxLastSyncDateAgeMs;
    /** Forces a fetch when lastReadDate is older than this (may be +Infinity == no forcing). */
    double maxLastReadDateAgeMs;
    /** Cap on stored popup items. */
    int maxPopupsEntries;
    /** Cap on stored inbox items. */
    int maxInboxEntries;

    /** Exponential-backoff base (ms). */
    double exponentialBackoffMinMs;
    /** Exponential-backoff cap (ms). */
    double exponentialBackoffMaxMs;
    /** Exponential-backoff growth ratio. */
    double exponentialBackoffRatio;
    /** Exponential-backoff jitter ratio. */
    double exponentialBackoffJitterRatio;

    /** Mutex liveness TTL (ms). */
    double mutexTtlMs;
    /** Opportunistic-injection kill switch. */
    boolean opportunisticInjectionEnabled;
    /** Per-source floor between explicit-fetch attempts (ms); 0 disables. */
    double minSourceFetchIntervalMs;

    /**
     * Reads a double, mapping the JSON string sentinels {@code "Infinity"}/{@code "-Infinity"}/
     * {@code "NaN"} (JSON has no literal for these; default-knobs.json / the vectors encode them as
     * strings). Absent or otherwise-invalid values yield 0.
     */
    private static double readDouble(JSONObject dict, String key) {
        Object v = dict.opt(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            switch ((String) v) {
                case "Infinity": return Double.POSITIVE_INFINITY;
                case "-Infinity": return Double.NEGATIVE_INFINITY;
                case "NaN": return Double.NaN;
                default: return 0;
            }
        }
        return 0;
    }

    /**
     * Mirrors the JS {@code pickNumber}: accept only a real (non-NaN) JSON number. A boolean is NOT a
     * number (JS {@code typeof true === 'boolean'}) — in Java a JSON boolean is a {@link Boolean},
     * which is not a {@link Number}, so {@code instanceof Number} already excludes it.
     */
    private static double pickNumber(JSONObject data, String key, double fallback) {
        Object c = data.opt(key);
        if (c instanceof Number) {
            double d = ((Number) c).doubleValue();
            if (!Double.isNaN(d)) return d;
        }
        return fallback;
    }

    /**
     * Round-trips with the JSON shape used by default-knobs.json and the conformance vectors
     * (the SyncKnobs field names; the "Infinity" sentinel is mapped here).
     */
    static SyncKnobs fromJSON(JSONObject dict) {
        SyncKnobs k = new SyncKnobs();
        if (dict == null) return k;
        k.weakSyncSignalDebounceMs = readDouble(dict, "weakSyncSignalDebounceMs");
        k.maxLastSyncDateAgeMs = readDouble(dict, "maxLastSyncDateAgeMs");
        k.maxLastReadDateAgeMs = readDouble(dict, "maxLastReadDateAgeMs");
        k.maxPopupsEntries = (int) readDouble(dict, "maxPopupsEntries");
        k.maxInboxEntries = (int) readDouble(dict, "maxInboxEntries");
        k.exponentialBackoffMinMs = readDouble(dict, "exponentialBackoffMinMs");
        k.exponentialBackoffMaxMs = readDouble(dict, "exponentialBackoffMaxMs");
        k.exponentialBackoffRatio = readDouble(dict, "exponentialBackoffRatio");
        k.exponentialBackoffJitterRatio = readDouble(dict, "exponentialBackoffJitterRatio");
        k.mutexTtlMs = readDouble(dict, "mutexTtlMs");
        k.opportunisticInjectionEnabled = dict.optBoolean("opportunisticInjectionEnabled");
        k.minSourceFetchIntervalMs = readDouble(dict, "minSourceFetchIntervalMs");
        return k;
    }

    /** The canonical default knob values (DEFAULT_KNOBS in sync-knobs.ts:29-56). */
    static SyncKnobs defaultKnobs() {
        SyncKnobs k = new SyncKnobs();
        k.weakSyncSignalDebounceMs = 5000;
        k.maxLastSyncDateAgeMs = Double.POSITIVE_INFINITY;
        k.maxLastReadDateAgeMs = Double.POSITIVE_INFINITY;
        k.maxPopupsEntries = 1000;
        k.maxInboxEntries = 1000;
        k.exponentialBackoffMinMs = 1000;
        k.exponentialBackoffMaxMs = 300000;
        k.exponentialBackoffRatio = 2;
        k.exponentialBackoffJitterRatio = 0.5;
        k.mutexTtlMs = 600000;
        k.opportunisticInjectionEnabled = true;
        k.minSourceFetchIntervalMs = 2000;
        return k;
    }

    SyncKnobs copy() {
        SyncKnobs k = new SyncKnobs();
        k.weakSyncSignalDebounceMs = weakSyncSignalDebounceMs;
        k.maxLastSyncDateAgeMs = maxLastSyncDateAgeMs;
        k.maxLastReadDateAgeMs = maxLastReadDateAgeMs;
        k.maxPopupsEntries = maxPopupsEntries;
        k.maxInboxEntries = maxInboxEntries;
        k.exponentialBackoffMinMs = exponentialBackoffMinMs;
        k.exponentialBackoffMaxMs = exponentialBackoffMaxMs;
        k.exponentialBackoffRatio = exponentialBackoffRatio;
        k.exponentialBackoffJitterRatio = exponentialBackoffJitterRatio;
        k.mutexTtlMs = mutexTtlMs;
        k.opportunisticInjectionEnabled = opportunisticInjectionEnabled;
        k.minSourceFetchIntervalMs = minSourceFetchIntervalMs;
        return k;
    }

    /**
     * Merge remote-config overrides on top of the defaults. Reads the {@code sync*}-prefixed keys;
     * any field absent or non-numeric falls back to the default. The kill switch
     * {@code syncOpportunisticInjection} disables injection only on an explicit boolean false
     * (anything else keeps it on, matching JS {@code value !== false}). Pure.
     */
    static SyncKnobs mergeKnobs(SyncKnobs defaults, JSONObject data) {
        SyncKnobs k = defaults.copy();
        if (data == null) return k;   // null / non-dict -> defaults
        k.weakSyncSignalDebounceMs = pickNumber(data, "syncWeakSignalDebounceMs", defaults.weakSyncSignalDebounceMs);
        k.maxLastSyncDateAgeMs = pickNumber(data, "syncMaxLastSyncDateAgeMs", defaults.maxLastSyncDateAgeMs);
        k.maxLastReadDateAgeMs = pickNumber(data, "syncMaxLastReadDateAgeMs", defaults.maxLastReadDateAgeMs);
        k.maxPopupsEntries = (int) pickNumber(data, "syncMaxPopupsEntries", defaults.maxPopupsEntries);
        k.maxInboxEntries = (int) pickNumber(data, "syncMaxInboxEntries", defaults.maxInboxEntries);
        k.exponentialBackoffMinMs = pickNumber(data, "syncBackoffMinMs", defaults.exponentialBackoffMinMs);
        k.exponentialBackoffMaxMs = pickNumber(data, "syncBackoffMaxMs", defaults.exponentialBackoffMaxMs);
        k.exponentialBackoffRatio = pickNumber(data, "syncBackoffRatio", defaults.exponentialBackoffRatio);
        k.exponentialBackoffJitterRatio = pickNumber(data, "syncBackoffJitterRatio", defaults.exponentialBackoffJitterRatio);
        k.mutexTtlMs = pickNumber(data, "syncMutexTtlMs", defaults.mutexTtlMs);
        k.minSourceFetchIntervalMs = pickNumber(data, "syncMinSourceFetchIntervalMs", defaults.minSourceFetchIntervalMs);
        Object inj = data.opt("syncOpportunisticInjection");
        boolean isBooleanFalse = (inj instanceof Boolean) && !((Boolean) inj);
        k.opportunisticInjectionEnabled = !isBooleanFalse;
        return k;
    }

    /**
     * Should the SDK force a fetch because the source's state is too old? True when a finite age cap
     * is exceeded for lastSyncDate or lastReadDate. Both checks are gated on the field being &gt; 0 (a
     * never-fetched source is never "stale"). Pure — tests inject {@code now}.
     */
    static boolean isStateStale(SyncSourceState state, SyncKnobs knobs, long now) {
        if (state.lastSyncDate > 0 && Double.isFinite(knobs.maxLastSyncDateAgeMs)) {
            if ((double) (now - state.lastSyncDate) > knobs.maxLastSyncDateAgeMs) return true;
        }
        if (state.lastReadDate > 0 && Double.isFinite(knobs.maxLastReadDateAgeMs)) {
            if ((double) (now - state.lastReadDate) > knobs.maxLastReadDateAgeMs) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SyncKnobs)) return false;
        SyncKnobs o = (SyncKnobs) object;
        return weakSyncSignalDebounceMs == o.weakSyncSignalDebounceMs
                && maxLastSyncDateAgeMs == o.maxLastSyncDateAgeMs
                && maxLastReadDateAgeMs == o.maxLastReadDateAgeMs
                && maxPopupsEntries == o.maxPopupsEntries
                && maxInboxEntries == o.maxInboxEntries
                && exponentialBackoffMinMs == o.exponentialBackoffMinMs
                && exponentialBackoffMaxMs == o.exponentialBackoffMaxMs
                && exponentialBackoffRatio == o.exponentialBackoffRatio
                && exponentialBackoffJitterRatio == o.exponentialBackoffJitterRatio
                && mutexTtlMs == o.mutexTtlMs
                && opportunisticInjectionEnabled == o.opportunisticInjectionEnabled
                && minSourceFetchIntervalMs == o.minSourceFetchIntervalMs;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(weakSyncSignalDebounceMs) ^ Double.hashCode(minSourceFetchIntervalMs);
    }
}
