package com.wonderpush.sdk.remoteconfig;

import javax.annotation.Nullable;

public interface RemoteConfigVersionHandler {
    void handle(@Nullable RemoteConfig config, @Nullable String version, @Nullable Throwable error);
}
