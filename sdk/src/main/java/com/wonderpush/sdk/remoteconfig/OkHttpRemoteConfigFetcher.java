package com.wonderpush.sdk.remoteconfig;

import android.net.TrafficStats;
import android.os.Process;

import com.wonderpush.sdk.SafeDeferProvider;
import com.wonderpush.sdk.SafeOkHttpCallback;
import com.wonderpush.sdk.UserAgentProvider;

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Locale;

public class OkHttpRemoteConfigFetcher implements RemoteConfigFetcher {

    @Nullable
    private volatile OkHttpClient _client = null; // lazily initialized to avoid the cost during SDK's synchronous initialization
    @Nonnull
    String clientId;
    @Nonnull
    final private SafeDeferProvider safeDeferProvider;
    @Nonnull
    final private UserAgentProvider userAgentProvider;

    public OkHttpRemoteConfigFetcher(@Nonnull String clientId, @Nonnull SafeDeferProvider safeDeferProvider, @Nonnull UserAgentProvider userAgentProvider) {
        this.clientId = clientId;
        this.safeDeferProvider = safeDeferProvider;
        this.userAgentProvider = userAgentProvider;
    }

    private @Nonnull OkHttpClient getClient() {
        if (_client == null) {
            synchronized (this) {
                if (_client == null) {
                    _client = new OkHttpClient.Builder()
                            .addInterceptor(chain ->
                                    chain.proceed(chain.request().newBuilder()
                                            .header("User-Agent", this.userAgentProvider.getUserAgent())
                                            .build())
                            )
                            .eventListener(new EventListener() {
                                @Override
                                public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
                                    TrafficStats.setThreadStatsTag(Process.myTid());
                                }
                            }).build();
                }
            }
        }
        return _client;
    }

    @Override
    public void fetchRemoteConfig(@Nullable String version, @Nonnull RemoteConfigHandler handler) {
        String url = String.format(Locale.ENGLISH, "%s%s%s?_=%d",Constants.REMOTE_CONFIG_BASE_URL, clientId, Constants.REMOTE_CONFIG_SUFFIX, System.currentTimeMillis());
        this.safeDeferProvider.safeDefer(() -> {
            Request request = new Request.Builder().url(url).get().build();
            getClient().newCall(request).enqueue(new SafeOkHttpCallback() {
                @Override
                public void onFailureSafe(Call call, IOException e) {
                    handler.handle(null, e);
                }

                @Override
                public void onResponseSafe(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        handler.handle(null, new Exception("Invalid status code from remote config server:" + response.code()));
                        return;
                    }

                    JSONObject responseJson;
                    try {
                        responseJson = new JSONObject(response.body().string());
                        String version = responseJson.optString("version", Long.toString(responseJson.optLong("version", 0)));
                        if (version != null) {
                            Long maxAge = responseJson.optLong("maxAge", responseJson.optLong("cacheTtl", 0));
                            Long minAge = responseJson.optLong("minAge", responseJson.optLong("cacheMinAge", 0));
                            RemoteConfig config = RemoteConfig.with(responseJson, version, DateHelper.now(), maxAge, minAge);
                            handler.handle(config, null);
                            return;
                        }
                        handler.handle(null, new Exception("Invalid remote config format"));
                    } catch (JSONException e) {
                        handler.handle(null, e);
                        return;
                    }
                }
            });
        }, 0);
    }
}
