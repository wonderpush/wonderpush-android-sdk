package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

class JSONSync {

    interface ResponseHandler {
        void onSuccess();
        void onFailure();
    }

    interface Callbacks {
        void save(JSONObject state);
        void schedulePatchCall();
        void serverPatchInstallation(JSONObject diff, ResponseHandler handler);
    }

    private Callbacks callbacks;
    private JSONObject sdkState;
    private JSONObject serverState;
    private JSONObject putAccumulator;
    private JSONObject inflightDiff;
    private JSONObject inflightPutAccumulator;
    private boolean scheduledPatchCall;
    private boolean inflightPatchCall;

    JSONSync(Callbacks callbacks) {
        this(callbacks, null, null, null, false);
    }

    public JSONSync(Callbacks callbacks, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator) {
        this(callbacks, sdkState, serverState, putAccumulator, false);
    }

    JSONSync(Callbacks callbacks, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, boolean scheduledPatchCall) {
        if (callbacks == null) throw new NullPointerException("callbacks cannot be null");
        this.callbacks = callbacks;
        if (sdkState == null) sdkState = new JSONObject();
        if (serverState == null) serverState = new JSONObject();
        if (putAccumulator == null) putAccumulator = new JSONObject();
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
        this.scheduledPatchCall = scheduledPatchCall;
    }

    public JSONObject getSdkState() throws JSONException {
        return JSONUtil.deepCopy(sdkState);
    }

    private synchronized void save() {
        JSONObject state = new JSONObject();
        // TODO
        callbacks.save(state);
    }

    public synchronized void put(JSONObject diff) throws JSONException {
        JSONUtil.merge(sdkState, diff);
        JSONUtil.merge(putAccumulator, diff, false);
        schedulePatchCallAndSave();
    }

    public synchronized void receiveServerState(JSONObject srvState) throws JSONException {
        JSONUtil.stripNulls(srvState);
        serverState = JSONUtil.deepCopy(srvState);
        schedulePatchCallAndSave();
    }

    public synchronized void receiveState(JSONObject receivedState, boolean resetSdkState) throws JSONException {
        JSONUtil.stripNulls(receivedState);
        serverState = JSONUtil.deepCopy(receivedState);
        sdkState = JSONUtil.deepCopy(serverState);
        if (resetSdkState) {
            putAccumulator = new JSONObject();
        } else {
            JSONUtil.merge(sdkState, putAccumulator);
            JSONUtil.merge(sdkState, inflightDiff);
        }
        schedulePatchCallAndSave();
    }

    public synchronized void receiveDiff(JSONObject diff) throws JSONException {
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

    synchronized boolean performScheduledPatchCall() throws JSONException {
        if (scheduledPatchCall) {
            callPatch();
            return true;
        }
        return false;
    }

    private synchronized void callPatch() throws JSONException {
        if (inflightPatchCall) {
            if (!scheduledPatchCall) {
                schedulePatchCallAndSave();
            }
            return;
        }
        scheduledPatchCall = false;

        inflightDiff = JSONUtil.diff(serverState, sdkState);
        if (inflightDiff.length() == 0) {
            return;
        }
        inflightPatchCall = true;

        inflightPutAccumulator = putAccumulator;
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
        return this.getClass().getSimpleName()
                + "<sdkState:" + sdkState
                + ",serverState:" + serverState
                + ",putAccumulator:" + putAccumulator
                + ",inflightDiff:" + inflightDiff
                + ",inflightPutAccumulator:" + inflightPutAccumulator
                + ",scheduledPatchCall:" + scheduledPatchCall
                + ",inflightPatchCall:" + inflightPatchCall
                + ">";
    }
}
