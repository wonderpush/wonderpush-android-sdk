package com.wonderpush.sdk;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
    protected static void requestForUser(String userId, HttpMethod method, String resource, Params params, ResponseHandler responseHandler) {
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
    protected static void get(String resource, Params params, ResponseHandler responseHandler) {
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
    protected static void post(String resource, Params params, ResponseHandler responseHandler) {
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
    protected static void postEventually(String resource, Params params) {
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
    protected static void put(String resource, Params params, ResponseHandler responseHandler) {
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
        Params params = request.getParams();
        if (null == params) {
            params = new Params();
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
                BasicNameValuePair authorizationHeader = request.getAuthorizationHeader();

                String url = WonderPushUriHelper.getAbsoluteUrl(request.getResource());
                WonderPush.logDebug("requesting url: " + request.getMethod() + " " + url + "?" + request.getParams().getURLEncodedString());
                // TODO: support other contentTypes such as "application/json"
                String contentType = "application/x-www-form-urlencoded";

                // Handler
                final ResponseHandler handler = request.getHandler();
                final long sendDate = SystemClock.elapsedRealtime();
                Callback jsonHandler = new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        WonderPush.setNetworkAvailable(false);
                        if (handler != null) {
                            handler.onFailure(e, new Response((JSONObject)null));
                        }
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
                            if (handler != null) {
                                handler.onFailure(e, new Response(responseString));
                            }
                            return;
                        }

                        syncTime(responseJson);
                        declareConfigVersion(responseJson);
                        if (!response.isSuccessful()) {
                            WonderPush.logError("Error answer: " + response.code() + " headers: " + response.headers().toString() + " response: " + responseString);
                            WonderPush.logDebug("Request Error: " + responseString);
                            if (handler != null) {
                                handler.onFailure(null, new Response(responseJson));
                            }
                        } else {
                            if (handler != null) {
                                handler.onSuccess(response.code(), new Response(responseJson));
                            }
                        }

                    }

                    private void declareConfigVersion(JSONObject data) {
                        if (data == null || !data.has("_configVersion") || data.isNull("_configVersion")) return;
                        String version = data.optString("_configVersion", Long.toString(data.optLong("_configVersion", 0)));
                        if (version != null && WonderPush.remoteConfigManager != null) {
                            WonderPush.remoteConfigManager.declareVersion(version);
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
                            for (BasicNameValuePair pair : request.getParams().getParamsList()) {
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
                            for (BasicNameValuePair pair : request.getParams().getParamsList()) {
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
        Params authParams = new Params();
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
                                        JSONObject custom = installation.optJSONObject("custom");
                                        WonderPush.receivedFullInstallationCustomPropertiesFromServer(custom);
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

    /**
     * A serializable object that represents a request to the WonderPush API.
     */
    protected static class Request {

        String mUserId;
        HttpMethod mMethod;
        Params mParams;
        ResponseHandler mHandler;
        String mResource;

        public Request(String userId, HttpMethod method, String resource, Params params, ResponseHandler handler) {
            mUserId = userId;
            mMethod = method;
            mParams = params;
            mHandler = handler;
            mResource = resource;
        }

        public Request(JSONObject data) throws JSONException {
            mUserId = data.has("userId") ? JSONUtil.getString(data, "userId") : WonderPushConfiguration.getUserId();
            try {
                mMethod = HttpMethod.valueOf(JSONUtil.getString(data, "method"));
            } catch (IllegalArgumentException ex) {
                mMethod = HttpMethod.values()[data.getInt("method")];
            }
            mResource = data.getString("resource");
            JSONObject paramsJson = data.getJSONObject("params");
            mParams = new Params();
            @SuppressWarnings("unchecked")
            Iterator<String> keys = paramsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                mParams.put(key, paramsJson.getString(key));
            }
        }

        public JSONObject toJSON() {
            try {
                JSONObject result = new JSONObject();
                result.put("userId", mUserId);
                result.put("method", mMethod.name());
                result.put("resource", mResource);
                JSONObject params = new JSONObject();
                if (null != mParams) {
                    for (BasicNameValuePair pair : mParams.getParamsList()) {
                        params.put(pair.getName(), pair.getValue());
                    }
                }
                result.put("params", params);
                return result;
            } catch (JSONException e) {
                WonderPush.logError("Failed to serialize job", e);
                return null;
            }
        }

        public String getUserId() {
            return mUserId;
        }

        public HttpMethod getMethod() {
            return mMethod;
        }

        public Params getParams() {
            return mParams;
        }

        public ResponseHandler getHandler() {
            return mHandler;
        }

        public String getResource() {
            return mResource;
        }

        public void setMethod(HttpMethod mMethod) {
            this.mMethod = mMethod;
        }

        public void setParams(Params mParams) {
            this.mParams = mParams;
        }

        public void setHandler(ResponseHandler mHandler) {
            this.mHandler = mHandler;
        }

        public void setResource(String resource) {
            this.mResource = resource;
        }

        @Override
        protected Object clone() {
            return new Request(mUserId, mMethod, mResource, mParams, mHandler);
        }

        /**
         * Generates X-WonderPush-Authorization header with request signature
         *
         * @return The authorization header or null for GET requests
         */
        protected BasicNameValuePair getAuthorizationHeader() {
            return getAuthorizationHeader(mMethod, Uri.parse(String.format("%s%s", WonderPush.getBaseURL(), mResource)), mParams);
        }

        protected static BasicNameValuePair getAuthorizationHeader(HttpMethod method, Uri uri, Params params) {
            try {
                StringBuilder sb = new StringBuilder();

                // Step 1: add HTTP method uppercase
                sb.append(method.name().toUpperCase());
                sb.append('&');

                // Step 2: add the URI
                // Query string is stripped from resource
                sb.append(encode(String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getEncodedPath())));

                // Step 3: add URL encoded parameters
                sb.append('&');

                // Params from the URL
                List<BasicNameValuePair> unencodedParams = new ArrayList<>();
                Params queryStringParams = QueryStringParser.getRequestParams(uri.getQuery());
                if (queryStringParams != null) {
                    unencodedParams.addAll(queryStringParams.getParamsList());
                }

                // Params from the request
                if (params != null) {
                    unencodedParams.addAll(params.getParamsList());
                }

                // Encode and sort params
                List<BasicNameValuePair> encodedParams = new ArrayList<>(unencodedParams.size());
                for (BasicNameValuePair pair : unencodedParams) {
                    encodedParams.add(new BasicNameValuePair(encode(pair.getName()), encode(pair.getValue())));
                }
                Collections.sort(encodedParams, new Comparator<BasicNameValuePair>() {
                    @Override
                    public int compare(BasicNameValuePair lhs, BasicNameValuePair rhs) {
                        int rtn = lhs.getName().compareTo(rhs.getName());
                        if (rtn == 0) {
                            rtn = lhs.getValue().compareTo(rhs.getValue());
                        }
                        return rtn;
                    }
                });

                // Append to the clear signature
                boolean first = true;
                for (BasicNameValuePair pair : encodedParams) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append("%26");
                    }
                    sb.append(encode(String.format("%s=%s", pair.getName(), pair.getValue())));
                }

                // Step 4: add body
                sb.append('&');
                // TODO: add the body here when we support other content types than application/x-www-form-urlencoded

                // Final step: Hash and format header
                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec secret = new SecretKeySpec(WonderPush.getClientSecret().getBytes("UTF-8"), mac.getAlgorithm());
                mac.init(secret);
                byte[] digest = mac.doFinal(sb.toString().getBytes());
                String sig = Base64.encodeToString(digest, Base64.DEFAULT).trim();
                String encodedSig = encode(sig.trim());
                return new BasicNameValuePair("X-WonderPush-Authorization", String.format("WonderPush sig=\"%s\", meth=\"0\"", encodedSig));
            } catch (Exception e) {
                Log.e(TAG, "Could not generate signature", e);
                return null;
            }
        }

        protected static String encode(String s) throws UnsupportedEncodingException {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        }

        @Override
        public String toString() {
            return "" + mMethod + " " + mResource + "?" + mParams;
        }

    }

    public static class BasicNameValuePair {
        private String name;
        private String value;
        public BasicNameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * A class that handles the parameter to provide to either an api call or a view.
     */
    static class Params implements Parcelable {

        public static final String TAG = "RequestParams";

        public Params(Parcel in) throws JSONException {
            JSONObject json = new JSONObject(in.readString());
            Iterator<?> it = json.keys();
            String key;
            while (it.hasNext()) {
                key = (String) it.next();
                this.put(key, json.optString(key));
            }
        }

        public String getURLEncodedString() {
            Iterator<BasicNameValuePair> iter = getParamsList().iterator();
            StringBuffer buffer = new StringBuffer();
            while (iter.hasNext()) {
                BasicNameValuePair pair = iter.next();
                if (pair.getName() == null || pair.getValue() == null) continue;
                try {
                    buffer.append(URLEncoder.encode(pair.getName(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                }
                buffer.append("=");
                try {
                    buffer.append(URLEncoder.encode(pair.getValue(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                }
                if (iter.hasNext()) buffer.append("&");
            }
            return buffer.toString();
        }

        public JSONObject toJSONObject() {
            JSONObject result = new JSONObject();
            List<BasicNameValuePair> params = getParamsList();
            for (BasicNameValuePair parameter : params) {
                try {
                    result.put(parameter.getName(), parameter.getValue());
                } catch (JSONException e) {
                    WonderPush.logError("Failed to add parameter " + parameter, e);
                }
            }
            return result;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            destination.writeString(toJSONObject().toString());
        }

        public static final Creator<Params> CREATOR = new Creator<Params>() {
            public Params createFromParcel(Parcel in) {
                try {
                    return new Params(in);
                } catch (Exception e) {
                    Log.e(TAG, "Error while unserializing JSON from a WonderPush.RequestParams", e);
                    return null;
                }
            }

            public Params[] newArray(int size) {
                return new Params[size];
            }
        };

        protected final ConcurrentSkipListMap<String, String> urlParams = new ConcurrentSkipListMap<String, String>();
        protected final ConcurrentSkipListMap<String, Object> urlParamsWithObjects = new ConcurrentSkipListMap<String, Object>();

        /**
         * Constructs a new empty {@code RequestParams} instance.
         */
        public Params() {
            this((Map<String, String>) null);
        }

        /**
         * Constructs a new RequestParams instance containing the key/value string params from the
         * specified map.
         *
         * @param source the source key/value string map to add.
         */
        public Params(Map<String, String> source) {
            if (source != null) {
                for (Map.Entry<String, String> entry : source.entrySet()) {
                    put(entry.getKey(), entry.getValue());
                }
            }
        }

        /**
         * Constructs a new RequestParams instance and populate it with a single initial key/value
         * string param.
         *
         * @param key   the key name for the intial param.
         * @param value the value string for the initial param.
         */
        public Params(final String key, final String value) {
            this(new HashMap<String, String>() {{
                put(key, value);
            }});
        }

        /**
         * Adds a key/value string pair to the request.
         *
         * @param key   the key name for the new param.
         * @param value the value string for the new param.
         */
        public void put(String key, String value) {
            if (key != null && value != null) {
                urlParams.put(key, value);
            }
        }

        /**
         * Adds param with non-string value (e.g. Map, List, Set).
         *
         * @param key   the key name for the new param.
         * @param value the non-string value object for the new param.
         */
        public void put(String key, Object value) {
            if (key != null && value != null) {
                urlParamsWithObjects.put(key, value);
            }
        }

        /**
         * Adds string value to param which can have more than one value.
         *
         * @param key   the key name for the param, either existing or new.
         * @param value the value string for the new param.
         */
        public void add(String key, String value) {
            if (key != null && value != null) {
                Object params = urlParamsWithObjects.get(key);
                if (params == null) {
                    // Backward compatible, which will result in "k=v1&k=v2&k=v3"
                    params = new HashSet<String>();
                    this.put(key, params);
                }
                if (params instanceof List) {
                    ((List<Object>) params).add(value);
                } else if (params instanceof Set) {
                    ((Set<Object>) params).add(value);
                }
            }
        }

        /**
         * Removes a parameter from the request.
         *
         * @param key the key name for the parameter to remove.
         */
        public void remove(String key) {
            urlParams.remove(key);
            urlParamsWithObjects.remove(key);
        }

        /**
         * Check if a parameter is defined.
         *
         * @param key the key name for the parameter to check existence.
         * @return Boolean
         */
        public boolean has(String key) {
            return urlParams.get(key) != null ||
                    urlParamsWithObjects.get(key) != null;
        }

        protected List<BasicNameValuePair> getParamsList() {
            List<BasicNameValuePair> lparams = new LinkedList<BasicNameValuePair>();

            for (ConcurrentSkipListMap.Entry<String, String> entry : urlParams.entrySet()) {
                lparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            lparams.addAll(getParamsList(null, urlParamsWithObjects));

            return lparams;
        }

        protected FormBody getFormBody() {
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            for (BasicNameValuePair pair : getParamsList()) {
                formBodyBuilder.add(pair.getName(), pair.getValue());
            }
            return formBodyBuilder.build();
        }

        private List<BasicNameValuePair> getParamsList(String key, Object value) {
            List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
            if (value instanceof Map) {
                Map map = (Map) value;
                List list = new ArrayList<Object>(map.keySet());
                // Ensure consistent ordering in query string
                if (list.size() > 0 && list.get(0) instanceof Comparable) {
                    Collections.sort(list);
                }
                for (Object nestedKey : list) {
                    if (nestedKey instanceof String) {
                        Object nestedValue = map.get(nestedKey);
                        if (nestedValue != null) {
                            params.addAll(getParamsList(key == null ? (String) nestedKey : String.format(Locale.US, "%s[%s]", key, nestedKey),
                                    nestedValue));
                        }
                    }
                }
            } else if (value instanceof List) {
                List list = (List) value;
                int listSize = list.size();
                for (int nestedValueIndex = 0; nestedValueIndex < listSize; nestedValueIndex++) {
                    params.addAll(getParamsList(String.format(Locale.US, "%s[%d]", key, nestedValueIndex), list.get(nestedValueIndex)));
                }
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                int arrayLength = array.length;
                for (int nestedValueIndex = 0; nestedValueIndex < arrayLength; nestedValueIndex++) {
                    params.addAll(getParamsList(String.format(Locale.US, "%s[%d]", key, nestedValueIndex), array[nestedValueIndex]));
                }
            } else if (value instanceof Set) {
                Set set = (Set) value;
                for (Object nestedValue : set) {
                    params.addAll(getParamsList(key, nestedValue));
                }
            } else {
                params.add(new BasicNameValuePair(key, value.toString()));
            }
            return params;
        }
    }
}
