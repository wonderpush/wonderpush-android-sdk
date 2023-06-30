package com.wonderpush.sdk;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class JSONSyncInstallation extends JSONSync {

    private static final String UPGRADE_META_VERSION_KEY = "version";
    private static final Long UPGRADE_META_VERSION_0_INITIAL = 0l;
    private static final Long UPGRADE_META_VERSION_1_IMPORTED_CUSTOM = 1l;
    private static final Long UPGRADE_META_VERSION_LATEST = UPGRADE_META_VERSION_1_IMPORTED_CUSTOM;

    private static final Map<String, JSONSyncInstallation> sInstancePerUserId = new HashMap<>();
    private static DeferredFuture<Void> initializedDeferred = new DeferredFuture<>();
    private static boolean initializing = false;
    private static boolean initialized = false;

    private final String userId;
    private long firstDelayedWriteDate;
    private static boolean disabled = false;

    private Future<Void> scheduledPatchCallDelayedTask;

    @Override
    public void doUpgrade(JSONObject upgradeMeta, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, JSONObject inflightDiff, JSONObject inflightPutAccumulator) {
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

    private static synchronized void waitForInitialization() {
        if (!initialized) {
            try {
                initializedDeferred.getFuture().get();
            } catch (ExecutionException ex) {
                // This is thrown if the deferred is settled to an exception, which we don't do
                Log.e(WonderPush.TAG, "Unexpected error while waiting for JSONSyncInstallation initialization", ex);
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                Log.e(WonderPush.TAG, "Unexpected error while waiting for JSONSyncInstallation initialization", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    public static void initialize() {
        synchronized (sInstancePerUserId) {
            if (initialized) {
                return;
            } else if (initializing) {
                waitForInitialization();
                return;
            }
            initializing = true;

            JSONObject installationCustomSyncStatePerUserId = WonderPushConfiguration.getInstallationCustomSyncStatePerUserId();
            if (installationCustomSyncStatePerUserId == null) installationCustomSyncStatePerUserId = new JSONObject();
            Iterator<String> it = installationCustomSyncStatePerUserId.keys();
            while (it.hasNext()) {
                String userId = it.next();
                JSONObject state = installationCustomSyncStatePerUserId.optJSONObject(userId);
                if (userId != null && userId.length() == 0) userId = null;
                try {
                    sInstancePerUserId.put(userId, new JSONSyncInstallation(userId, state));
                } catch (JSONException ex1) {
                    Log.e(WonderPush.TAG, "Failed to restore installation custom from saved state for user " + userId + " and state " + state, ex1);
                    try {
                        sInstancePerUserId.put(userId, new JSONSyncInstallation(userId, null));
                    } catch (JSONException ex2) {
                        Log.e(WonderPush.TAG, "Failed to restore installation custom from saved state for user " + userId + " and state null", ex2);
                    }
                }

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

            initialized = true;
            initializedDeferred.set(null); // settle the deferred
            initializing = false;
        }
    }

    public static JSONSyncInstallation forCurrentUser() {
        return forUser(WonderPushConfiguration.getUserId());
    }

    static JSONSyncInstallation forUser(String userId) {
        waitForInitialization();
        if (userId != null && userId.length() == 0) userId = null;
        synchronized (sInstancePerUserId) {
            JSONSyncInstallation rtn = sInstancePerUserId.get(userId);
            if (rtn == null) {
                try {
                    rtn = new JSONSyncInstallation(userId, null);
                    sInstancePerUserId.put(userId, rtn);
                } catch (JSONException ex) {
                    Log.e(WonderPush.TAG, "Failed to restore installation custom from saved state for user " + userId + " and state null", ex);
                }
            }
            return rtn;
        }
    }

    public static void flushAll() {
        flushAll(false);
    }

    public static void flushAll(boolean sync) {
        waitForInitialization();
        WonderPush.logDebug("Flushing delayed updates of custom properties for all known users");
        synchronized (sInstancePerUserId) {
            for (JSONSyncInstallation jsonSync : sInstancePerUserId.values()) {
                jsonSync.flush(sync);
            }
        }
    }

    public static synchronized void setDisabled(boolean disabled) {
        JSONSyncInstallation.disabled = disabled;
    }

    public static synchronized boolean isDisabled() {
        return disabled;
    }

    private JSONSyncInstallation(String userId, JSONObject sdkState, JSONObject serverState) {
        super(sdkState, serverState);
        if (userId != null && userId.length() == 0) userId = null;
        this.userId = userId;
    }

    private JSONSyncInstallation(String userId, JSONObject savedState) throws JSONException {
        super(savedState);
        if (userId != null && userId.length() == 0) userId = null;
        this.userId = userId;
    }

    synchronized void flush() {
        flush(false);
    }

    synchronized void flush(boolean sync) {
        if (scheduledPatchCallDelayedTask != null) {
            scheduledPatchCallDelayedTask.cancel(false);
            scheduledPatchCallDelayedTask = null;
        }
        if (sync || putAndFlushSynchronously()) {
            performScheduledPatchCall();
        } else {
            WonderPush.safeDefer(() -> {
                performScheduledPatchCall();
            }, 0);
        }
    }

    @Override
    protected synchronized void doSave(JSONObject state) {
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
    protected synchronized void doSchedulePatchCall() {
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
                            doSchedulePatchCall(); // NOTE: imposes this function to be somewhat reentrant
                        } catch (Exception ex) {
                            Log.e(WonderPush.TAG, "Unexpected error on user consent changed.", ex);
                        }
                    }
                }
            });
            return;
        }
        scheduledPatchCallDelayedTask = WonderPush.safeDefer(
                () -> {
                    try {
                        performScheduledPatchCall();
                    } catch (Exception ex) {
                        Log.e(WonderPush.TAG, "Unexpected error on scheduled task", ex);
                    }
                    return null;
                },
                Math.min(InstallationManager.CACHED_INSTALLATION_CUSTOM_PROPERTIES_MIN_DELAY,
                        firstDelayedWriteDate + InstallationManager.CACHED_INSTALLATION_CUSTOM_PROPERTIES_MAX_DELAY - nowRT));
    }

    @Override
    synchronized boolean performScheduledPatchCall() {
        if (!WonderPush.hasUserConsent()) {
            WonderPush.logDebug("Need consent, not performing scheduled patch call for user " + userId);
            return false;
        }
        firstDelayedWriteDate = 0;
        return super.performScheduledPatchCall();
    }

    @Override
    protected synchronized void doServerPatchInstallation(final JSONObject diff, final ResponseHandler handler) {
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
                + ",super=" + super.toString()
                + "}";
    }

}
