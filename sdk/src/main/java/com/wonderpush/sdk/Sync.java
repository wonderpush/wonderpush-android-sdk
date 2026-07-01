package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The sdk-sync orchestrator. Ported from wonderpush-ios-sdk {@code WPSync} /
 * wonderpush-javascript-sdk {@code sync.js}. Ties the pieces together:
 *   - {@link #registerSource} — declare a source and its apply/clear callbacks.
 *   - {@link #prepareOutgoingParams} — params to inject on opportunistic requests.
 *   - {@link #consumeIncomingResponse} — classify, run the processor per source, execute decisions,
 *     then max-age forcing.
 *   - {@link #dataForSource} — the synced payload, for the segmentation/popup engines.
 *
 * Per-source response processing is serialized (load→process→save→callbacks under a per-source lock)
 * so two responses for the same source can't interleave and clobber state.
 */
class Sync implements SyncRequestObserver {

    interface IdentifiersProvider {
        /** A JSON object with optional userId/deviceId/installationId/visitorId. */
        JSONObject identifiers();
    }

    interface KnobsProvider {
        SyncKnobs knobs();
    }

    /**
     * Posted (best-effort) after a source's stored DATA changes as the result of applying a response
     * (reset / delta / clear). The in-app engine observes this for source "popups".
     */
    interface SourceDataChangeListener {
        void onSourceDataChanged(String source);
    }

    private final SyncStateStore stateStore;
    private final SyncFetching fetcher;
    private final Map<String, SyncSourcePlugin> sources = new HashMap<>();   // value may be null (state-only)
    private final Map<String, Object> procLocks = new HashMap<>();
    private final Object registryLock = new Object();
    private final List<SourceDataChangeListener> listeners = new CopyOnWriteArrayList<>();

    /** Current identifiers. Default: empty. */
    IdentifiersProvider identifiersProvider = JSONObject::new;
    /** Effective knobs. Default: {@link SyncKnobs#defaultKnobs}. */
    KnobsProvider knobsProvider = SyncKnobs::defaultKnobs;
    /** Current time in ms. Default: server-adjusted clock. */
    SyncClock nowProvider = TimeSync::getTime;

    Sync(SyncStateStore stateStore, SyncFetching fetcher) {
        this.stateStore = stateStore;
        this.fetcher = fetcher;
    }

    // region registry

    void registerSource(String name, SyncSourcePlugin plugin) {
        synchronized (registryLock) {
            sources.put(name, plugin);
            if (!procLocks.containsKey(name)) procLocks.put(name, new Object());
        }
    }

    List<String> registeredSources() {
        synchronized (registryLock) {
            return new ArrayList<>(sources.keySet());
        }
    }

    private Object procLockForSource(String source) {
        synchronized (registryLock) {
            Object lock = procLocks.get(source);
            if (lock == null) { lock = new Object(); procLocks.put(source, lock); }
            return lock;
        }
    }

    private SyncSourcePlugin pluginForSource(String source) {
        synchronized (registryLock) {
            return sources.get(source);
        }
    }

    void addSourceDataChangeListener(SourceDataChangeListener listener) {
        if (listener != null) listeners.add(listener);
    }

    void removeSourceDataChangeListener(SourceDataChangeListener listener) {
        listeners.remove(listener);
    }

    // endregion

    // region helpers

    /** Effective knobs, failing open to defaults (a null provider result must not read 0.0 age caps). */
    private SyncKnobs effectiveKnobs() {
        SyncKnobs k = knobsProvider.knobs();
        return k != null ? k : SyncKnobs.defaultKnobs();
    }

    /** The current identifiers, or null when there's no usable deviceId yet (sync is a no-op before init). */
    private JSONObject currentValidIdentifiers() {
        JSONObject ids = identifiersProvider.identifiers();
        if (ids == null) ids = new JSONObject();
        Object deviceId = ids.opt("deviceId");
        return (deviceId instanceof String && ((String) deviceId).length() > 0) ? ids : null;
    }

    /** Extract a non-empty string identifier, or null. */
    private static String idString(JSONObject ids, String key) {
        Object v = ids.opt(key);
        return (v instanceof String && ((String) v).length() > 0) ? (String) v : null;
    }

    // endregion

    // region outgoing

    @Override
    public Map<String, Object> prepareOutgoingParams(String path, String method) {
        try {
            if (!SyncOutgoing.shouldInject(path, method)) return Collections.emptyMap();
            if (!effectiveKnobs().opportunisticInjectionEnabled) return Collections.emptyMap();
            JSONObject ids = currentValidIdentifiers();
            if (ids == null) return Collections.emptyMap();
            String userId = idString(ids, "userId");
            String deviceId = idString(ids, "deviceId");
            Map<String, SyncSourceState> statePerSource = new LinkedHashMap<>();
            for (String source : registeredSources()) {
                statePerSource.put(source, stateStore.loadSource(source, userId, deviceId));
            }
            return SyncOutgoing.buildOutgoingParams(ids, statePerSource);
        } catch (Throwable t) {
            Log.w(WonderPush.TAG, "Sync: prepareOutgoingParams failed, skipping injection", t);
            return Collections.emptyMap();   // best-effort: never break the host request
        }
    }

    // endregion

    // region incoming

    @Override
    public void consumeIncomingResponse(String path, String method, JSONObject response) {
        try {
            if (response == null) return;
            SyncProcessor.Classification c = SyncProcessor.classifyResponse(path, method);
            if ("none".equals(c.mode)) return;
            JSONObject ids = currentValidIdentifiers();
            if (ids == null) return;
            String userId = idString(ids, "userId");
            String deviceId = idString(ids, "deviceId");
            Object stObj = response.opt("_serverTime");
            Number serverTime = (stObj instanceof Number) ? (Number) stObj : null;

            if ("opportunistic".equals(c.mode)) {
                // Iterate ALL registered sources (the processor handles a missing block correctly).
                for (String source : registeredSources()) {
                    Object block = response.opt("_" + source + "Sync");
                    processSource(source, block, serverTime, ids, userId, deviceId, "opportunistic");
                }
            } else if (c.explicitSource != null) {
                // The response root IS the block; SyncResponseBlock reads only recognized keys.
                processSource(c.explicitSource, response, serverTime, ids, userId, deviceId, "explicit");
            }
            checkMaxAgeForcing(ids, userId, deviceId);
        } catch (Throwable t) {
            Log.w(WonderPush.TAG, "Sync: consumeIncomingResponse failed", t);   // never break the host callback chain
        }
    }

    /**
     * Serialized per source: load → process → apply (save + data) under the per-source lock. The fetch
     * trigger fires AFTER releasing the lock — a synchronous transport completion would otherwise
     * re-enter this same lock and nest the read-modify-write on partially-applied state.
     */
    private void processSource(String source, Object blockDict, Number serverTime, JSONObject ids,
                               String userId, String deviceId, String mode) {
        SyncDecision decision;
        synchronized (procLockForSource(source)) {
            SyncSourceState state = stateStore.loadSource(source, userId, deviceId);
            SyncResponseBlock block = (blockDict instanceof JSONObject)
                    ? SyncResponseBlock.fromJSON((JSONObject) blockDict) : null;
            decision = SyncProcessor.processSourceBlock(block, serverTime, state, mode);
            applyDecision(decision, source, userId, deviceId);
        }
        // Notify consumers (the in-app engine) that this source's DATA changed, AFTER releasing the lock.
        boolean dataChanged = decision.nextState != null
                && (decision.hasApplyData || decision.hasApplyDelta || decision.clearState);
        if (dataChanged) {
            for (SourceDataChangeListener l : listeners) {
                try { l.onSourceDataChanged(source); } catch (Throwable ignored) { /* best-effort */ }
            }
        }
        if (decision.triggerFetch != null) {
            // Fire-and-forget, outside the lock; forward the head hint so the explicit request echoes known*.
            fetcher.fetchSource(source, userId, deviceId, ids, effectiveKnobs(),
                    "weak".equals(decision.triggerFetch), decision.fetchHint, null);
        }
        // decision.continuePaging (multi-object paging) is wired with the popups source.
    }

    /**
     * Fold the decision's data transforms into the new state and persist once, under the captured
     * profile. The plug-in supplies PURE transforms; the orchestrator owns the data + its persistence.
     */
    private void applyDecision(SyncDecision decision, String source, String userId, String deviceId) {
        SyncSourceState next = decision.nextState;
        if (next == null) return;   // no state change (and thus no data to apply)
        SyncSourcePlugin plugin = pluginForSource(source);
        if (decision.clearState) {
            next.data = null;
        }
        if (decision.hasApplyData) {
            next.data = (plugin != null) ? plugin.applyData(decision.applyData, next.data) : decision.applyData;
        }
        if (decision.hasApplyDelta && plugin != null) {
            next.data = plugin.applyDelta(decision.applyDelta, next.data);
        }
        stateStore.saveState(next, source, userId, deviceId);
    }

    // endregion

    // region max-age forcing

    private void checkMaxAgeForcing(JSONObject ids, String userId, String deviceId) {
        SyncKnobs knobs = effectiveKnobs();
        if (!Double.isFinite(knobs.maxLastSyncDateAgeMs) && !Double.isFinite(knobs.maxLastReadDateAgeMs)) {
            return;   // fast path: no forcing
        }
        long now = nowProvider.now();
        for (String source : registeredSources()) {
            SyncSourceState state = stateStore.loadSource(source, userId, deviceId);
            if (SyncKnobs.isStateStale(state, knobs, now)) {
                fetcher.fetchSource(source, userId, deviceId, ids, knobs, false, null, null);   // firm, non-debounced
            }
        }
    }

    // endregion

    // region read

    Object dataForSource(String source) {
        JSONObject ids = currentValidIdentifiers();
        if (ids == null) return null;
        return stateStore.loadSource(source, idString(ids, "userId"), idString(ids, "deviceId")).data;
    }

    // endregion
}
