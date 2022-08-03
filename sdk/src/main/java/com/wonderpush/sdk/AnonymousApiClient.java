package com.wonderpush.sdk;

import static com.wonderpush.sdk.remoteconfig.Constants.REMOTE_CONFIG_ANONYMOUS_API_CLIENT_RATE_LIMIT_LIMIT;
import static com.wonderpush.sdk.remoteconfig.Constants.REMOTE_CONFIG_ANONYMOUS_API_CLIENT_RATE_LIMIT_TIME_TO_LIVE_MILLISECONDS;

import android.util.Log;

import com.wonderpush.sdk.ratelimiter.RateLimit;
import com.wonderpush.sdk.ratelimiter.RateLimiter;
import com.wonderpush.sdk.remoteconfig.RemoteConfigManager;

public class AnonymousApiClient extends BaseApiClient {
    private static final int ANONYMOUS_API_CLIENT_RATE_LIMIT_LIMIT = 6;
    private static final long ANONYMOUS_API_CLIENT_RATE_LIMIT_TIME_TO_LIVE_MILLISECONDS = 60000;

    private static final String TAG = "WonderPush." + AnonymousApiClient.class.getSimpleName();
    private static AnonymousApiClient sInstance = new AnonymousApiClient();
    public static AnonymousApiClient getInstance() {
        return sInstance;
    }

    @Override
    protected void decorate(Request request) {
        Request.Params params = request.getParams();
        if (null == params) {
            params = new Request.Params();
            request.setParams(params);
        }
        params.remove("clientId");
        params.put("clientId", WonderPush.getClientId());
        params.remove("clientSecret");
        params.put("clientSecret", WonderPush.getClientSecret());
        params.remove("devicePlatform");
        params.put("devicePlatform", "Android");
        params.remove("deviceId");
        params.put("deviceId", WonderPushConfiguration.getDeviceId());
        params.remove("userId");
        params.put("userId", request.getUserId() != null ? request.getUserId() : "");
    }

    @Override
    public void execute(Request request) {
        WonderPush.getRemoteConfigManager().read((config, error) -> {
            int limit = error != null ? ANONYMOUS_API_CLIENT_RATE_LIMIT_LIMIT : config.getData().optInt(REMOTE_CONFIG_ANONYMOUS_API_CLIENT_RATE_LIMIT_LIMIT, ANONYMOUS_API_CLIENT_RATE_LIMIT_LIMIT);
            long timeToLive = error != null ? ANONYMOUS_API_CLIENT_RATE_LIMIT_TIME_TO_LIVE_MILLISECONDS : config.getData().optLong(REMOTE_CONFIG_ANONYMOUS_API_CLIENT_RATE_LIMIT_TIME_TO_LIVE_MILLISECONDS, ANONYMOUS_API_CLIENT_RATE_LIMIT_TIME_TO_LIVE_MILLISECONDS);
            RateLimit rateLimit = new RateLimit("AnonymousAPIClient", timeToLive, limit);
            try {
                RateLimiter limiter = RateLimiter.getInstance();
                if (limiter.isRateLimited(rateLimit)) {
                    // Retry in 10 seconds
                    WonderPush.safeDefer(() -> {
                        execute(request);
                    }, 10000);
                } else {
                    limiter.increment(rateLimit);
                    super.execute(request);
                }
            } catch (RateLimiter.MissingSharedPreferencesException e) {
                Log.e(WonderPush.TAG, "Could not rate limit anonymous API client", e);
            }
        });
    }

    @Override
    protected String getTag() {
        return TAG;
    }

}
