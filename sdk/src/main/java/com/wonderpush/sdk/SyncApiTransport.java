package com.wonderpush.sdk;

import java.util.Map;

/**
 * Adapts a {@link SyncApiRequestSender} to the fetcher's {@link SyncFetchTransport}.
 * Ported from wonderpush-ios-sdk {@code WPSyncAPITransport}.
 */
class SyncApiTransport implements SyncFetchTransport {

    private final SyncApiRequestSender sender;

    SyncApiTransport(SyncApiRequestSender sender) {
        this.sender = sender;
    }

    @Override
    public void fetch(String source, String userId, String path, Map<String, Object> params, Completion completion) {
        sender.send(userId, path, params, success -> {
            if (completion != null) completion.onComplete(success);
        });
    }
}
