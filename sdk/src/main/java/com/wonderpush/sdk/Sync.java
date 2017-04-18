package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

class Sync {

    interface ResponseHandler {
        void onSuccess();
        void onFailure();
    }

    interface Server {
        void patchInstallation(JSONObject diff, ResponseHandler handler);
    }

    private JSONObject sdkState;
    private JSONObject serverState;
    private JSONObject putAccumulator;
    private JSONObject inflightDiff;
    private JSONObject inflightPutAccumulator;
    private boolean scheduledPatchCall;
    private boolean inflightPatchCall;

    Sync() {
        this(null, null, null, false);
    }

    public Sync(JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator) {
        this(sdkState, serverState, putAccumulator, false);
    }

    Sync(JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, boolean scheduledPatchCall) {
        if (sdkState == null) sdkState = new JSONObject();
        if (serverState == null) serverState = new JSONObject();
        if (putAccumulator == null) putAccumulator = new JSONObject();
        this.sdkState = sdkState;
        this.serverState = serverState;
        this.putAccumulator = putAccumulator;
        this.scheduledPatchCall = scheduledPatchCall;
    }

    public JSONObject getSdkState() throws JSONException {
        return JSONUtil.deepCopy(sdkState);
    }

    private synchronized void save() {
        // TODO
    }

    public synchronized void put(JSONObject diff) throws JSONException {
        JSONUtil.merge(sdkState, diff);
        JSONUtil.merge(putAccumulator, diff, false);
        schedulePatchCallAndSave();
    }

    public synchronized void recvSrvState(JSONObject srvState) throws JSONException {
        serverState = JSONUtil.deepCopy(srvState);
        schedulePatchCallAndSave();
    }

    public synchronized void recvState(JSONObject receivedState, boolean resetSdkState) throws JSONException {
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

    public synchronized void recvDiff(JSONObject diff) throws JSONException {
        // The diff is already server-side, by contract
        JSONUtil.merge(serverState, diff);
        put(diff);
    }

    private synchronized void schedulePatchCallAndSave() {
        scheduledPatchCall = true;
        save();
        // TODO schedule
    }

    synchronized boolean hasScheduledPatchCall() {
        return scheduledPatchCall;
    }

    synchronized boolean hasInflightPatchCall() {
        return inflightPatchCall;
    }

    synchronized boolean performScheduledPatchCall(Server server) throws JSONException {
        if (scheduledPatchCall) {
            callPatch(server);
            return true;
        }
        return false;
    }

    private synchronized void callPatch(Server server) throws JSONException {
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
        server.patchInstallation(inflightDiff, new ResponseHandler() {
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
