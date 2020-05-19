package com.wonderpush.sdk.remoteconfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface RemoteConfigStorage {
    void storeRemoteConfig(RemoteConfig config, @Nullable ErrorHandler handler);
    void loadRemoteConfigAndHighestDeclaredVersion(RemoteConfigVersionHandler handler);
    void declareVersion(@Nonnull String version, @Nullable ErrorHandler handler);
}
