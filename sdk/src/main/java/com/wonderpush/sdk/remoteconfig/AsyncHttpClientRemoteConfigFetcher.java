package com.wonderpush.sdk.remoteconfig;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import cz.msebera.android.httpclient.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

public class AsyncHttpClientRemoteConfigFetcher implements RemoteConfigFetcher {
    @Nonnull
    AsyncHttpClient client = new AsyncHttpClient(); // to allow any HTTPS certificate use: new AsyncHttpClient(true, 80, 443);
    @Nonnull
    String clientId;
    @Nonnull
    private SafeDeferProvider safeDeferProvider;
    public AsyncHttpClientRemoteConfigFetcher(String clientId, SafeDeferProvider safeDeferProvider) {
        this.clientId = clientId;
        this.safeDeferProvider = safeDeferProvider;
    }

    public interface SafeDeferProvider {
        void safeDefer(Runnable r, long defer);
    }

    @Override
    public void fetchRemoteConfig(@Nullable String version, @Nonnull RemoteConfigHandler handler) {
        String url = String.format("%s%s%s?_=%d",Constants.REMOTE_CONFIG_BASE_URL, clientId, Constants.REMOTE_CONFIG_SUFFIX, new Date().getTime());
        this.safeDeferProvider.safeDefer(() -> {
            client.get(url, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    if (statusCode != 200) {
                        handler.handle(null, new Exception("Invalid status code from remote config server:" + statusCode));
                        return;
                    }

                    String version = response.optString("version", Long.toString(response.optLong("version", 0)));
                    if (version != null) {
                        Long maxAge = response.optLong("maxAge", response.optLong("cacheTtl", 0));
                        Long minAge = response.optLong("minAge", response.optLong("cacheMinAge", 0));
                        RemoteConfig config = RemoteConfig.with(response, version, DateHelper.now(), maxAge, minAge);
                        handler.handle(config, null);
                        return;
                    }
                    handler.handle(null, new Exception("Invalid remote config format"));
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    handler.handle(null, new Exception("Invalid response from remote config server"));
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    handler.handle(null, throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    handler.handle(null, throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    handler.handle(null, throwable);
                }
            });
        }, 0);
    }
}
