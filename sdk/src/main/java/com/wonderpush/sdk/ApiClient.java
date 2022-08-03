package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONObject;

import java.util.*;

/**
 * A REST client that lets you hit the WonderPush REST server.
 */
class ApiClient extends BaseApiClient {

    private static final String TAG = "WonderPush." + ApiClient.class.getSimpleName();
    private static final int RETRY_INTERVAL_ACCESS_TOKEN = 30 * 1000; // in milliseconds

    private static final ApiClient sInstance = new ApiClient();
    public static ApiClient getInstance() {
        return sInstance;
    }

    private boolean isFetchingAnonymousAccessToken = false;
    private final List<ResponseHandler> pendingHandlers = new ArrayList<>();

    @Override
    protected void decorate(Request request) {
        Request.Params params = request.getParams();
        if (null == params) {
            params = new Request.Params();
            request.setParams(params);
        }
        params.remove("accessToken");
        params.put("accessToken", getAccessTokenForRequest(request));
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    private String getAccessTokenForRequest(Request request) {
        return WonderPushConfiguration.getAccessTokenForUserId(request.getUserId());
    }

    /**
     * Runs the specified request and ensure a valid access token is fetched if
     * necessary beforehand, or afterwards (and re-run the request) if the request
     * fails for auth reasons.
     */
    @Override
    public void execute(Request request) {
        String accessToken = getAccessTokenForRequest(request);

        if (accessToken == null) {
            // User is not authenticated, request a token
            fetchAnonymousAccessTokenAndRunRequest(request);
            return;
        }
        super.execute(request);
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
    protected boolean fetchAnonymousAccessTokenIfNeeded(final String userId, final ResponseHandler onFetchedHandler) {
        if (!WonderPush.isInitialized()) {
            // Note: Could use WonderPush.safeDefer() here but as we require consent to proceed,
            // let's use WonderPush.safeDeferWithConsent() to additionally passively wait for SDK initialization.
            WonderPush.safeDeferWithConsent(() -> {
                if (!fetchAnonymousAccessTokenIfNeeded(userId, onFetchedHandler)) {
                    // Call the handler anyway
                    onFetchedHandler.onSuccess(null);
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
    protected void fetchAnonymousAccessTokenIfNeeded(final String userId) {
        fetchAnonymousAccessTokenIfNeeded(userId, null);
    }

    protected void fetchAnonymousAccessToken(final String userId, final ResponseHandler handler) {
        fetchAnonymousAccessToken(userId, handler, 0);
    }

    protected void fetchAnonymousAccessToken(final String userId, final ResponseHandler handler, final int nbRetries) {
        if (isFetchingAnonymousAccessToken) {
            queueHandler(handler);
            return;
        }
        isFetchingAnonymousAccessToken = true;
        WonderPush.safeDeferWithConsent(() -> fetchAnonymousAccessToken_inner(userId, handler, nbRetries), "fetchAnonymousAccessToken");
    }

    private void fetchAnonymousAccessToken_inner(final String userId, final ResponseHandler handler, final int nbRetries) {
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
                            if (e instanceof Request.ClientDisabledException) {
                                if (WonderPush.getLogging()) Log.w(TAG, "Request anonymous access token is currently disabled (once the configuration is fetched, it should enable it)");
                            } else {
                                Log.e(TAG, "Error request anonymous access token (aborting): " + (errorResponse != null ? errorResponse.toString() : "null error response, aborting"), e);
                            }
                            if (errorResponse != null && ERROR_INVALID_CREDENTIALS == errorResponse.getErrorCode()) {
                                Log.e(TAG, "Check your clientId/clientSecret couple");
                            }

                            isFetchingAnonymousAccessToken = false;
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

                        WonderPush.safeDefer(() -> {
                            WonderPush.logDebug(TAG, "re-requesting access token!");

                            isFetchingAnonymousAccessToken = false;
                            fetchAnonymousAccessToken(userId, handler, nbRetries - 1);
                        }, RETRY_INTERVAL_ACCESS_TOKEN);
                    }

                    @Override
                    public void onSuccess(int statusCode, Response response) {
                        // Parse response
                        JSONObject json = response.getJSONObject();
                        WonderPush.logDebug(TAG, "Got access token response: " + json);
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
                                isFetchingAnonymousAccessToken = false;

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
    protected void fetchAnonymousAccessTokenAndRunRequest(final Request request) {
        fetchAnonymousAccessToken(request.getUserId(), new ResponseHandler() {
            @Override
            public void onSuccess(Response response) {
                execute(request);
            }

            @Override
            public void onFailure(Throwable e, Response errorResponse) {
                request.getHandler().onFailure(e, errorResponse);
            }
        });
    }

    private void queueHandler(ResponseHandler handler) {
        if (null == handler) {
            return;
        }

        synchronized (pendingHandlers) {
            pendingHandlers.add(handler);
        }
    }

    private ResponseHandler dequeueHandler() {
        ResponseHandler handler = null;
        synchronized (pendingHandlers) {
            if (pendingHandlers.size() > 0) {
                handler = pendingHandlers.get(0);
                if (null != handler) {
                    pendingHandlers.remove(0);
                }
            }
        }
        return handler;
    }

}
