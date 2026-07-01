package com.wonderpush.sdk;

/**
 * The install point for the sdk-sync request observer. Ported from wonderpush-ios-sdk
 * {@code WPSyncHook}. {@link BaseApiClient} consults {@link #observer()} on every request; until an
 * observer is installed (only when {@code syncEnabled} is on), the hook is completely inert.
 */
class SyncHook {

    private static SyncRequestObserver installedObserver;

    static synchronized void installObserver(SyncRequestObserver observer) {
        installedObserver = observer;
    }

    static synchronized SyncRequestObserver observer() {
        return installedObserver;
    }

    private SyncHook() {}
}
