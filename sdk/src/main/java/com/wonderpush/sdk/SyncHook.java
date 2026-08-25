package com.wonderpush.sdk;

/**
 * The install point for the sdk-sync request observer. Ported from wonderpush-ios-sdk
 * {@code WPSyncHook}. {@link BaseApiClient} consults {@link #observer()} on every request; if no
 * observer is installed (e.g. before SDK init, or after the server turns on {@code syncDisabled}),
 * the hook is completely inert.
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
