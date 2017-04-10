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
    private JSONObject srvState;
    private JSONObject sdkPutAcc;
    private JSONObject inflightDiff;
    private boolean scheduledPatchCall;
    private boolean inflightPatchCall;

    Sync() {
        this(null, null, null, false);
    }

    public Sync(JSONObject sdkState, JSONObject srvState, JSONObject sdkPutAcc) {
        this(sdkState, srvState, sdkPutAcc, false);
    }

    Sync(JSONObject sdkState, JSONObject srvState, JSONObject sdkPutAcc, boolean scheduledPatchCall) {
        if (sdkState == null) sdkState = new JSONObject();
        if (srvState == null) srvState = new JSONObject();
        if (sdkPutAcc == null) sdkPutAcc = new JSONObject();
        this.sdkState = sdkState;
        this.srvState = srvState;
        this.sdkPutAcc = sdkPutAcc;
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
        JSONUtil.merge(sdkPutAcc, diff);
        schedulePatchCallAndSave();
    }

    public synchronized void recvState(JSONObject receivedState, boolean resetSdkState) throws JSONException {
        srvState = JSONUtil.deepCopy(receivedState);
        sdkState = JSONUtil.deepCopy(srvState);
        if (resetSdkState) {
            sdkPutAcc = new JSONObject();
        } else {
            JSONUtil.merge(sdkState, sdkPutAcc);
            JSONUtil.merge(sdkState, inflightDiff);
        }
        schedulePatchCallAndSave(); // FIXME do right away, no schedule?
    }

    public synchronized void recvDiff(JSONObject diff) throws JSONException {
        // The diff is already server-side, by contract
        JSONUtil.merge(srvState, diff);
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
            scheduledPatchCall = true;
            return;
        }
        scheduledPatchCall = false;

        inflightDiff = JSONUtil.diff(srvState, sdkState);
        if (inflightDiff.length() == 0) {
            return;
        }
        inflightPatchCall = true;

        final JSONObject oldSdkPutAcc = sdkPutAcc;
        sdkPutAcc = new JSONObject();

        server.patchInstallation(inflightDiff, new ResponseHandler() {
            @Override
            public void onSuccess() {
                synchronized (Sync.this) {
                    inflightPatchCall = false;
                    try {
                        JSONUtil.merge(srvState, inflightDiff);
                        inflightDiff = new JSONObject();
                    } catch (JSONException ex) {
                        WonderPush.logError("Failed to copy sdkPutAcc", ex);
                    }
                    schedulePatchCallAndSave();
                }
            }

            @Override
            public void onFailure() {
                synchronized (Sync.this) {
                    inflightPatchCall = false;
                    try {
                        JSONUtil.merge(oldSdkPutAcc, sdkPutAcc);
                    } catch (JSONException ex) {
                        WonderPush.logError("Failed to merge sdkPutAcc into oldSdkPutAcc", ex);
                    }
                    sdkPutAcc = oldSdkPutAcc;
                    schedulePatchCallAndSave();
                }
            }
        });
    }

    @Override
    public synchronized String toString() {
        return this.getClass().getSimpleName()
                + "<sdkState:" + sdkState
                + ",srvState:" + srvState
                + ",sdkPutAcc:" + sdkPutAcc
                + ",inflightDiff:" + inflightDiff
                + ",scheduledPatchCall:" + scheduledPatchCall
                + ",inflightPatchCall:" + inflightPatchCall
                + ">";
    }
}
