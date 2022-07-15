package com.wonderpush.sdk.remoteconfig;

import com.wonderpush.sdk.WonderPushConfiguration;

public class Constants {
    public static final long REMOTE_CONFIG_DEFAULT_MINIMUM_CONFIG_AGE = 0;
    public static final long REMOTE_CONFIG_DEFAULT_MAXIMUM_CONFIG_AGE = 86400000 * 10;
    public static final String DISABLE_FETCH_KEY = "disableConfigFetch";
    public static final String INTENT_REMOTE_CONFIG_UPDATED = "INTENT_REMOTE_CONFIG_UPDATED";
    public static final String EXTRA_REMOTE_CONFIG = "EXTRA_REMOTE_CONFIG";
    public static final String REMOTE_CONFIG_BASE_URL = "https://cdn.by.wonderpush.com/config/clientids/";
    public static final String REMOTE_CONFIG_SUFFIX = "-Android";
    public static final String REMOTE_CONFIG_DISABLE_JSON_SYNC_KEY = "disableJsonSync";
    public static final String REMOTE_CONFIG_DISABLE_API_CLIENT_KEY = "disableApiClient";
    public static final String REMOTE_CONFIG_DISABLE_MEASUREMENTS_API_CLIENT_KEY = "disableMeasurementsApiClient";
    public static final String REMOTE_CONFIG_EVENTS_BLACK_WHITE_LIST_KEY = "eventsBlackWhiteList";
    public static final String REMOTE_CONFIG_TRACK_EVENTS_FOR_NON_SUBSCRIBERS_KEY = "trackEventsForNonSubscribers";
    public static final String REMOTE_CONFIG_TRACKED_EVENTS_UNCOLLAPSED_MAXIMUM_AGE_MS_KEY = "trackedEventsUncollapsedMaximumAgeMs";
    public static final String REMOTE_CONFIG_TRACKED_EVENTS_UNCOLLAPSED_MAXIMUM_COUNT_KEY = "trackedEventsUncollapsedMaximumCount";
    public static final String REMOTE_CONFIG_TRACKED_EVENTS_COLLAPSED_LAST_BUILTIN_MAXIMUM_COUNT_KEY = "trackedEventsCollapsedLastBuiltinMaximumCount";
    public static final String REMOTE_CONFIG_TRACKED_EVENTS_COLLAPSED_LAST_CUSTOM_MAXIMUM_COUNT_KEY = "trackedEventsCollapsedLastCustomMaximumCount";
    public static final String REMOTE_CONFIG_TRACKED_EVENTS_COLLAPSED_OTHER_MAXIMUM_COUNT_KEY = "trackedEventsCollapsedOtherMaximumCount";
    public static final String REMOTE_CONFIG_ANONYMOUS_API_CLIENT_RATE_LIMIT_LIMIT = "anonymousApiClientRateLimitLimit";
    public static final String REMOTE_CONFIG_ANONYMOUS_API_CLIENT_RATE_LIMIT_TIME_TO_LIVE_MILLISECONDS = "anonymousApiClientRateLimitTimeToLiveMilliseconds";

}
