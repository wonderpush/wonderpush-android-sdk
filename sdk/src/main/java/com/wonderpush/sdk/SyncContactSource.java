package com.wonderpush.sdk;

import org.json.JSONObject;

/**
 * The {@code contact} sync source (single-object). Ported from wonderpush-ios-sdk
 * {@code WPSyncContactSource}. Pure transforms delegating to {@link SyncContactStore}.
 */
class SyncContactSource implements SyncSourcePlugin {

    @Override
    public Object applyData(Object data, Object currentData) {
        JSONObject current = (currentData instanceof JSONObject) ? (JSONObject) currentData : null;
        return SyncContactStore.applyContactData(current, data);
    }

    @Override
    public Object applyDelta(Object delta, Object currentData) {
        JSONObject current = (currentData instanceof JSONObject) ? (JSONObject) currentData : null;
        return SyncContactStore.applyContactDelta(current, delta);
    }
}
