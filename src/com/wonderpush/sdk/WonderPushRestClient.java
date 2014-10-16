package com.wonderpush.sdk;

import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.Header;
import org.apache.http.NoHttpResponseException;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.wonderpush.sdk.WonderPush.Response;

/**
 * A REST client that lets you hit the WonderPush REST server.
 */
class WonderPushRestClient {

    private static final String TAG = WonderPushRestClient.class.getSimpleName();

    private enum HttpMethod {
        GET, PUT, POST, DELETE
    }

    private static final int RETRY_INTERVAL_BAD_AUTH = 1 * 1000; // in milliseconds
    private static final int RETRY_INTERVAL = 30 * 1000; // in milliseconds
    private static final int RETRY_INTERVAL_NETWORK_ISSUE = 60 * 1000; // in milliseconds
    private static boolean sIsFetchingAnonymousAccessToken = false;
    private static List<WonderPush.ResponseHandler> sPendingHandlers = new ArrayList<WonderPush.ResponseHandler>();

    private static AsyncHttpClient sClient = new AsyncHttpClient();

    /**
     * A GET request
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            AsyncHttpClient request parameters
     * @param responseHandler
     *            An AsyncHttpClient response handler
     */
    protected static void get(String resource, WonderPush.RequestParams params, WonderPush.ResponseHandler responseHandler) {
        requestAuthenticated(new Request(HttpMethod.GET, resource, params, responseHandler));
    }

    /**
     * A POST request
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            AsyncHttpClient request parameters
     * @param responseHandler
     *            An AsyncHttpClient response handler
     */
    protected static void post(String resource, WonderPush.RequestParams params, WonderPush.ResponseHandler responseHandler) {
        requestAuthenticated(new Request(HttpMethod.POST, resource, params, responseHandler));
    }

    /**
     * A POST request that is guaranteed to be executed when a network connection
     * is present, surviving application reboot. The responseHandler will be
     * called only if the network is present when the request is first run.
     *
     * @param resource
     * @param params
     * @param responseHandler
     */
    protected static void postEventually(String resource, WonderPush.RequestParams params,
            final WonderPush.ResponseHandler responseHandler) {

        // Create a request
        final Request request = new Request(HttpMethod.POST, resource, params, null);

        // Wrap the provided handler with ours
        request.setHandler(new WonderPush.ResponseHandler() {
            @Override
            public void onFailure(Throwable e, Response errorResponse) {
                // Post to vault on network error
                if (e instanceof NoHttpResponseException
                        || e instanceof UnknownHostException
                        || e instanceof SocketException) {
                    // retry later
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                WonderPushRequestVault.getDefaultVault().put(request);
                            } catch (JSONException e1) {
                                Log.e(TAG, "Could not save request to vault", e1);
                            }
                        }
                    }, RETRY_INTERVAL_NETWORK_ISSUE);
                    return;
                }

