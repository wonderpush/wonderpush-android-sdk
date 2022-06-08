package com.wonderpush.sdk;

public interface LogErrorProvider {
    void logError(String msg);
    void logError(String msg, Exception e);
}
