package com.wonderpush.sdk.remoteconfig;

public class Constants {
    public static final long REMOTE_CONFIG_DEFAULT_MINIMUM_CONFIG_AGE = 0;
    public static final long REMOTE_CONFIG_DEFAULT_MAXIMUM_CONFIG_AGE = 86400000 * 10;
    public static final String DISABLE_FETCH_KEY = "disableConfigFetch";
    public static final String INTENT_REMOTE_CONFIG_UPDATED = "INTENT_REMOTE_CONFIG_UPDATED";
    public static final String EXTRA_REMOTE_CONFIG = "EXTRA_REMOTE_CONFIG";
    public static final String REMOTE_CONFIG_BASE_URL = "https://cdn.by.wonderpush.com/config/clientids/";
    public static final String REMOTE_CONFIG_SUFFIX = "-Android";
}
