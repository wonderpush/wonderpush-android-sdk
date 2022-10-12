package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract class JSONSync {

    interface ResponseHandler {
        void onSuccess();
        void onFailure();
    }

    abstract void persist(JSONObject state);
    abstract void schedulePatchCall();
    abstract void serverPatchInstallation(JSONObject diff, ResponseHandler handler);

    private static final String UPGRADE_META_VERSION_KEY = "version";
    private static final Long UPGRADE_META_VERSION_0_INITIAL = 0l;
    private static final Long UPGRADE_META_VERSION_1_IMPORTED_CUSTOM = 1l;
    private static final Long UPGRADE_META_VERSION_LATEST = UPGRADE_META_VERSION_1_IMPORTED_CUSTOM;
    private static final int SAVED_STATE_STATE_VERSION_1 = 1;
    private static final int SAVED_STATE_STATE_VERSION_2 = 2;
    protected static final String SAVED_STATE_FIELD__SYNC_STATE_VERSION = "_syncStateVersion";
    protected static final String SAVED_STATE_FIELD_UPGRADE_META = "upgradeMeta";
    protected static final String SAVED_STATE_FIELD_SDK_STATE = "sdkState";
    protected static final String SAVED_STATE_FIELD_SERVER_STATE = "serverState";
    protected static final String SAVED_STATE_FIELD_PUT_ACCUMULATOR = "putAccumulator";
    protected static final String SAVED_STATE_FIELD_INFLIGHT_DIFF = "inflightDiff";
    protected static final String SAVED_STATE_FIELD_INFLIGHT_PUT_ACCUMULATOR = "inflightPutAccumulator";
    protected static final String SAVED_STATE_FIELD_SCHEDULED_PATCH_CALL = "scheduledPatchCall";
    protected static final String SAVED_STATE_FIELD_INFLIGHT_PATCH_CALL = "inflightPatchCall";

    private JSONObject sdkState;
    private JSONObject serverState;
    private JSONObject putAccumulator;
    private JSONObject inflightDiff;
    private JSONObject inflightPutAccumulator;
    private final JSONObject upgradeMeta;
    private boolean scheduledPatchCall;
    private boolean inflightPatchCall;

    JSONSync() {
        this(null, null, null, null, null, null, false, false);
    }

    JSONSync(JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, JSONObject inflightDiff, JSONObject inflightPutAccumulator, JSONObject upgradeMeta, boolean scheduledPatchCall, boolean inflightPatchCall) {
        if (sdkState == null) sdkState = new JSONObject();
        if (serverState == null) serverState = new JSONObject();
        if (putAccumulator == null) putAccumulator = new JSONObject();
        if (inflightDiff == null) inflightDiff = new JSONObject();
        if (upgradeMeta == null) upgradeMeta = new JSONObject();
        if (inflightPutAccumulator == null) inflightPutAccumulator = new JSONObject();

        try {
            JSONUtil.stripNulls(sdkState);
        } catch (JSONException ex) {
            WonderPush.logError("Unexpected JSON error while removing null fields on sdkState", ex);
        }
        try {
            JSONUtil.stripNulls(serverState);
        } catch (JSONException ex) {
            WonderPush.logError("Unexpected JSON error while removing null fields on serverState", ex);
        }

        this.sdkState = sdkState;
        this.serverState = serverState;
        this.putAccumulator = putAccumulator;
        this.inflightDiff = inflightDiff;
        this.inflightPutAccumulator = inflightPutAccumulator;
        this.upgradeMeta = upgradeMeta;
        this.scheduledPatchCall = scheduledPatchCall;
        this.inflightPatchCall = inflightPatchCall;

        // Handle state version upgrades
        // - 0 -> 1: No-op. 0 means no previous state.
        // - 1 -> 2: No-op. Only the "upgradeMeta" key has been added and it is read with proper default.

        // Handle client upgrades
        applyUpgrade();

        if (this.inflightPatchCall) {
            callPatch_onFailure();
        }
    }

    public synchronized JSONObject getSdkState() throws JSONException {
        return JSONUtil.deepCopy(sdkState);
    }

    private synchronized void save() {
        try {
            JSONObject state = new JSONObject();
            state.put(SAVED_STATE_FIELD__SYNC_STATE_VERSION,      SAVED_STATE_STATE_VERSION_2);
            state.put(SAVED_STATE_FIELD_UPGRADE_META,             upgradeMeta);
            state.put(SAVED_STATE_FIELD_SDK_STATE,                sdkState);
            state.put(SAVED_STATE_FIELD_SERVER_STATE,             serverState);
            state.put(SAVED_STATE_FIELD_PUT_ACCUMULATOR,          putAccumulator);
            state.put(SAVED_STATE_FIELD_INFLIGHT_DIFF,            inflightDiff);
            state.put(SAVED_STATE_FIELD_INFLIGHT_PUT_ACCUMULATOR, inflightPutAccumulator);
            state.put(SAVED_STATE_FIELD_SCHEDULED_PATCH_CALL,     scheduledPatchCall);
            state.put(SAVED_STATE_FIELD_INFLIGHT_PATCH_CALL,      inflightPatchCall);
            persist(state);
        } catch (JSONException ex) {
            WonderPush.logError("Failed to build state object for saving installation custom for " + this, ex);
        }
    }

    private synchronized void applyUpgrade() {
        upgrade(upgradeMeta, sdkState, serverState,putAccumulator, inflightDiff, inflightPutAccumulator);
    }

    public synchronized void put(JSONObject diff) throws JSONException {
        if (diff == null) diff = new JSONObject();
        JSONUtil.merge(sdkState, diff);
        JSONUtil.merge(putAccumulator, diff, false);
        WonderPush.safeDefer(() -> {
            schedulePatchCallAndSave();
        }, 0);
    }

    public synchronized void receiveServerState(JSONObject srvState) throws JSONException {
        if (srvState == null) srvState = new JSONObject();
        serverState = JSONUtil.deepCopy(srvState);
        JSONUtil.stripNulls(serverState);
        schedulePatchCallAndSave();
    }

    public synchronized void receiveState(JSONObject receivedState, boolean resetSdkState) throws JSONException {
        if (receivedState == null) receivedState = new JSONObject();
        serverState = JSONUtil.deepCopy(receivedState);
        JSONUtil.stripNulls(serverState);
        sdkState = JSONUtil.deepCopy(serverState);
        if (resetSdkState) {
            putAccumulator = new JSONObject();
        } else {
            JSONUtil.merge(sdkState, inflightDiff);
            JSONUtil.merge(sdkState, putAccumulator);
        }
        schedulePatchCallAndSave();
    }

    public synchronized void receiveDiff(JSONObject diff) throws JSONException {
        if (diff == null) diff = new JSONObject();
        // The diff is already server-side, by contract
        JSONUtil.merge(serverState, diff);
        put(diff);
    }

    private synchronized void schedulePatchCallAndSave() {
        scheduledPatchCall = true;
        save();
        schedulePatchCall();
    }

    synchronized boolean hasScheduledPatchCall() {
        return scheduledPatchCall;
    }

    synchronized boolean hasInflightPatchCall() {
        return inflightPatchCall;
    }

    synchronized boolean performScheduledPatchCall() {
        if (scheduledPatchCall) {
            callPatch();
            return true;
        }
        return false;
    }

    private synchronized void callPatch() {
        if (inflightPatchCall) {
            if (!scheduledPatchCall) {
                WonderPush.logDebug("Server PATCH call already inflight, scheduling a new one");
                schedulePatchCallAndSave();
            } else {
                WonderPush.logDebug("Server PATCH call already inflight, and already scheduled");
            }
            save();
            return;
        }
        scheduledPatchCall = false;

        try {
            inflightDiff = JSONUtil.diff(serverState, sdkState);
        } catch (JSONException ex) {
            WonderPush.logError("Failed to diff server state and sdk state to send installation custom diff", ex);
            inflightDiff = new JSONObject();
        }
        if (inflightDiff.length() == 0) {
            WonderPush.logDebug("No diff to send to server");
            save();
            return;
        }
        inflightPatchCall = true;

        try {
            inflightPutAccumulator = JSONUtil.deepCopy(putAccumulator);
        } catch (JSONException e) {
            inflightPutAccumulator = putAccumulator;
        }
        putAccumulator = new JSONObject();

        save();
        serverPatchInstallation(inflightDiff, new ResponseHandler() {
            @Override
            public void onSuccess() {
                callPatch_onSuccess();
            }

            @Override
            public void onFailure() {
                callPatch_onFailure();
            }
        });
    }

    private synchronized void callPatch_onSuccess() {
        inflightPatchCall = false;
        inflightPutAccumulator = new JSONObject();
        try {
            JSONUtil.merge(serverState, inflightDiff);
            inflightDiff = new JSONObject();
        } catch (JSONException ex) {
            WonderPush.logError("Failed to copy putAccumulator", ex);
        }
        save();
    }

    private synchronized void callPatch_onFailure() {
        inflightPatchCall = false;
        try {
            JSONUtil.merge(inflightPutAccumulator, putAccumulator, false);
        } catch (JSONException ex) {
            WonderPush.logError("Failed to merge putAccumulator into oldPutAccumulator", ex);
        }
        putAccumulator = inflightPutAccumulator;
        inflightPutAccumulator = new JSONObject();
        schedulePatchCallAndSave();
    }

    void upgrade(JSONObject upgradeMeta, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, JSONObject inflightDiff, JSONObject inflightPutAccumulator) {
        long currentVersion = upgradeMeta.optLong(UPGRADE_META_VERSION_KEY, 0);

        // Do not alter latest, or future versions we don't understand
        if (currentVersion >= UPGRADE_META_VERSION_LATEST) {
            return;
        }

        // VERSION 1: puts everything inside a key called "custom".
        if (currentVersion < UPGRADE_META_VERSION_1_IMPORTED_CUSTOM) {
            try {
                moveInsideCustom(sdkState);
                moveInsideCustom(serverState);
                moveInsideCustom(putAccumulator);
                moveInsideCustom(inflightDiff);
                moveInsideCustom(inflightPutAccumulator);
            } catch (JSONException e) {
                Log.e(WonderPush.TAG, "Could not upgrade custom properties", e);
            }
        }

        // Set to the latest version
        try {
            upgradeMeta.put(UPGRADE_META_VERSION_KEY, UPGRADE_META_VERSION_LATEST);
        } catch (JSONException e) {
            Log.e(WonderPush.TAG, "Could not set json sync version", e);
        }

    }

    private void moveInsideCustom(JSONObject input) throws JSONException {
        JSONObject tmp = new JSONObject(input.toString());
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = input.keys();
        while(iterator.hasNext()) keys.add(iterator.next());
        for (String key : keys) input.remove(key);
        input.put("custom", tmp);
    }

    @Override
    public synchronized String toString() {
        return "JSONSync"
                + "{sdkState:" + sdkState
                + ",serverState:" + serverState
                + ",putAccumulator:" + putAccumulator
                + ",inflightDiff:" + inflightDiff
                + ",inflightPutAccumulator:" + inflightPutAccumulator
                + ",upgradeMeta:" + upgradeMeta
                + ",scheduledPatchCall:" + scheduledPatchCall
                + ",inflightPatchCall:" + inflightPatchCall
                + "}";
    }
}
