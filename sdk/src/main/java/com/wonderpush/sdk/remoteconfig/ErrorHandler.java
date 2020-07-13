package com.wonderpush.sdk.remoteconfig;

import javax.annotation.Nullable;

public interface ErrorHandler {
    void handle(@Nullable Throwable error);
}
