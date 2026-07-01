package com.wonderpush.sdk;

import org.json.JSONObject;

/**
 * Persistent store for per-(source, profile) sdk-sync state.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncStateStore}. Backed by a single
 * {@link WonderPushConfiguration} JSON blob {@code {storageKey: stateDict}}, keyed
 * {@code sync:<source>:<userId or "">:<deviceId>} (a profile switch via setUserId does not leak
 * state across users). The read-modify-write is serialized so concurrent writes from different
 * sources don't clobber the shared blob (last-writer-wins).
 */
class SyncStateStore {

    private static final SyncStateStore SHARED = new SyncStateStore();

    static SyncStateStore defaultStore() {
        return SHARED;
    }

    /** null/empty userId both collapse to "" (JS {@code userId || ''}). */
    static String storageKey(String source, String userId, String deviceId) {
        String uid = (userId != null && userId.length() > 0) ? userId : "";
        return "sync:" + (source != null ? source : "") + ":" + uid + ":" + (deviceId != null ? deviceId : "");
    }

    private JSONObject rootDictionary() {
        JSONObject root = WonderPushConfiguration.getSdkSyncStatePerProfile();
        return root != null ? root : new JSONObject();
    }

    synchronized SyncSourceState loadSource(String source, String userId, String deviceId) {
        String key = storageKey(source, userId, deviceId);
        JSONObject stateDict = rootDictionary().optJSONObject(key);
        if (stateDict != null) {
            return SyncSourceState.fromJSON(stateDict);
        }
        return SyncSourceState.emptyState();
    }

    synchronized void saveState(SyncSourceState state, String source, String userId, String deviceId) {
        JSONObject root = rootDictionary();
        String key = storageKey(source, userId, deviceId);
        try {
            root.put(key, state.toJSON());
        } catch (org.json.JSONException e) {
            // key is a non-null String and the value is a JSONObject; cannot happen.
        }
        WonderPushConfiguration.setSdkSyncStatePerProfile(root);
    }
}
