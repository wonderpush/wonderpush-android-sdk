package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The pure processor's decision for a single source's response block.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncDecision} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-processor.ts} (ProcessorDecision).
 * The processor never touches storage/network: it returns this decision and the orchestrator
 * applies it, in order:
 *   1. Save newState (if set).
 *   2. Plug-in callbacks: clearState, then applyData, then applyDelta.
 *   3. Trigger the fetch (if set), echoing fetchHint on the explicit request.
 *
 * {@code applyData} / {@code applyDelta} may legitimately be set to null/empty (e.g. the
 * "no data exists" reset), so each has a companion {@code hasX} flag to distinguish "set to null"
 * from "not set".
 */
class SyncDecision {

    /** The state to persist; null means "no state change" (serialized under the key "newState"). */
    SyncSourceState nextState;
    /** Call the plug-in's clearState() (currently only on empty-reset). */
    boolean clearState;

    boolean hasApplyData;
    Object applyData;      // applied before delta
    boolean hasApplyDelta;
    Object applyDelta;

    /** {@code "weak"} (debounced) or {@code "firm"}; null means no fetch. */
    String triggerFetch;
    /** The head hint to echo on the explicit request; set only for known*-triggered fetches. */
    SyncFetchHint fetchHint;
    /**
     * On the explicit path, set when a head hint sits above the page we just applied (more pages
     * remain) — the orchestrator continues paging immediately, floor-exempt.
     */
    boolean continuePaging;

    /**
     * Minimal dictionary matching the JS decision shape (omits unset fields). Used by the
     * conformance harness for deep-equality against the vectors' {@code expected}.
     */
    JSONObject toJSON() {
        JSONObject dict = new JSONObject();
        try {
            if (nextState != null) dict.put("newState", nextState.toJSON());
            if (clearState) dict.put("clearState", true);
            if (hasApplyData) dict.put("applyData", applyData != null ? applyData : JSONObject.NULL);
            if (hasApplyDelta) dict.put("applyDelta", applyDelta != null ? applyDelta : JSONObject.NULL);
            if (triggerFetch != null) dict.put("triggerFetch", triggerFetch);
            if (fetchHint != null) dict.put("fetchHint", fetchHint.toJSON());
            if (continuePaging) dict.put("continuePaging", true);
        } catch (JSONException e) {
            // Constant keys, JSON-safe values; cannot happen.
        }
        return dict;
    }

    @Override
    public String toString() {
        return "SyncDecision" + toJSON();
    }
}
