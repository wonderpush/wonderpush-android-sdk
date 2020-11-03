package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class JSONSync {

    interface ResponseHandler {
        void onSuccess();
        void onFailure();
    }

    interface Callbacks {
        void save(JSONObject state);
        void schedulePatchCall();
        void serverPatchInstallation(JSONObject diff, ResponseHandler handler);
        void upgrade(JSONObject upgradeMeta, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, JSONObject inflightDiff, JSONObject inflightPutAccumulator);
    }

    private static final int SAVED_STATE_STATE_VERSION_1 = 1;
    private static final int SAVED_STATE_STATE_VERSION_2 = 2;
    private static final String SAVED_STATE_FIELD__SYNC_STATE_VERSION = "_syncStateVersion";
    private static final String SAVED_STATE_FIELD_UPGRADE_META = "upgradeMeta";
    private static final String SAVED_STATE_FIELD_SDK_STATE = "sdkState";
    private static final String SAVED_STATE_FIELD_SERVER_STATE = "serverState";
    private static final String SAVED_STATE_FIELD_PUT_ACCUMULATOR = "putAccumulator";
    private static final String SAVED_STATE_FIELD_INFLIGHT_DIFF = "inflightDiff";
    private static final String SAVED_STATE_FIELD_INFLIGHT_PUT_ACCUMULATOR = "inflightPutAccumulator";
    private static final String SAVED_STATE_FIELD_SCHEDULED_PATCH_CALL = "scheduledPatchCall";
    private static final String SAVED_STATE_FIELD_INFLIGHT_PATCH_CALL = "inflightPatchCall";

    private Callbacks callbacks;
    private JSONObject sdkState;
    private JSONObject serverState;
    private JSONObject putAccumulator;
    private JSONObject inflightDiff;
    private JSONObject inflightPutAccumulator;
    private JSONObject upgradeMeta;
    private boolean scheduledPatchCall;
    private boolean inflightPatchCall;

    JSONSync(Callbacks callbacks) {
        this(callbacks, null, null, null, null, null, null, false, false);
    }

    private static JSONObject _initDiffServerAndSdkState(JSONObject serverState, JSONObject sdkState) {
        if (serverState == null) serverState = new JSONObject();
        if (sdkState == null) sdkState = new JSONObject();
        JSONObject diff;
        try {
            diff = JSONUtil.diff(serverState, sdkState);
        } catch (JSONException ex) {
            WonderPush.logError("Error while diffing serverState " + serverState + " with sdkState " + sdkState + ". Falling back to full sdkState", ex);
            try {
                diff = JSONUtil.deepCopy(sdkState);
            } catch (JSONException ex2) {
                WonderPush.logError("Error while cloning sdkState " + sdkState + ". Falling back to empty diff", ex2);
                diff = new JSONObject();
            }
        }
        return diff;
    }

    static JSONSync fromSdkStateAndServerState(Callbacks callbacks, JSONObject sdkState, JSONObject serverState) {
        return new JSONSync(callbacks, sdkState, serverState, _initDiffServerAndSdkState(serverState, sdkState), null, null, null, true /*schedule a patch call*/, false);
    }

    static JSONSync fromSavedState(Callbacks callbacks, JSONObject savedState) throws JSONException {
        if (savedState == null) savedState = new JSONObject();
        return new JSONSync(
                callbacks,
                savedState.has(SAVED_STATE_FIELD_SDK_STATE)                ? savedState.getJSONObject(SAVED_STATE_FIELD_SDK_STATE)                : null,
                savedState.has(SAVED_STATE_FIELD_SERVER_STATE)             ? savedState.getJSONObject(SAVED_STATE_FIELD_SERVER_STATE)             : null,
                savedState.has(SAVED_STATE_FIELD_PUT_ACCUMULATOR)          ? savedState.getJSONObject(SAVED_STATE_FIELD_PUT_ACCUMULATOR)          : null,
                savedState.has(SAVED_STATE_FIELD_INFLIGHT_DIFF)            ? savedState.getJSONObject(SAVED_STATE_FIELD_INFLIGHT_DIFF)            : null,
                savedState.has(SAVED_STATE_FIELD_INFLIGHT_PUT_ACCUMULATOR) ? savedState.getJSONObject(SAVED_STATE_FIELD_INFLIGHT_PUT_ACCUMULATOR) : null,
                savedState.has(SAVED_STATE_FIELD_UPGRADE_META)             ? savedState.getJSONObject(SAVED_STATE_FIELD_UPGRADE_META)             : null,
                savedState.has(SAVED_STATE_FIELD_SCHEDULED_PATCH_CALL)     ? savedState.getBoolean(SAVED_STATE_FIELD_SCHEDULED_PATCH_CALL)        : true,
                savedState.has(SAVED_STATE_FIELD_INFLIGHT_PATCH_CALL)      ? savedState.getBoolean(SAVED_STATE_FIELD_INFLIGHT_PATCH_CALL)         : false
        );
    }

    JSONSync(Callbacks callbacks, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, JSONObject inflightDiff, JSONObject inflightPutAccumulator, JSONObject upgradeMeta, boolean scheduledPatchCall, boolean inflightPatchCall) {
        if (callbacks == null) throw new NullPointerException("callbacks cannot be null");
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

        this.callbacks = callbacks;
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
            callbacks.save(state);
        } catch (JSONException ex) {
            WonderPush.logError("Failed to build state object for saving installation custom for " + this, ex);
        }
    }

    private synchronized void applyUpgrade() {
        if (callbacks == null) return;
        callbacks.upgrade(upgradeMeta, sdkState, serverState,putAccumulator, inflightDiff, inflightPutAccumulator);
    }

    public synchronized void put(JSONObject diff) throws JSONException {
        if (diff == null) diff = new JSONObject();
        JSONUtil.merge(sdkState, diff);
        JSONUtil.merge(putAccumulator, diff, false);
        schedulePatchCallAndSave();
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
        callbacks.schedulePatchCall();
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
        callbacks.serverPatchInstallation(inflightDiff, new ResponseHandler() {
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
