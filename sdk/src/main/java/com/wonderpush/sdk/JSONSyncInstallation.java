package com.wonderpush.sdk;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class JSONSyncInstallation {

    private static final String UPGRADE_META_VERSION_KEY = "version";
    private static final Long UPGRADE_META_VERSION_0_INITIAL = 0l;
    private static final Long UPGRADE_META_VERSION_1_IMPORTED_CUSTOM = 1l;
    private static final Long UPGRADE_META_VERSION_LATEST = UPGRADE_META_VERSION_1_IMPORTED_CUSTOM;

    private static final Map<String, JSONSyncInstallation> sInstancePerUserId = new HashMap<>();
    private static boolean initialized = false;

    private final String userId;
    private final JSONSync sync;
    private long firstDelayedWriteDate;

    private static ScheduledFuture<Void> scheduledPatchCallDelayedTask;

    private class Callbacks implements JSONSync.Callbacks {
        @Override
        public void save(JSONObject state) {
            _save(state);
        }

        @Override
        public void schedulePatchCall() {
            _schedulePatchCall();
        }

        @Override
        public void serverPatchInstallation(JSONObject diff, JSONSync.ResponseHandler handler) {
            _serverPatchInstallation(diff, handler);
        }

        @Override
        public void upgrade(JSONObject upgradeMeta, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, JSONObject inflightDiff, JSONObject inflightPutAccumulator) {
            Long currentVersion = upgradeMeta.optLong(UPGRADE_META_VERSION_KEY, 0);

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
    }

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
                sInstancePerUserId.put(userId, new JSONSyncInstallation(userId, state));
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
            WonderPush.addUserConsentListener(new WonderPush.UserConsentListener() {
                @Override
                public void onUserConsentChanged(boolean hasUserConsent) {
                    if (hasUserConsent) {
                        flushAll();
                    }
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
                rtn = new JSONSyncInstallation(userId, null);
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

    private JSONSyncInstallation(String userId, JSONObject sdkState, JSONObject serverState) {
        if (userId != null && userId.length() == 0) userId = null;
        this.userId = userId;

        sync = JSONSync.fromSdkStateAndServerState(new Callbacks(), sdkState, serverState);
    }

    private JSONSyncInstallation(String userId, JSONObject savedState) {
        if (userId != null && userId.length() == 0) userId = null;
        this.userId = userId;

        JSONSync sync;
        try {
            sync = JSONSync.fromSavedState(new Callbacks(), savedState);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to restore installation custom from saved state for user " + userId + " and state " + savedState, ex);
            sync = new JSONSync(new Callbacks());
        }
        this.sync = sync;
    }

    synchronized void flush() {
        if (scheduledPatchCallDelayedTask != null) {
            scheduledPatchCallDelayedTask.cancel(false);
            scheduledPatchCallDelayedTask = null;
        }
        _performScheduledPatchCall();
    }

    private synchronized void _save(JSONObject state) {
        WonderPush.logDebug("Saving installation custom state for userId " + userId + ": " + state);
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

    private synchronized void _schedulePatchCall() {
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
                        WonderPush.removeUserConsentListener(this);
                        WonderPush.logDebug("Now scheduling user consent delayed patch call for installation custom state for userId " + userId);
                        _schedulePatchCall(); // NOTE: imposes this function to be somewhat reentrant
                    }
                }
            });
            return;
        }
        scheduledPatchCallDelayedTask = WonderPush.sScheduledExecutor.schedule(
                new Callable<Void>() {
                    @Override
                    public Void call() {
                        try {
                            _performScheduledPatchCall();
                        } catch (Exception ex) {
                            Log.e(WonderPush.TAG, "Unexpected error on scheduled task", ex);
                        }
                        return null;
                    }
                },
                Math.min(InstallationManager.CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY,
                        firstDelayedWriteDate + InstallationManager.CACHED_INSTALLATION_CUSTOM_PROPERTIES_MAX_DELAY - nowRT),
                TimeUnit.MILLISECONDS);
    }

    private synchronized void _performScheduledPatchCall() {
        if (!WonderPush.hasUserConsent()) {
            WonderPush.logDebug("Need consent, not performing scheduled patch call for user " + userId);
            return;
        }
        firstDelayedWriteDate = 0;
        sync.performScheduledPatchCall();
    }

    private synchronized void _serverPatchInstallation(final JSONObject diff, final JSONSync.ResponseHandler handler) {
        if (!WonderPush.hasUserConsent()) {
            WonderPush.logDebug("Need consent, not sending installation custom diff " + diff + " for user " + userId);
            handler.onFailure();
            return;
        }
        WonderPush.logDebug("Sending installation custom diff " + diff + " for user " + userId);
        ApiClient.Params parameters = new ApiClient.Params();
        parameters.put("body", diff.toString());
        ApiClient.requestForUser(userId, ApiClient.HttpMethod.PATCH, "/installation", parameters, new ResponseHandler() {
            @Override
            public void onFailure(Throwable ex, Response errorResponse) {
                Log.e(WonderPush.TAG, "Failed to send installation custom diff, got " + errorResponse, ex);
                handler.onFailure();
            }

            @Override
            public void onSuccess(Response response) {
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
        });
    }

    @Override
    public String toString() {
        return "JSONSyncInstallationCustom"
                + "{userId=" + userId
                + ",sync=" + sync
                + "}";
    }

    ///
    /// Forward JSONSync methods
    ///

    public JSONObject getSdkState() throws JSONException {
        return sync.getSdkState();
    }

    public void put(JSONObject diff) throws JSONException {
        sync.put(diff);
    }

    public void receiveServerState(JSONObject srvState) throws JSONException {
        sync.receiveServerState(srvState);
    }

    public void receiveState(JSONObject receivedState, boolean resetSdkState) throws JSONException {
        sync.receiveState(receivedState, resetSdkState);
    }

    public void receiveDiff(JSONObject diff) throws JSONException {
        sync.receiveDiff(diff);
    }
}
