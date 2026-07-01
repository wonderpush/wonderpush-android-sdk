package com.wonderpush.sdk;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure helpers for sdk-sync outgoing-param injection.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncOutgoing} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-outgoing.ts}. Spec: algorithm.md:84-91.
 *
 * The SDK piggybacks per-source sync state onto the opportunistic API calls (POST /events and
 * PATCH /installation). Path matching is host-agnostic by suffix (via the shared classifier), so it
 * covers both the SDK API and the Measurements API.
 */
class SyncOutgoing {

    private static final Map<String, String> ID_KEY_TO_PARAM = new LinkedHashMap<>();

    static {
        // NEVER contactId, by design — the server resolves it.
        ID_KEY_TO_PARAM.put("userId", "_syncUserId");
        ID_KEY_TO_PARAM.put("deviceId", "_syncDeviceId");
        ID_KEY_TO_PARAM.put("installationId", "_syncInstallationId");
        ID_KEY_TO_PARAM.put("visitorId", "_syncVisitorId");
    }

    /**
     * Whether sync params may be injected onto this request — true exactly for the opportunistic
     * endpoints. Delegates to the classifier so the inject set and the processed set stay identical,
     * and a GET/PUT/DELETE on those suffixes is NOT injected.
     */
    static boolean shouldInject(String path, String method) {
        return "opportunistic".equals(SyncProcessor.classifyResponse(path, method).mode);
    }

    /**
     * Build the params to inject: the 4 identifiers under {@code _sync<Id>} keys (empty/missing
     * skipped) and each registered source's state under {@code _<source>Sync.*} keys (lastSyncMeta
     * JSON-encoded; lastSyncMeta/lastVersionId omitted when null).
     */
    static Map<String, Object> buildOutgoingParams(JSONObject identifiers,
                                                    Map<String, SyncSourceState> statePerSource) {
        Map<String, Object> out = new LinkedHashMap<>();

        if (identifiers != null) {
            for (Map.Entry<String, String> e : ID_KEY_TO_PARAM.entrySet()) {
                Object value = identifiers.opt(e.getKey());
                if (value instanceof String && ((String) value).length() > 0) {
                    out.put(e.getValue(), value);
                }
            }
        }

        if (statePerSource != null) {
            for (Map.Entry<String, SyncSourceState> e : statePerSource.entrySet()) {
                String prefix = "_" + e.getKey() + "Sync.";
                e.getValue().writeWireParams(prefix, out);
            }
        }

        return out;
    }

    private SyncOutgoing() {}
}
