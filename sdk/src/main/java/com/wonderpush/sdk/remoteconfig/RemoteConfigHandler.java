package com.wonderpush.sdk.remoteconfig;

import javax.annotation.Nullable;

public interface RemoteConfigHandler {
    void handle(@Nullable RemoteConfig config, @Nullable Throwable error);
}
