package com.wonderpush.sdk;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure timing/params helpers for the explicit-fetch loop.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncFetchPolicy} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-fetch.ts}. Backoff / debounce / rate-limit
 * are covered by the conformance vectors; {@link #buildExplicitFetchParams} shapes the GET request.
 */
class SyncFetchPolicy {

    private static final Map<String, String> EXPLICIT_PATH_BY_SOURCE = new HashMap<>();

    static {
        // Explicit sync endpoints live under the dedicated `/sync/` namespace (GET /v1/sync/{source}),
        // distinct from the opportunistic resource paths. No `/v1` prefix — the REST layer prepends it.
        EXPLICIT_PATH_BY_SOURCE.put("contact", "/sync/contact");
        EXPLICIT_PATH_BY_SOURCE.put("user", "/sync/user");
        EXPLICIT_PATH_BY_SOURCE.put("installation", "/sync/installation");
        EXPLICIT_PATH_BY_SOURCE.put("popups", "/sync/popups");
        EXPLICIT_PATH_BY_SOURCE.put("inbox", "/sync/inbox");
    }

    static String explicitPathForSource(String source) {
        return EXPLICIT_PATH_BY_SOURCE.get(source != null ? source : "");
    }

    static boolean shouldDebounceWeakSignal(long now, long lastFetchAttemptedDate, double debounceMs) {
        return lastFetchAttemptedDate > 0 && (double) (now - lastFetchAttemptedDate) < debounceMs;
    }

    static boolean shouldRateLimitSource(long now, long lastFetchAttemptedDate, double minIntervalMs) {
        return minIntervalMs > 0 && lastFetchAttemptedDate > 0
                && (double) (now - lastFetchAttemptedDate) < minIntervalMs;
    }

    static double computeBackoffSleep(int attemptCount, double rand, SyncKnobs knobs) {
        if (attemptCount <= 0) return 0;
        double raw = knobs.exponentialBackoffMinMs * Math.pow(knobs.exponentialBackoffRatio, (double) attemptCount);
        double capped = Math.min(knobs.exponentialBackoffMaxMs, raw);
        return capped * (1 + rand * knobs.exponentialBackoffJitterRatio);
    }

    /**
     * Coalesce a freshly-received {@code syncAfterTime} hint (CP-56 — "Late identifier resolution")
     * into the due date the SDK should schedule its one extra explicit sync for.
     *
     * The server re-emits the hint on every opportunistic call until the explicit call lands
     * (expected, not a bug) — so repeated hints must coalesce to the EARLIEST due time already
     * scheduled, never pushed later by a subsequent hint. If nothing is scheduled yet
     * ({@code existingDueDate} is 0), or the existing due date has already passed, the fresh hint wins.
     */
    static long coalesceSyncAfterTimeDueDate(long now, double delayMs, long existingDueDate) {
        long candidate = now + (long) Math.max(0.0, delayMs);
        if (existingDueDate > now && existingDueDate <= candidate) return existingDueDate;
        return candidate;
    }

    /** True iff value is a non-empty string (mirrors JS truthiness for the identifier fields). */
    private static boolean nonEmptyString(Object value) {
        return value instanceof String && ((String) value).length() > 0;
    }

    /**
     * Build the GET /sync/&lt;source&gt; query params: identifiers (never contactId; userId is added by
     * the request layer), the sync state at the top level (empty prefix, via the shared encoder), and
     * the echoed head hints when the fetch was triggered by one.
     */
    static Map<String, Object> buildExplicitFetchParams(JSONObject identifiers, SyncSourceState state,
                                                         SyncFetchHint hint) {
        Map<String, Object> params = new LinkedHashMap<>();

        if (identifiers != null) {
            for (String key : new String[]{"deviceId", "installationId", "visitorId"}) {
                Object value = identifiers.opt(key);
                if (nonEmptyString(value)) params.put(key, value);
            }
        }

        state.writeWireParams("", params);

        if (hint != null) {
            if (hint.knownVersion != null) params.put("knownVersion", hint.knownVersion);
            if (hint.knownVersionId != null && hint.knownVersionId != JSONObject.NULL) {
                params.put("knownVersionId", hint.knownVersionId);
            }
            if (hint.knownReadDate != null) params.put("knownReadDate", hint.knownReadDate);
        }

        return params;
    }

    private SyncFetchPolicy() {}
}
