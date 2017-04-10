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
    private JSONObject srvPutAcc;
    private boolean scheduledPatchCall;
    private boolean inflightPatchCall;

    Sync() {
        this(null, null, null, null, false);
    }

    public Sync(JSONObject sdkState, JSONObject srvState, JSONObject sdkPutAcc, JSONObject srvPutAcc) {
        this(sdkState, srvState, sdkPutAcc, srvPutAcc, false);
    }

    Sync(JSONObject sdkState, JSONObject srvState, JSONObject sdkPutAcc, JSONObject srvPutAcc, boolean scheduledPatchCall) {
        if (sdkState == null) sdkState = new JSONObject();
        if (srvState == null) srvState = new JSONObject();
        if (sdkPutAcc == null) sdkPutAcc = new JSONObject();
        if (srvPutAcc == null) srvPutAcc = new JSONObject();
        this.sdkState = sdkState;
        this.srvState = srvState;
        this.sdkPutAcc = sdkPutAcc;
        this.srvPutAcc = srvPutAcc;
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
        JSONUtil.merge(srvPutAcc, diff);
        schedulePatchCallAndSave();
    }

    public synchronized void recvState(JSONObject receivedState, boolean resetSdkState) throws JSONException {
        srvState = JSONUtil.deepCopy(receivedState);
        if (resetSdkState) {
            sdkPutAcc = new JSONObject();
            srvPutAcc = new JSONObject();
        }
        sdkState = JSONUtil.deepCopy(srvState);
        JSONUtil.merge(sdkState, srvPutAcc);
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

        final JSONObject diff = JSONUtil.diff(srvState, sdkState);
        if (diff.length() == 0) {
            return;
        }
        inflightPatchCall = true;

        final JSONObject oldSdkPutAcc = sdkPutAcc;
        sdkPutAcc = new JSONObject();

        server.patchInstallation(diff, new ResponseHandler() {
            @Override
            public void onSuccess() {
                synchronized (Sync.this) {
                    inflightPatchCall = false;
                    try {
                        JSONUtil.merge(srvState, diff);
                        srvPutAcc = JSONUtil.deepCopy(sdkPutAcc);
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
                + ",srvPutAcc:" + srvPutAcc
                + ",scheduledPatchCall:" + scheduledPatchCall
                + ",inflightPatchCall:" + inflightPatchCall
                + ">";
    }
}
