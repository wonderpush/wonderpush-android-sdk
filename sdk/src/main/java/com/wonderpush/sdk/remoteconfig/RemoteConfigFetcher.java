package com.wonderpush.sdk.remoteconfig;

import javax.annotation.Nullable;

public interface RemoteConfigFetcher {
    void fetchRemoteConfig(@Nullable String version, RemoteConfigHandler handler);
}
