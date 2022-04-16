package com.wonderpush.sdk;

public interface SafeDeferProvider {
    void safeDefer(Runnable r, long defer);
}