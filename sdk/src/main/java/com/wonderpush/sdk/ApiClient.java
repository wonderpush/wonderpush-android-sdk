package com.wonderpush.sdk;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * A REST client that lets you hit the WonderPush REST server.
 */
class ApiClient {

    private static final String TAG = ApiClient.class.getSimpleName();

    enum HttpMethod {
        GET,
        PUT,
        POST,
        DELETE,
        PATCH,
        // APPEND ONLY!
    }

    private static final int RETRY_INTERVAL_BAD_AUTH = 1 * 1000; // in milliseconds
    private static final int RETRY_INTERVAL = 30 * 1000; // in milliseconds
    protected static final int ERROR_INVALID_CREDENTIALS = 11000;
    protected static final int ERROR_INVALID_ACCESS_TOKEN = 11003;

    private static boolean sIsFetchingAnonymousAccessToken = false;
    private static final List<ResponseHandler> sPendingHandlers = new ArrayList<>();

    private static final OkHttpClient sClient = new OkHttpClient();
    private static boolean sDisabled = false;

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
    protected static void requestForUser(String userId, HttpMethod method, String resource, Request.Params params, ResponseHandler responseHandler) {
        requestAuthenticated(new Request(userId, method, resource, params, responseHandler));
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
    protected static void get(String resource, Request.Params params, ResponseHandler responseHandler) {
        requestAuthenticated(new Request(WonderPushConfiguration.getUserId(), HttpMethod.GET, resource, params, responseHandler));
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
    protected static void post(String resource, Request.Params params, ResponseHandler responseHandler) {
        requestAuthenticated(new Request(WonderPushConfiguration.getUserId(), HttpMethod.POST, resource, params, responseHandler));
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
    protected static void postEventually(String resource, Request.Params params) {
        final Request request = new Request(WonderPushConfiguration.getUserId(), HttpMethod.POST, resource, params, null);
        WonderPushRequestVault.getDefaultVault().put(request, 0);
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
    protected static void put(String resource, Request.Params params, ResponseHandler responseHandler) {
        requestAuthenticated(new Request(WonderPushConfiguration.getUserId(), HttpMethod.PUT, resource, params, responseHandler));
    }

    /**
     * A DELETE request
     *
     * @param resource
     *            The resource path, starting with /
     * @param responseHandler
     *            Response handler
     */
    protected static void delete(String resource, ResponseHandler responseHandler) {
        requestAuthenticated(new Request(WonderPushConfiguration.getUserId(), HttpMethod.DELETE, resource, null, responseHandler));
    }

    /**
     * If no access token is found in the user's preferences, fetch an anonymous access token.
     *
     * @param onFetchedHandler
     *            A handler called if a request to fetch an access token has been
     *            executed successfully, never called if retrieved from cache
     * @return Whether or not a request has been executed to fetch an anonymous
     *         access token (true fetching, false retrieved from local cache)
     */
    protected static boolean fetchAnonymousAccessTokenIfNeeded(final String userId, final ResponseHandler onFetchedHandler) {
        if (!WonderPush.isInitialized()) {
            // Note: Could use WonderPush.safeDefer() here but as we require consent to proceed,
            // let's use WonderPush.safeDeferWithConsent() to additionally passively wait for SDK initialization.
            WonderPush.safeDeferWithConsent(new Runnable() {
                @Override
                public void run() {
                    if (!fetchAnonymousAccessTokenIfNeeded(userId, onFetchedHandler)) {
                        // Call the handler anyway
                        onFetchedHandler.onSuccess(null);
                    }
                }
            }, null);
            return true; // true: the handler will be called
        }

        if (null == WonderPushConfiguration.getAccessToken()) {
            fetchAnonymousAccessToken(userId, onFetchedHandler);
            return true;
        }
        return false;
    }

    /**
     * If no access token is found in the user's preferences, fetch an anonymous access token.
     */
    protected static void fetchAnonymousAccessTokenIfNeeded(final String userId) {
        fetchAnonymousAccessTokenIfNeeded(userId, null);
    }

    /**
     * Runs the specified request and ensure a valid access token is fetched if
     * necessary beforehand, or afterwards (and re-run the request) if the request
     * fails for auth reasons.
     */
    protected static void requestAuthenticated(final Request request) {
        if (null == request) {
            return;
        }

        if (!WonderPush.isInitialized()) {
            WonderPush.safeDefer(new Runnable() {
                @Override
                public void run() {
                    requestAuthenticated(request);
                }
            }, 100);
            return;
        }

        String accessToken = WonderPushConfiguration.getAccessTokenForUserId(request.getUserId());

        if (accessToken == null) {
            // User is not authenticated, request a token
            fetchAnonymousAccessTokenAndRunRequest(request);
            return;
        }

        // Add the access token to the params
        Request.Params params = request.getParams();
        if (null == params) {
            params = new Request.Params();
            request.setParams(params);
        }

        params.remove("accessToken");
        params.put("accessToken", accessToken);

        // Wrap the request handler with our own
        ResponseHandler wrapperHandler = new ResponseHandler() {
            @Override
            public void onSuccess(int status, Response response) {
                WonderPush.logDebug("Request successful: (" + status + ") " + response + " (for " + request + ")");
                if (request.getHandler() != null) {
                    request.getHandler().onSuccess(status, response);
                }
            }

            @Override
            public void onFailure(Throwable e, Response errorResponse) {
                WonderPush.logError("Request failed: " + errorResponse, e);
                if (errorResponse != null && ERROR_INVALID_ACCESS_TOKEN == errorResponse.getErrorCode()) {
                    // null out the access token
                    WonderPushConfiguration.invalidateCredentials();

                    // retry later now
                    WonderPush.safeDefer(new Runnable() {
                        @Override
                        public void run() {
                            requestAuthenticated(request);
                        }
                    }, RETRY_INTERVAL_BAD_AUTH);
                } else {
                    if (request.getHandler() != null) {
                        request.getHandler().onFailure(e, errorResponse);
                    }
                }
            }

            @Override
            public void onSuccess(Response response) {
                WonderPush.logDebug("Request successful: " + response + " (for " + request + ")");
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

    /**
     * Thin wrapper to the network library.
     */
    private static void request(final Request request) {
        if (null == request) {
            WonderPush.logError("Request with null request.");
            return;
        }

        WonderPush.safeDefer(new Runnable() {
            @Override
            public void run() {
                // Decorate parameters
                WonderPushRequestParamsDecorator.decorate(request.getResource(), request.getParams());

                // Generate signature
                Request.BasicNameValuePair authorizationHeader = request.getAuthorizationHeader();

                String url = WonderPushUriHelper.getAbsoluteUrl(request.getResource());
                WonderPush.logDebug("requesting url: " + request.getMethod() + " " + url + "?" + request.getParams().getURLEncodedString());
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
                Callback jsonHandler = new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        WonderPush.setNetworkAvailable(false);
                        handler.onFailure(e, new Response((JSONObject)null));
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        WonderPush.setNetworkAvailable(true);
                        // Try parse JSON
                        String responseString = response.body().string();
                        JSONObject responseJson;
                        try {
                            responseJson = new JSONObject(responseString);
                        } catch (JSONException e) {
                            WonderPush.logError("Unexpected string error answer: " + response.code() + " headers: " + response.headers().toString() + " response: (" + responseString.length() + ") \"" + responseString + "\"");
                            handler.onFailure(e, new Response(responseString));
                            return;
                        }

                        syncTime(responseJson);
                        declareConfigVersion(responseJson);
                        if (!response.isSuccessful()) {
                            WonderPush.logError("Error answer: " + response.code() + " headers: " + response.headers().toString() + " response: " + responseString);
                            WonderPush.logDebug("Request Error: " + responseString);
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

                HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(url).newBuilder();
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
                    sClient.newCall(requestBuilder.build()).enqueue(jsonHandler);
                }

            }
        }, 0);
    }

    protected static void fetchAnonymousAccessToken(final String userId, final ResponseHandler handler) {
        fetchAnonymousAccessToken(userId, handler, 0);
    }

    protected static void fetchAnonymousAccessToken(final String userId, final ResponseHandler handler, final int nbRetries) {
        if (sIsFetchingAnonymousAccessToken) {
            queueHandler(handler);
            return;
        }
        sIsFetchingAnonymousAccessToken = true;
        WonderPush.safeDeferWithConsent(new Runnable() {
            @Override
            public void run() {
                fetchAnonymousAccessToken_inner(userId, handler, nbRetries);
            }
        }, "fetchAnonymousAccessToken");
    }

    private static void fetchAnonymousAccessToken_inner(final String userId, final ResponseHandler handler, final int nbRetries) {
        Request.Params authParams = new Request.Params();
        authParams.put("clientId", WonderPush.getClientId());
        authParams.put("devicePlatform", "Android");
        authParams.put("deviceModel", InstallationManager.getDeviceModel());
        String deviceId = WonderPush.getDeviceId();
        if (null != deviceId) {
            authParams.put("deviceId", deviceId);
        }
        if (null != userId) {
            authParams.put("userId", userId);
        }

        String resource = "/authentication/accessToken";

        request(new Request(userId, HttpMethod.POST, resource, authParams,
                new ResponseHandler() {
                    @Override
                    public void onFailure(Throwable e, Response errorResponse) {
                        if (nbRetries <= 0) {
                            Log.e(TAG, "Error request anonymous access token (aborting): " + (errorResponse != null ? errorResponse.toString() : "null error response, aborting"), e);
                            if (errorResponse != null && ERROR_INVALID_CREDENTIALS == errorResponse.getErrorCode()) {
                                Log.e(TAG, "Check your clientId/clientSecret couple");
                            }

                            sIsFetchingAnonymousAccessToken = false;
                            if (null != handler) {
                                handler.onFailure(e, errorResponse);
                            }
                            ResponseHandler chainedHandler;
                            while ((chainedHandler = dequeueHandler()) != null) {
                                chainedHandler.onFailure(e, errorResponse);
                            }
                            return;
                        }
                        Log.e(TAG, "Error request anonymous access token (retrying: " + nbRetries + "): " + (errorResponse != null ? errorResponse.toString() : "null error response, retrying"), e);

                        WonderPush.safeDefer(new Runnable() {
                            @Override
                            public void run() {
                                WonderPush.logDebug("re-requesting access token!");

                                sIsFetchingAnonymousAccessToken = false;
                                fetchAnonymousAccessToken(userId, handler, nbRetries - 1);
                            }
                        }, RETRY_INTERVAL);
                    }

                    @Override
                    public void onSuccess(int statusCode, Response response) {
                        // Parse response
                        JSONObject json = response.getJSONObject();
                        WonderPush.logDebug("Got access token response: " + json);
                        if (json != null && json.has("token") && json.has("data")) {
                            String token = JSONUtil.getString(json, "token");
                            JSONObject data = json.optJSONObject("data");
                            if (data != null && data.has("installationId")) {
                                String prevUserId = WonderPushConfiguration.getUserId();
                                try {
                                    // Make sure we alter the storage of the appropriate user
                                    WonderPushConfiguration.changeUserId(userId);

                                    String sid = JSONUtil.getString(data, "sid");
                                    String installationId = JSONUtil.getString(data, "installationId");
                                    String userId = JSONUtil.getString(data, "userId");

                                    // Store access token
                                    WonderPushConfiguration.setAccessToken(token);
                                    WonderPushConfiguration.setSID(sid);
                                    WonderPushConfiguration.setInstallationId(installationId);
                                    WonderPushConfiguration.setUserId(userId);

                                    JSONObject installation = json.optJSONObject("_installation");
                                    if (installation != null) {
                                        WonderPush.receivedFullInstallationFromServer(installation);
                                    }
                                } finally {
                                    // Make sure to switch back to the current user now
                                    WonderPushConfiguration.changeUserId(prevUserId);
                                }
                                sIsFetchingAnonymousAccessToken = false;

                                WonderPush.refreshPreferencesAndConfiguration(false);

                                // call handlers
                                if (null != handler) {
                                    handler.onSuccess(statusCode, response);
                                }
                                ResponseHandler chainedHandler;
                                while ((chainedHandler = dequeueHandler()) != null) {
                                    chainedHandler.onSuccess(statusCode, response);
                                }
                                return;
                            }
                        }
                        Log.e(TAG, "Could not obtain anonymous access token from server");
                    }

                    @Override
                    public void onSuccess(Response response) {
                        this.onSuccess(200, response);
                    }
                }
        ));
    }

    /**
     * Fetches an anonymous access token and run the given request with that token.
     * Retries when access token cannot be fetched.
     *
     * @param request
     *            The request to be run
     */
    protected static void fetchAnonymousAccessTokenAndRunRequest(final Request request) {
        fetchAnonymousAccessToken(request.getUserId(), new ResponseHandler() {
            @Override
            public void onSuccess(Response response) {
                requestAuthenticated(request);
            }

            @Override
            public void onFailure(Throwable e, Response errorResponse) {
                request.getHandler().onFailure(e, errorResponse);
            }
        });
    }

    private static void queueHandler(ResponseHandler handler) {
        if (null == handler) {
            return;
        }

        synchronized (sPendingHandlers) {
            sPendingHandlers.add(handler);
        }
    }

    private static ResponseHandler dequeueHandler() {
        ResponseHandler handler = null;
        synchronized (sPendingHandlers) {
            if (sPendingHandlers.size() > 0) {
                handler = sPendingHandlers.get(0);
                if (null != handler) {
                    sPendingHandlers.remove(0);
                }
            }
        }
        return handler;
    }

    static void setDisabled(boolean disabled) {
        sDisabled = disabled;
    }

    static boolean isDisabled() {
        return sDisabled;
    }
}