                // Forward to original handler otherwise
                if (null != responseHandler) {
                    responseHandler.onFailure(e, errorResponse);
                }
            }

            @Override
            public void onSuccess(Response response) {
                if (null != responseHandler) {
                    responseHandler.onSuccess(response);
                }
            }

            @Override
            public void onSuccess(int statusCode, Response response) {
                if (null != responseHandler) {
                    responseHandler.onSuccess(statusCode, response);
                }
            }
        });

        requestAuthenticated(request);
    }

    /**
     * A PUT request
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            AsyncHttpClient request parameters
     * @param responseHandler
     *            An AsyncHttpClient response handler
     */
    protected static void put(String resource, WonderPush.RequestParams params, WonderPush.ResponseHandler responseHandler) {
        requestAuthenticated(new Request(HttpMethod.PUT, resource, params, responseHandler));
    }

    /**
     * A DELETE request
     *
     * @param resource
     *            The resource path, starting with /
     * @param params
     *            AsyncHttpClient request parameters
     * @param responseHandler
     *            An AsyncHttpClient response handler
     */
    protected static void delete(String resource, WonderPush.ResponseHandler responseHandler) {
        requestAuthenticated(new Request(HttpMethod.DELETE, resource, null, responseHandler));
    }

    /**
     * If no access token is found in the user's preferences, fetch an anonymous access token.
     *
     * @param onFetchedHandler
     *            A handler called if a request to fetch an access token has been
     *            executed successfully, never called if retreived from cache
     * @return Whether or not a request has been executed to fetch an anonymous
     *         access token (true fetching, false retrived from local cache)
     */
    protected static boolean fetchAnonymousAccessTokenIfNeeded(WonderPush.ResponseHandler onFetchedHandler) {
        if (null == WonderPushConfiguration.getAccessToken()) {
            fetchAnonymousAccessToken(onFetchedHandler);
            return true;
        }
        return false;
    }

    /**
     * If no access token is found in the user's preferences, fetch an anonymous access token.
     */
    protected static void fetchAnonymousAccessTokenIfNeeded() {
        fetchAnonymousAccessTokenIfNeeded(null);
    }

    /**
     * Runs the specified request and ensure a valid access token is fetched if
     * necessary beforehand, or afterwards (and re-run the request) if the request
     * fails for auth reasons.
     *
     * @param request
     */
    protected static void requestAuthenticated(final Request request) {
        if (null == request) {
            return;
        }

        String accessToken = WonderPushConfiguration.getAccessToken();

        // User is authenticated
        if (accessToken == null) {
            // User is not authenticated, request a token
            fetchAnonymousAccessTokenAndRunRequest(request);
            return;
        }

        // Add the access token to the params
        WonderPush.RequestParams params = request.getParams();
        if (null == params) {
            params = new WonderPush.RequestParams();
            request.setParams(params);
        }

        params.remove("accessToken");
        params.put("accessToken", accessToken);

        // Wrap the request handler with our own
        WonderPush.ResponseHandler wrapperHandler = new WonderPush.ResponseHandler() {
            @Override
            public void onSuccess(int status, WonderPush.Response response) {
                if (request.getHandler() != null) {
                    request.getHandler().onSuccess(status, response);
                }
            }

            @Override
            public void onFailure(Throwable e, WonderPush.Response errorResponse) {
                Log.e(TAG, "Request failed", e);
                if (errorResponse != null && WonderPush.ERROR_INVALID_ACCESS_TOKEN == errorResponse.getErrorCode()) {
                    // null out the access token
                    WonderPushConfiguration.setAccessToken(null);
                    WonderPushConfiguration.setSID(null);
                    WonderPushConfiguration.setInstallationId(null);

                    // retry later now
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            requestAuthenticated(request);
                        }
                    }, RETRY_INTERVAL_BAD_AUTH);
//                } else if (e instanceof ConnectTimeoutException) {
//                    // retry later
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            requestAuthenticated(request);
//                        }
//                    }, RETRY_INTERVAL_NETWORK_ISSUE);
                } else {
                    if (request.getHandler() != null) {
                        request.getHandler().onFailure(e, errorResponse);
                    }
                }
            }

            @Override
            public void onSuccess(Response response) {
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
     * Thin wrapper to the {@link AsyncHttpClient} library.
     *
     * @param request
     */
    private static void request(final Request request) {
        if (null == request) {
            Log.e(TAG, "Request with null request.");
            return;
        }

        // Decorate parameters
        WonderPushRequestParamsDecorator.decorate(request.getResource(), request.getParams());

        // Generate signature
        Header authorizationHeader = request.getAuthorizationHeader();

        // Headers
        Header[] headers = null;
        if (null != authorizationHeader) {
            headers = new Header[1];
            headers[0] = authorizationHeader;
        }

        // Handler
        JsonHttpResponseHandler jsonHandler = null;
        if (null != request.getHandler()) {
            jsonHandler = new JsonHttpResponseHandler() {
                @Override
                public void onFailure(Throwable e, JSONObject data) {
                    if (data != null) {
                        WonderPush.logDebug("Requesting Error: " + data);
                        WonderPush.setNetworkAvailable(true);
                        request.getHandler().onFailure(e, new WonderPush.Response(data));
                    } else {
                        WonderPush.setNetworkAvailable(false);
                        request.getHandler().onFailure(e, null);
                    }
                }

                @Override
                public void onFailure(Throwable e, String data) {
                    WonderPush.setNetworkAvailable(false);
                    request.getHandler().onFailure(e, null);
                }

                @Override
                public void onSuccess(int statusCode, JSONObject data) {
                    WonderPush.setNetworkAvailable(true);
                    request.getHandler().onSuccess(statusCode, new WonderPush.Response(data));
                }
            };
        }

        String url = WonderPushUriHelper.getAbsoluteUrl(request.getResource());
        WonderPush.logDebug("requesting url: " + request.getMethod() + " " + url + "?" + request.getParams().getURLEncodedString());
        // TODO: support other contentTypes such as "application/json"
        String contentType = "application/x-www-form-urlencoded";
        switch (request.getMethod()) {
            case GET:
                sClient.get(null, url, headers, request.getParams(), jsonHandler);
                break;
            case PUT:
                sClient.put(null, url, headers,
                        request.getParams() != null ? request.getParams().getEntity() : null,
                        contentType, jsonHandler);
                break;
            case POST:
                sClient.post(null, url, headers, request.getParams(), contentType, jsonHandler);
                break;
            case DELETE:
                sClient.delete(null, url, headers, jsonHandler);
                break;
        }
    }

    protected static void fetchAnonymousAccessToken(final WonderPush.ResponseHandler handler) {
        fetchAnonymousAccessToken(handler, 0);
    }

    protected static void fetchAnonymousAccessToken(final WonderPush.ResponseHandler handler, final int nbRetries) {
        if (sIsFetchingAnonymousAccessToken) {
            queueHandler(handler);
            return;
        }
        sIsFetchingAnonymousAccessToken = true;
        WonderPush.RequestParams authParams = new WonderPush.RequestParams();
        authParams.put("clientId", WonderPush.getClientId());
        authParams.put("devicePlatform", "Android");
        authParams.put("deviceModel", WonderPush.getDeviceModel());
        String udid = WonderPush.getUDID();
        if (null != udid) {
            authParams.put("deviceId", udid);
        }
        String userId = WonderPushConfiguration.getUserId();
        if (null != userId) {
            authParams.put("userId", userId);
        }

        String resource = "/authentication/accessToken";

        request(new Request(HttpMethod.POST, resource, authParams,
                new WonderPush.ResponseHandler() {
                    @Override
                    public void onFailure(Throwable e, Response errorResponse) {
                        if (nbRetries <= 0) {
                            Log.e(TAG, "Error request anonymous access token (aborting): " + (errorResponse != null ? errorResponse.getJSONObject().toString() : "null error response, aborting"), e);
                            if (errorResponse != null && WonderPush.ERROR_INVALID_CREDENTIALS == errorResponse.getErrorCode()) {
                                Log.e(TAG, "Check your clientId/clientSecret couple");
                            }

                            sIsFetchingAnonymousAccessToken = false;
                            if (null != handler) {
                                handler.onFailure(e, errorResponse);
                            }
                            WonderPush.ResponseHandler chainedHandler = null;
                            while ((chainedHandler = dequeueHandler()) != null) {
                                chainedHandler.onFailure(e, errorResponse);
                            }
                            return;
                        }
                        Log.e(TAG, "Error request anonymous access token (retrying: " + nbRetries + "): " + (errorResponse != null ? errorResponse.getJSONObject().toString() : "null error response, retrying"), e);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                WonderPush.logDebug("re-requesting access token!");

                                sIsFetchingAnonymousAccessToken = false;
                                fetchAnonymousAccessToken(handler, nbRetries - 1);
                            }
                        }, RETRY_INTERVAL);
                    }

                    @Override
                    public void onSuccess(int statusCode, Response response) {
                        // Parse response
                        JSONObject json = response.getJSONObject();
                        if (json.has("token") && json.has("data")) {
                            String token = json.optString("token");
                            JSONObject data = json.optJSONObject("data");
                            if (data.has("installationId")) {
                                String sid = data.optString("sid");
                                String installationId = data.optString("installationId");
                                String userId = data.optString("userId");

                                // Store access token
                                WonderPushConfiguration.setAccessToken(token);
                                WonderPushConfiguration.setSID(sid);
                                WonderPushConfiguration.setInstallationId(installationId);
                                WonderPushConfiguration.setUserId(userId);
                                sIsFetchingAnonymousAccessToken = false;

                                // call handlers
                                if (null != handler) {
                                    handler.onSuccess(statusCode, response);
                                }
                                WonderPush.ResponseHandler chainedHandler = null;
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
        fetchAnonymousAccessToken(new WonderPush.ResponseHandler() {
            @Override
            public void onSuccess(Response response) {
                requestAuthenticated(request);
            }

            @Override
            public void onFailure(Throwable e, Response errorResponse) {
            }
        });
    }

    private static void queueHandler(WonderPush.ResponseHandler handler) {
        if (null == handler) {
            return;
        }

        synchronized (sPendingHandlers) {
            sPendingHandlers.add(handler);
        }
    }

    private static WonderPush.ResponseHandler dequeueHandler() {
        WonderPush.ResponseHandler handler = null;
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
    protected static class Request implements Cloneable {

        HttpMethod mMethod;
        WonderPush.RequestParams mParams;
        WonderPush.ResponseHandler mHandler;
        String mResource;

        public Request(HttpMethod method, String resource, WonderPush.RequestParams params, WonderPush.ResponseHandler handler) {
            mMethod = method;
            mParams = params;
            mHandler = handler;
            mResource = resource;
        }

        public Request(JSONObject data) throws JSONException {
            mMethod = HttpMethod.values()[data.getInt("method")];
            mResource = data.getString("resource");
            JSONObject paramsJson = data.getJSONObject("params");
            mParams = new WonderPush.RequestParams();
            @SuppressWarnings("unchecked")
            Iterator<String> keys = paramsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                mParams.put(key, paramsJson.getString(key));
            }
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("method", mMethod.ordinal());
            result.put("resource", mResource);
            JSONObject params = new JSONObject();
            if (null != mParams) {
                for (String key : mParams.getParamNames()) {
                    params.put(key, mParams.getParamValue(key));
                }
            }
            result.put("params", params);
            return result;
        }

        public HttpMethod getMethod() {
            return mMethod;
        }

        public WonderPush.RequestParams getParams() {
            return mParams;
        }

        public WonderPush.ResponseHandler getHandler() {
            return mHandler;
        }

        public String getResource() {
            return mResource;
        }

        public void setMethod(HttpMethod mMethod) {
            this.mMethod = mMethod;
        }

        public void setParams(WonderPush.RequestParams mParams) {
            this.mParams = mParams;
        }

        public void setHandler(WonderPush.ResponseHandler mHandler) {
            this.mHandler = mHandler;
        }

        public void setResource(String resource) {
            this.mResource = resource;
        }

        @Override
        protected Object clone() {
            return new Request(mMethod, mResource, mParams, mHandler);
        }

        /**
         * Generates X-WonderPush-Authorization header with request signature
         *
         * @return The authorization header or null for GET requests
         */
        protected BasicHeader getAuthorizationHeader() {
            try {
                StringBuilder sb = new StringBuilder();

                // Step 1: add HTTP method uppercase
                switch (mMethod) {
                    case POST:
                        sb.append("POST");
                        break;
                    case PUT:
                        sb.append("PUT");
                        break;
                    case GET:
                        // No authorization header for GET requests
                        return null;
                    case DELETE:
                        sb.append("DELETE");
                        break;
                }

                sb.append('&');

                // Step 2: add the URI
                Uri uri = Uri.parse(mResource);

                // Query string is stripped from resource
                sb.append(encode(String.format("%s%s", WonderPush.getBaseURL(),
                        uri.getEncodedPath())));

                // Step 3: add URL encoded parameters
                sb.append('&');
                TreeSet<String> paramNames = new TreeSet<String>();

                // Params from the URL
                WonderPush.RequestParams queryStringParams = QueryStringParser.getRequestParams(uri.getQuery());
                if (queryStringParams != null) {
                    paramNames.addAll(queryStringParams.getParamNames());
                }

                // Params from the request
                if (mParams != null) {
                    paramNames.addAll(mParams.getParamNames());
                }

                if (paramNames.size() > 0) {
                    String last = paramNames.last();
                    for (String paramName : paramNames) {
                        String paramValue = null;

                        if (null != mParams) {
                            paramValue = mParams.getParamValue(paramName);
                        }

                        if (paramValue == null && queryStringParams != null) {
                            paramValue = queryStringParams.getParamValue(paramName);
                        }

                        sb.append(encode(String.format("%s=%s", encode(paramName), encode(paramValue))));
                        if (!last.equals(paramName)) {
                            sb.append("%26");
                        }
                    }
                }

                // Step 4: add body
                sb.append('&');
                // TODO: add the body here when we support other content types than application/x-www-form-urlencoded
                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec secret = new SecretKeySpec(WonderPush.getClientSecret().getBytes("UTF-8"), mac.getAlgorithm());
                mac.init(secret);
                byte[] digest = mac.doFinal(sb.toString().getBytes());
                String sig = Base64.encodeToString(digest, Base64.DEFAULT).trim();
                String encodedSig = encode(sig.trim());
                BasicHeader result = new BasicHeader("X-WonderPush-Authorization", String.format("WonderPush sig=\"%s\", meth=\"0\"", encodedSig));

                return result;
            } catch (Exception e) {
                Log.e(TAG, "Could not generate signature", e);
                return null;
            }
        }

        private static String encode(String s) throws UnsupportedEncodingException {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        }

        @Override
        public String toString() {
            String method = null;
            switch (mMethod) {
                case POST:
                    method = "POST";
                    break;
                case PUT:
                    method = "PUT";
                    break;
                case GET:
                    method = "GET";
                    break;
                case DELETE:
                    method = "DELETE";
            }
            return String.format("%s %s", method, mResource);
        }

    }

}
