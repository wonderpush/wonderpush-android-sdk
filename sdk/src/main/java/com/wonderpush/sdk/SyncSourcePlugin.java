package com.wonderpush.sdk;

/**
 * A source's optional data transforms for the sdk-sync channel.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncSourcePlugin}. PURE functions: given the source's
 * current stored data and a payload, return the new data. The orchestrator owns persistence — it
 * folds the result into the source's state and saves once, under the response's captured profile +
 * per-source lock — so the plug-in never re-reads the profile or touches storage.
 *
 * The defaults make a plug-in that overrides neither behave as a raw full-replace with no delta
 * handling (matching iOS's "respondsToSelector" fallbacks).
 */
interface SyncSourcePlugin {

    /** Full reset (single-object: replace; multi-object: full list). Default: raw replace. */
    default Object applyData(Object data, Object currentData) {
        return data;
    }

    /** Patch (single-object: merge; multi-object: merge items). Default: no-op. */
    default Object applyDelta(Object delta, Object currentData) {
        return currentData;
    }
}
