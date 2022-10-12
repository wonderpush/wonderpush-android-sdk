package com.wonderpush.sdk;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class JSONSyncInstallation extends JSONSync {

    private static final Map<String, JSONSyncInstallation> sInstancePerUserId = new HashMap<>();
    private static boolean initialized = false;

    private final String userId;
    private long firstDelayedWriteDate;
    private static boolean disabled = false;

    private ScheduledFuture<Void> scheduledPatchCallDelayedTask;

    public static void initialize() {
        synchronized (sInstancePerUserId) {
            if (initialized) {
                return;
            }
            initialized = true;

            JSONObject installationCustomSyncStatePerUserId = WonderPushConfiguration.getInstallationCustomSyncStatePerUserId();
            if (installationCustomSyncStatePerUserId == null) installationCustomSyncStatePerUserId = new JSONObject();
            Iterator<String> it = installationCustomSyncStatePerUserId.keys();
            while (it.hasNext()) {
                String userId = it.next();
                JSONObject state = installationCustomSyncStatePerUserId.optJSONObject(userId);
                if (userId != null && userId.length() == 0) userId = null;
                JSONSyncInstallation sync;
                try {
                    sync = new JSONSyncInstallation(userId, state);
                } catch (JSONException e) {
                    Log.e(WonderPush.TAG, "Failed to restore installation custom from saved state for user " + userId + " and state " + (state != null ? state : "null"), e);
                    sync = new JSONSyncInstallation(userId);
                }
                sInstancePerUserId.put(userId, sync);
            }
            String oldUserId = WonderPushConfiguration.getUserId();
            try {
                for (String userId : WonderPushConfiguration.listKnownUserIds()) {
                    if (!sInstancePerUserId.containsKey(userId)) {
                        WonderPushConfiguration.changeUserId(userId);
                        sInstancePerUserId.put(userId, new JSONSyncInstallation(
                                userId,
                                WonderPushConfiguration.getCachedInstallationCustomPropertiesUpdated(),
                                WonderPushConfiguration.getCachedInstallationCustomPropertiesWritten()
                        ));
                    }
                }
            } catch (Exception ex) {
                Log.e(WonderPush.TAG, "Unexpected error while initializing installation customs", ex);
            } finally {
                WonderPushConfiguration.changeUserId(oldUserId);
            }

            // Adding the listener here will catch the an initial call triggered after this function is called, all during SDK initialization.
            // It also flushes any scheduled call that was dropped when the user withdrew consent.
            WonderPush.addUserConsentListener(hasUserConsent -> {
                if (hasUserConsent) {
                    flushAll();
                }
            });
        }
    }

    public static JSONSyncInstallation forCurrentUser() {
        return forUser(WonderPushConfiguration.getUserId());
    }

    static JSONSyncInstallation forUser(String userId) {
        if (userId != null && userId.length() == 0) userId = null;
        synchronized (sInstancePerUserId) {
            JSONSyncInstallation rtn = sInstancePerUserId.get(userId);
            if (rtn == null) {
                try {
                    rtn = new JSONSyncInstallation(userId, null);
                } catch (JSONException e) {
                    Log.e(WonderPush.TAG, "Failed to restore installation custom from saved state for user " + userId + " and null state", e);
                    rtn = new JSONSyncInstallation(userId);
                }
                sInstancePerUserId.put(userId, rtn);
            }
            return rtn;
        }
    }

    public static void flushAll() {
        WonderPush.logDebug("Flushing delayed updates of custom properties for all known users");
        synchronized (sInstancePerUserId) {
            for (JSONSyncInstallation sync : sInstancePerUserId.values()) {
                sync.flush();
            }
        }
    }

    public static synchronized void setDisabled(boolean disabled) {
        JSONSyncInstallation.disabled = disabled;
    }

    public static synchronized boolean isDisabled() {
        return disabled;
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

    private JSONSyncInstallation(String userId) {
        super();
        if (userId != null && userId.length() == 0) userId = null;
        this.userId = userId;
    }

    private JSONSyncInstallation(String userId, JSONObject sdkState, JSONObject serverState) {
        super(sdkState, serverState, _initDiffServerAndSdkState(serverState, sdkState), null, null, null, true /*schedule a patch call*/, false);
        if (userId != null && userId.length() == 0) userId = null;
        this.userId = userId;
    }

    private JSONSyncInstallation(String userId, JSONObject savedState) throws JSONException {
        super(
                savedState != null && savedState.has(SAVED_STATE_FIELD_SDK_STATE)                ? savedState.getJSONObject(SAVED_STATE_FIELD_SDK_STATE)                : null,
                savedState != null && savedState.has(SAVED_STATE_FIELD_SERVER_STATE)             ? savedState.getJSONObject(SAVED_STATE_FIELD_SERVER_STATE)             : null,
                savedState != null && savedState.has(SAVED_STATE_FIELD_PUT_ACCUMULATOR)          ? savedState.getJSONObject(SAVED_STATE_FIELD_PUT_ACCUMULATOR)          : null,
                savedState != null && savedState.has(SAVED_STATE_FIELD_INFLIGHT_DIFF)            ? savedState.getJSONObject(SAVED_STATE_FIELD_INFLIGHT_DIFF)            : null,
                savedState != null && savedState.has(SAVED_STATE_FIELD_INFLIGHT_PUT_ACCUMULATOR) ? savedState.getJSONObject(SAVED_STATE_FIELD_INFLIGHT_PUT_ACCUMULATOR) : null,
                savedState != null && savedState.has(SAVED_STATE_FIELD_UPGRADE_META)             ? savedState.getJSONObject(SAVED_STATE_FIELD_UPGRADE_META)             : null,
                savedState != null && savedState.has(SAVED_STATE_FIELD_SCHEDULED_PATCH_CALL)     ? savedState.getBoolean(SAVED_STATE_FIELD_SCHEDULED_PATCH_CALL)        : true,
                savedState != null && savedState.has(SAVED_STATE_FIELD_INFLIGHT_PATCH_CALL)      ? savedState.getBoolean(SAVED_STATE_FIELD_INFLIGHT_PATCH_CALL)         : false
        );
        if (userId != null && userId.length() == 0) userId = null;
        this.userId = userId;
    }

    synchronized void flush() {
        if (scheduledPatchCallDelayedTask != null) {
            scheduledPatchCallDelayedTask.cancel(false);
        }
        WonderPush.safeDefer(() -> {
            performScheduledPatchCall();
        }, 0);
    }

    @Override
    protected void persist(JSONObject state) {
        String key = userId == null ? "" : userId;
        JSONObject installationCustomSyncStatePerUserId = WonderPushConfiguration.getInstallationCustomSyncStatePerUserId();
        if (installationCustomSyncStatePerUserId == null) installationCustomSyncStatePerUserId = new JSONObject();
        if (state == null) state = new JSONObject();
        try {
            installationCustomSyncStatePerUserId.put(key, state);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to save installation custom sync state for user " + userId + " and value " + state, ex);
        }
        WonderPushConfiguration.setInstallationCustomSyncStatePerUserId(installationCustomSyncStatePerUserId);
    }

    @Override
    protected void schedulePatchCall() {
        WonderPush.logDebug("Scheduling patch call for installation custom state for userId " + userId);
        if (scheduledPatchCallDelayedTask != null) {
            scheduledPatchCallDelayedTask.cancel(false);
        }
        long nowRT = SystemClock.elapsedRealtime();
        if (firstDelayedWriteDate == 0) firstDelayedWriteDate = nowRT;
        if (!WonderPush.hasUserConsent()) {
            WonderPush.logDebug("Delaying scheduled patch call until user consent is provided for installation custom state for userId " + userId);
            WonderPush.addUserConsentListener(new WonderPush.UserConsentListener() {
                @Override
                public void onUserConsentChanged(boolean hasUserConsent) {
                    if (hasUserConsent) {
                        try {
                            WonderPush.removeUserConsentListener(this);
                            WonderPush.logDebug("Now scheduling user consent delayed patch call for installation custom state for userId " + userId);
                            schedulePatchCall(); // NOTE: imposes this function to be somewhat reentrant
                        } catch (Exception ex) {
                            Log.e(WonderPush.TAG, "Unexpected error on user consent changed.", ex);
                        }
                    }
                }
            });
            return;
        }
        scheduledPatchCallDelayedTask = WonderPush.sScheduledExecutor.schedule(
                () -> {
                    try {
                        performScheduledPatchCall();
                    } catch (Exception ex) {
                        Log.e(WonderPush.TAG, "Unexpected error on scheduled task", ex);
                    }
                    return null;
                }, Math.min(InstallationManager.CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY,
                        firstDelayedWriteDate + InstallationManager.CACHED_INSTALLATION_CUSTOM_PROPERTIES_MAX_DELAY - nowRT), TimeUnit.MILLISECONDS);
    }

    synchronized boolean performScheduledPatchCall() {
        if (!WonderPush.hasUserConsent()) {
            WonderPush.logDebug("Need consent, not performing scheduled patch call for user " + userId);
            return false;
        }
        firstDelayedWriteDate = 0;
        return super.performScheduledPatchCall();
    }

    @Override
    protected void serverPatchInstallation(JSONObject diff, JSONSync.ResponseHandler handler) {
        if (!WonderPush.hasUserConsent()) {
            WonderPush.logDebug("Need consent, not sending installation custom diff " + diff + " for user " + userId);
            handler.onFailure();
            return;
        }
        if (isDisabled()) {
            WonderPush.logDebug("JsonSync PATCH calls disabled");
            handler.onFailure();
            return;
        }
        WonderPush.logDebug("Sending installation custom diff " + diff + " for user " + userId);
        Request.Params parameters = new Request.Params();
        parameters.put("body", diff.toString());
        ApiClient.getInstance().requestForUser(userId, HttpMethod.PATCH, "/installation", parameters, new com.wonderpush.sdk.ResponseHandler() {
            @Override
            public void onFailure(Throwable ex, Response errorResponse) {

                synchronized (JSONSyncInstallation.this) {
                    Log.e(WonderPush.TAG, "Failed to send installation custom diff, got " + errorResponse, ex);
                    handler.onFailure();
                }
            }

            @Override
            public void onSuccess(Response response) {
                synchronized (JSONSyncInstallation.this) {
                    try {
                        if (response.isError() || !response.getJSONObject().has("success") || !response.getJSONObject().getBoolean("success")) {
                            Log.e(WonderPush.TAG, "Failed to send installation custom diff, got " + response);
                            handler.onFailure();
                        } else {
                            WonderPush.logDebug("Succeeded to send diff for user " + userId + ": " + diff);
                            handler.onSuccess();
                        }
                    } catch (JSONException ex) {
                        Log.e(WonderPush.TAG, "Failed to read success field from response " + response, ex);
                        handler.onFailure();
                    }
                }
            }
        });
    }

    @Override
    public synchronized String toString() {
        return "JSONSyncInstallationCustom"
                + "{userId=" + userId
                + ",sync=" + super.toString()
                + "}";
    }

}
