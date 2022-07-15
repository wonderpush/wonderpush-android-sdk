package com.wonderpush.sdk.ratelimiter;

import androidx.annotation.NonNull;

public class RateLimit {
    @NonNull
    public final String key;
    public final long timeToLive;
    public final int limit;

    public RateLimit(@NonNull String key, int timeToLive, int limit) {
        this.key = key;
        this.timeToLive = timeToLive;
        this.limit = limit;
    }
}
