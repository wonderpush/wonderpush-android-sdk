package com.wonderpush.sdk;

import android.net.TrafficStats;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.EventListener;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public abstract class BaseApiClient implements WonderPushRequestVault.RequestExecutor {

    private static final int RETRY_INTERVAL_BAD_AUTH = 1 * 1000; // in milliseconds
    protected static final int ERROR_INVALID_CREDENTIALS = 11000;
    protected static final int ERROR_INVALID_ACCESS_TOKEN = 11003;

    private final WonderPushRequestVault requestVault = new WonderPushRequestVault(WonderPushJobQueue.getDefaultQueue(), this);
    private volatile OkHttpClient _client = null; // lazy-initialized and retrieved using getClient()
    private boolean disabled = false;

    protected abstract void decorate(Request request);
    protected abstract String getTag();

    /**
     * A request that is guaranteed to be executed when a network connection
     * is present, surviving application reboot. The responseHandler will be
     * called only if the network is present when the request is first run.
     *
     * @param request
     */
    protected void requestEventually(Request request) {
        requestVault.put(request, 0);
    }

    /**
     * A request
     *
     * @param userId
     *            The userId to perform this request for
     * @param method
     *            The HTTP method to use
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            Request parameters
     * @param responseHandler
     *            Response handler
     */
    protected void requestForUser(String userId, HttpMethod method, String resource, Request.Params params, ResponseHandler responseHandler) {
        execute(new Request(userId, method, resource, params, responseHandler));
    }

    /**
     * A GET request
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            Request parameters
     * @param responseHandler
     *            Response handler
     */
    protected void get(String resource, Request.Params params, ResponseHandler responseHandler) {
        execute(new Request(WonderPushConfiguration.getUserId(), HttpMethod.GET, resource, params, responseHandler));
    }

    /**
     * A POST request
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            Request parameters
     * @param responseHandler
     *            Response handler
     */
    protected void post(String resource, Request.Params params, ResponseHandler responseHandler) {
        execute(new Request(WonderPushConfiguration.getUserId(), HttpMethod.POST, resource, params, responseHandler));
    }

    /**
     * A POST request that is guaranteed to be executed when a network connection
     * is present, surviving application reboot. The responseHandler will be
     * called only if the network is present when the request is first run.
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            Request parameters
     */
    protected void postEventually(String resource, Request.Params params) {
        final Request request = new Request(WonderPushConfiguration.getUserId(), HttpMethod.POST, resource, params, null);
        requestEventually(request);
    }

    /**
     * A PUT request
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            Request parameters
     * @param responseHandler
     *            Response handler
     */
    protected void put(String resource, Request.Params params, ResponseHandler responseHandler) {
        execute(new Request(WonderPushConfiguration.getUserId(), HttpMethod.PUT, resource, params, responseHandler));
    }

    /**
     * A DELETE request
     *
     * @param resource
     *            The resource path, starting with /
     * @param responseHandler
     *            Response handler
     */
    protected void delete(String resource, ResponseHandler responseHandler) {
        execute(new Request(WonderPushConfiguration.getUserId(), HttpMethod.DELETE, resource, null, responseHandler));
    }

    /**
     * Runs the specified request.
     */
    public void execute(final Request request) {
        if (null == request) {
            return;
        }

        if (!WonderPush.isInitialized()) {
            WonderPush.safeDefer(() -> request(request), 100);
            return;
        }

        // Add the access token to the params
        Request.Params params = request.getParams();
        if (null == params) {
            params = new Request.Params();
            request.setParams(params);
        }

        // Wrap the request handler with our own
        ResponseHandler wrapperHandler = new ResponseHandler() {
            @Override
            public void onSuccess(int status, Response response) {
                WonderPush.logDebug(getTag(), "Request successful: (" + status + ") " + response + " (for " + request.toHumanReadableString() + ")");
                if (request.getHandler() != null) {
                    request.getHandler().onSuccess(status, response);
                }
            }

            @Override
            public void onFailure(Throwable e, Response errorResponse) {
                WonderPush.logError(getTag(), "Request failed: " + errorResponse + " (for " + request.toHumanReadableString() + ")", e);
                if (errorResponse != null && ERROR_INVALID_ACCESS_TOKEN == errorResponse.getErrorCode()) {
                    // null out the access token
                    WonderPushConfiguration.invalidateCredentials(request.getUserId());

                    // retry later now
                    WonderPush.safeDefer(() -> execute(request), RETRY_INTERVAL_BAD_AUTH);
                } else {
                    if (request.getHandler() != null) {
                        request.getHandler().onFailure(e, errorResponse);
                    }
                }
            }

            @Override
            public void onSuccess(Response response) {
                WonderPush.logDebug(getTag(), "Request successful: " + response + " (for " + request.toHumanReadableString() + ")");
                if (request.getHandler() != null) {
                    request.getHandler().onSuccess(response);
                }
            }
        };
        Request wrapperRequest = (Request) request.clone();
        wrapperRequest.setHandler(wrapperHandler);

        // Perform request
        request(wrapperRequest);
    }

    private OkHttpClient getClient() {
        if (_client == null) {
            synchronized (this) {
                if (_client == null) {
                    _client = new OkHttpClient.Builder()
                            .addInterceptor(chain ->
                                    chain.proceed(chain.request().newBuilder()
                                            .header("User-Agent", WonderPush.getUserAgent())
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

    /**
     * Thin wrapper to the network library.
     */
    protected void request(final Request request) {
        if (null == request) {
            WonderPush.logError(getTag(), "Request with null request.");
            return;
        }

        WonderPush.safeDefer(() -> {
            // Decorate parameters
            WonderPushRequestParamsDecorator.decorate(request.getResource(), request.getParams());
            decorate(request);

            // Generate signature
            Request.BasicNameValuePair authorizationHeader = request.getAuthorizationHeader();

            WonderPush.logDebug(getTag(), "Requesting: " + request.toHumanReadableString());
            // TODO: support other contentTypes such as "application/json"
            String contentType = "application/x-www-form-urlencoded";

            // Handler
            final @NonNull ResponseHandler handler = new ResponseHandler() {
                @Override
                public void onFailure(Throwable e, Response errorResponse) {
                    WonderPush.safeDefer(() -> {
                        if (request.getHandler() != null) {
                            request.getHandler().onFailure(e, errorResponse);
                        }
                    }, 0);
                }

                @Override
                public void onSuccess(Response response) {
                    WonderPush.safeDefer(() -> {
                        if (request.getHandler() != null) {
                            request.getHandler().onSuccess(response);
                        }
                    }, 0);
                }
            };

            final long sendDate = SystemClock.elapsedRealtime();
            Callback jsonHandler = new SafeOkHttpCallback() {

                @Override
                public void onFailureSafe(Call call, IOException e) {
                    handler.onFailure(e, new Response((JSONObject)null));
                }

                @Override
                public void onResponseSafe(Call call, okhttp3.Response response) throws IOException {
                    // Try parse JSON
                    String responseString = response.body().string();
                    JSONObject responseJson;
                    try {
                        responseJson = new JSONObject(responseString);
                    } catch (JSONException e) {
                        if (WonderPush.getLogging()) {
                            Log.e(getTag(), "Unexpected string error answer: " + response.code() + " headers: " + response.headers() + " response: (" + responseString.length() + ") \"" + responseString + "\" (for " + request.toHumanReadableString() + ")", e);
                        } else {
                            Log.e(getTag(), "Unexpected string error answer: " + response.code() + " headers: " + response.headers() + " response: (" + responseString.length() + ") \"" + responseString + "\"", e);
                        }
                        handler.onFailure(e, new Response(responseString));
                        return;
                    }

                    syncTime(responseJson);
                    declareConfigVersion(responseJson);
                    if (!response.isSuccessful()) {
                        if (WonderPush.getLogging()) {
                            Log.e(getTag(), "Error answer: " + response.code() + " headers: " + response.headers() + " response: " + responseString + " (for " + request.toHumanReadableString() + ")");
                        } else {
                            Log.e(getTag(), "Error answer: " + response.code() + " headers: " + response.headers() + " response: " + responseString);
                        }
                        handler.onFailure(null, new Response(responseJson));
                    } else {
                        handler.onSuccess(response.code(), new Response(responseJson));
                    }

                }

                private void declareConfigVersion(JSONObject data) {
                    if (data == null || !data.has("_configVersion") || data.isNull("_configVersion")) return;
                    String version = data.optString("_configVersion", Long.toString(data.optLong("_configVersion", 0)));
                    if (version != null && WonderPush.getRemoteConfigManager() != null) {
                        WonderPush.getRemoteConfigManager().declareVersion(version);
                    }
                }

                private void syncTime(JSONObject data) {
                    long recvDate = SystemClock.elapsedRealtime();
                    if (data == null || !data.has("_serverTime")) {
                        return;
                    }
                    TimeSync.syncTimeWithServer(sendDate, recvDate, data.optLong("_serverTime"), data.optLong("_serverTook"));
                }
            };

            HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(WonderPushUriHelper.getAbsoluteUrl(request.getResource())).newBuilder();
            okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                    .addHeader("Content-Type", contentType);
            if (authorizationHeader != null) {
                requestBuilder.addHeader(authorizationHeader.getName(),authorizationHeader.getValue());
            }

            // NO UNNECESSARY WORK HERE, because of timed request
            switch (request.getMethod()) {
                case GET: {
                    if (request.getParams() != null) {
                        for (Request.BasicNameValuePair pair : request.getParams().getParamsList()) {
                            httpUrlBuilder.addQueryParameter(pair.getName(), pair.getValue());
                        }
                    }
                    requestBuilder.url(httpUrlBuilder.build());
                    requestBuilder.get();
                }
                break;
                case PUT: {
                    requestBuilder.url(httpUrlBuilder.build());
                    if (request.getParams() != null) {
                        requestBuilder.put(request.getParams().getFormBody());
                    }
                }
                break;
                case POST:
                    requestBuilder.url(httpUrlBuilder.build());
                    if (request.getParams() != null) {
                        requestBuilder.post(request.getParams().getFormBody());
                    }
                    break;
                case PATCH:
                    requestBuilder.url(httpUrlBuilder.build());
                    if (request.getParams() != null) {
                        requestBuilder.patch(request.getParams().getFormBody());
                    }
                    break;
                case DELETE: {
                    if (request.getParams() != null) {
                        for (Request.BasicNameValuePair pair : request.getParams().getParamsList()) {
                            httpUrlBuilder.addQueryParameter(pair.getName(), pair.getValue());
                        }
                    }
                    requestBuilder.url(httpUrlBuilder.build());
                    requestBuilder.delete();
                }
                break;
                default:
                    requestBuilder = null;
                    if (handler != null) {
                        handler.onFailure(new UnsupportedOperationException("Unhandled method " + request.getMethod()), null);
                    }
                    break;
            }

            if (isDisabled()) {
                handler.onFailure(new Request.ClientDisabledException(), new Response("Client disabled"));
                return;
            }
            if (requestBuilder != null) {
                getClient().newCall(requestBuilder.build()).enqueue(jsonHandler);
            }

        }, 0);
    }


    void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    boolean isDisabled() {
        return disabled;
    }

}
