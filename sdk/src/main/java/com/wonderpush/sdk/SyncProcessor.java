package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * The pure sdk-sync response processor: routing + the per-source decision.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncProcessor} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-processor.ts}.
 * Touches neither storage nor network — returns a {@link SyncDecision} the orchestrator applies.
 */
class SyncProcessor {

    /** Result of {@link #classifyResponse}: how a response should (or shouldn't) be processed. */
    static class Classification {
        String mode = "none";          // "opportunistic" | "explicit" | "none"
        String explicitSource;         // set only when mode == "explicit"

        JSONObject toJSON() {
            JSONObject d = new JSONObject();
            try {
                d.put("mode", mode != null ? mode : "none");
                if (explicitSource != null) d.put("explicitSource", explicitSource);
            } catch (JSONException e) {
                // Constant keys, JSON-safe values; cannot happen.
            }
            return d;
        }
    }

    private static final Map<String, String[]> OPPORTUNISTIC_PATHS_BY_METHOD = new HashMap<>();
    private static final Map<String, String> EXPLICIT_SOURCE_BY_PATH = new HashMap<>();

    static {
        OPPORTUNISTIC_PATHS_BY_METHOD.put("POST", new String[]{"/events"});
        OPPORTUNISTIC_PATHS_BY_METHOD.put("PATCH", new String[]{"/installation"});
        // Explicit sync fetches: GET /v1/sync/{source}. The dedicated `/sync/` namespace keeps these
        // distinct from opportunistic resource paths — so GET /v1/installation (no /sync) classifies
        // as none, removing the old GET-vs-PATCH ambiguity on /installation.
        EXPLICIT_SOURCE_BY_PATH.put("/sync/contact", "contact");
        EXPLICIT_SOURCE_BY_PATH.put("/sync/user", "user");
        EXPLICIT_SOURCE_BY_PATH.put("/sync/installation", "installation");
        EXPLICIT_SOURCE_BY_PATH.put("/sync/popups", "popups");
        EXPLICIT_SOURCE_BY_PATH.put("/sync/inbox", "inbox");
    }

    /**
     * Accepts both '/events' and '/v1/events'-prefixed forms. The leading '/' in each suffix enforces
     * a path-segment boundary, so '/foo-inbox' does not match '/inbox'.
     */
    private static boolean pathMatchesSuffix(String path, String suffix) {
        return path.equals(suffix) || path.endsWith(suffix);
    }

    /** {@link JSONObject#NULL} -> null; otherwise pass through. Normalizes a versionId. */
    private static Object denull(Object v) {
        return v == JSONObject.NULL ? null : v;
    }

    static Classification classifyResponse(String path, String method) {
        Classification c = new Classification();
        if (path == null || path.length() == 0) return c;
        String m = (method != null ? method : "").toUpperCase();

        String[] oppSuffixes = OPPORTUNISTIC_PATHS_BY_METHOD.get(m);
        if (oppSuffixes != null) {
            for (String suffix : oppSuffixes) {
                if (pathMatchesSuffix(path, suffix)) { c.mode = "opportunistic"; return c; }
            }
        }

        if (m.equals("GET")) {
            for (Map.Entry<String, String> e : EXPLICIT_SOURCE_BY_PATH.entrySet()) {
                if (pathMatchesSuffix(path, e.getKey())) {   // suffixes are mutually exclusive
                    c.mode = "explicit";
                    c.explicitSource = e.getValue();
                    return c;
                }
            }
        }
        return c;
    }

    private static SyncFetchHint buildFetchHint(SyncResponseBlock block) {
        SyncFetchHint h = new SyncFetchHint();
        if (block.knownVersion() != null) h.knownVersion = block.knownVersion();
        if (block.hasKnownVersionId()) h.knownVersionId = block.knownVersionId();
        if (block.knownReadDate() != null) h.knownReadDate = block.knownReadDate();
        return h;
    }

