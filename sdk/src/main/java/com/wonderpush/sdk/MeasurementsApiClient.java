package com.wonderpush.sdk;

import android.net.TrafficStats;
import android.net.Uri;
import android.os.Process;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class MeasurementsApiClient {

    private static final String TAG = "WonderPush." + MeasurementsApiClient.class.getSimpleName();

    private static final okhttp3.OkHttpClient sClient = new okhttp3.OkHttpClient.Builder().eventListener(new okhttp3.EventListener() {
        @Override
        public void connectStart(okhttp3.Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
            TrafficStats.setThreadStatsTag(Process.myTid());
        }
    }).build();
    private static boolean disabled;

    public static void execute(Request request) {
        if (isDisabled()) {
            if (request.getHandler() != null) request.getHandler().onFailure(new Request.ClientDisabledException(), new Response("Client disabled"));
            return;
        }

        HttpMethod method = request.getMethod();
        String resource = request.getResource();
        String url = String.format("%s%s", WonderPush.MEASUREMENTS_API_URL, resource);
        String contentType = "application/x-www-form-urlencoded";

        final Request.Params params = request.getParams() != null ? request.getParams() : new Request.Params();
        params.add("clientId", WonderPush.getClientId());
        params.add("devicePlatform", "Android");
        params.add("sdkVersion", WonderPush.SDK_VERSION);
        if (WonderPushConfiguration.getUserId() != null) {
            params.add("userId", WonderPushConfiguration.getUserId());
        }
        params.add("deviceId", WonderPushConfiguration.getDeviceId());

        Request.BasicNameValuePair authorizationHeader = Request.getAuthorizationHeader(method, Uri.parse(url), params);

        WonderPush.safeDefer(() -> {
            okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                    .url(url)
                    .header("Content-Type", contentType)
                    .post(params.getFormBody());
            if (authorizationHeader != null) {
                requestBuilder.header(authorizationHeader.getName(), authorizationHeader.getValue());
            }

            WonderPush.logDebug(TAG, "Requesting: " + humanReadableRequest(method, resource, params));
            sClient.newCall(requestBuilder.build())
                    .enqueue(new SafeOkHttpCallback() {
                        @Override
                        public void onFailureSafe(okhttp3.Call call, IOException e) {
                            Log.w(TAG, "Request failed: " + humanReadableRequest(method, resource, params), e);
                            if (request.getHandler() != null) {
                                request.getHandler().onFailure(e, null);
                            }
                        }

                        @Override
                        public void onResponseSafe(okhttp3.Call call, okhttp3.Response response) throws IOException {
                            String responseString = response.body().string();
                            JSONObject responseJson = null;
                            try {
                                if (responseString != null) {
                                    responseJson = new JSONObject(responseString);
                                }
                            } catch (JSONException e) {
                                if (request.getHandler() != null) {
                                    request.getHandler().onFailure(e, null);
                                }
                                return;
                            }
                            WonderPush.logDebug(TAG, "Request successful: (" + response.code() + ") " + responseString + " (for " + humanReadableRequest(method, resource, params) + ")");

                            // Read config version
                            if (responseJson != null && responseJson.has("_configVersion") && !responseJson.isNull("_configVersion")) {
                                String version = responseJson.optString("_configVersion", Long.toString(responseJson.optLong("_configVersion", 0)));
                                if (version != null && WonderPush.getRemoteConfigManager() != null) {
                                    WonderPush.getRemoteConfigManager().declareVersion(version);
                                }
                            }

                            // Callback
                            if (request.getHandler() != null) {
                                request.getHandler().onSuccess(response.code(), new Response(responseJson));
                            }
                        }
                    });

        }, 1);
    }

    public static boolean isDisabled() {
        return disabled;
    }

    public static void setDisabled(boolean disabled) {
        MeasurementsApiClient.disabled = disabled;
    }

    private static String humanReadableRequest(HttpMethod method, String resource, Request.Params params) {
        return method + " " + resource + "?" + params.toHumanReadableString();
    }

}
