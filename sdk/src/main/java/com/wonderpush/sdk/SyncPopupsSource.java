package com.wonderpush.sdk;

/**
 * The {@code popups} sync source (multi-object). Ported from wonderpush-ios-sdk
 * {@code WPSyncPopupsSource}. Needs a clock for expiry pruning; defaults to the server-adjusted time.
 */
class SyncPopupsSource implements SyncSourcePlugin {

    private final SyncClock nowProvider;

    SyncPopupsSource() {
        this(TimeSync::getTime);
    }

    SyncPopupsSource(SyncClock nowProvider) {
        this.nowProvider = nowProvider;
    }

    @Override
    public Object applyData(Object data, Object currentData) {
        return SyncPopupsStore.resetPopupsData(data, nowProvider.now());
    }

    @Override
    public Object applyDelta(Object delta, Object currentData) {
        return SyncPopupsStore.applyPopupsDelta(currentData, delta, nowProvider.now());
    }
}