    static SyncDecision processSourceBlock(SyncResponseBlock block, Number serverTime,
                                           SyncSourceState state, String mode) {
        SyncDecision decision = new SyncDecision();

        // 1. Block missing -> do nothing.
        if (block == null) return decision;

        // 2. Empty {} -> "try asking explicitly" (opportunistic) or nothing (explicit).
        if (block.isEmpty()) {
            if ("opportunistic".equals(mode)) decision.triggerFetch = "weak";
            return decision;
        }

        SyncSourceState next = state.copy();
        boolean stateChanged = false;

        // 3. meta is opaque — store + echo only, no acceptance gate.
        if (block.meta() != null) { next.lastSyncMeta = block.meta(); stateChanged = true; }

        boolean hasPayload = block.hasData() || block.hasDelta();
        if (hasPayload) {
            // 4. Payload-bearing: acceptance check.
            long version = block.version() != null ? block.version().longValue() : 0;
            long readDate = block.readDate() != null ? block.readDate().longValue() : 0;
            Object data = block.hasData() ? block.data() : null;
            boolean accepted = SyncVersionId.acceptsResponse(version,
                    block.hasVersionId() ? block.versionId() : null,
                    readDate, data,
                    state.lastVersion, state.lastVersionId, state.lastReadDate);
            if (!accepted) {
                // Stale/out-of-order: drop payload, but keep any meta update already captured.
                if (stateChanged) decision.nextState = next;
                return decision;
            }

            if (version == 0 && SyncVersionId.isEmptyDataPayload(data)) decision.clearState = true;
            if (block.hasData()) { decision.hasApplyData = true; decision.applyData = block.data(); }
            if (block.hasDelta()) { decision.hasApplyDelta = true; decision.applyDelta = block.delta(); }

            if (block.version() != null) next.lastVersion = version;
            if (block.hasVersionId()) next.lastVersionId = denull(block.versionId());
            if (block.readDate() != null && readDate > state.lastReadDate) next.lastReadDate = readDate;
            stateChanged = true;
        } else {
            // 5. No payload: "no change confirmed" or hint-only.
            if (block.version() != null && block.hasVersionId()) {
                long bver = block.version().longValue();
                int idCmp = SyncVersionId.compareVersionId(denull(block.versionId()), state.lastVersionId);
                if (bver == state.lastVersion && idCmp == 0) {
                    if (block.readDate() != null && block.readDate().longValue() > next.lastReadDate) {
                        next.lastReadDate = block.readDate().longValue();
                        stateChanged = true;
                    }
                } else if (bver > state.lastVersion || (bver == state.lastVersion && idCmp > 0)) {
                    decision.triggerFetch = "firm";
                }
                // strictly lower: ignore
            } else if (block.readDate() != null) {
                // Degenerate "no change confirmed" — only readDate.
                long brd = block.readDate().longValue();
                if (brd >= state.lastReadDate && brd > next.lastReadDate) {
                    next.lastReadDate = brd;
                    stateChanged = true;
                }
            }
        }

        // 6. Advance lastSyncDate from _serverTime if higher.
        if (serverTime != null && serverTime.longValue() > next.lastSyncDate) {
            next.lastSyncDate = serverTime.longValue();
            stateChanged = true;
        }

        // 7. Head hints — compared against the (possibly just-updated) lastVersion.
        if (block.knownVersion() != null && block.hasKnownVersionId()) {
            long kver = block.knownVersion().longValue();
            int headIdCmp = SyncVersionId.compareVersionId(denull(block.knownVersionId()), next.lastVersionId);
            if (kver > next.lastVersion || (kver == next.lastVersion && headIdCmp > 0)) {
                decision.fetchHint = buildFetchHint(block);
                if ("explicit".equals(mode)) {
                    decision.continuePaging = true;   // more pages remain; orchestrator continues, floor-exempt
                } else {
                    decision.triggerFetch = "firm";
                }
            }
        } else if (block.knownReadDate() != null && block.knownVersion() == null) {
            // Weak hint (knownReadDate only). Debounced fetch; don't downgrade an existing firm decision.
            if (block.knownReadDate().longValue() > next.lastReadDate && decision.triggerFetch == null) {
                decision.triggerFetch = "weak";
                decision.fetchHint = buildFetchHint(block);
            }
        }

        if (stateChanged) decision.nextState = next;
        return decision;
    }

    private SyncProcessor() {}
}
