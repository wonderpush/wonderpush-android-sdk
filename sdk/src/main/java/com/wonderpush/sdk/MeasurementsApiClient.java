package com.wonderpush.sdk;

import android.net.Uri;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;

public class MeasurementsApiClient {
    private static final String TAG = MeasurementsApiClient.class.getSimpleName();
    private static final okhttp3.OkHttpClient sClient = new okhttp3.OkHttpClient();
    public static void execute(Request request) {

        ApiClient.HttpMethod method = request.getMethod();
        String resource = request.getResource();
        String url = String.format("%s%s", WonderPush.MEASUREMENTS_API_URL, resource);
        String contentType = "application/x-www-form-urlencoded";
        final Request.Params params = request.getParams() != null ? request.getParams() : new Request.Params();
        params.add("clientId", WonderPush.getClientId());
        params.add("devicePlatform", "Android");
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
            sClient.newCall(requestBuilder.build())
                    .enqueue(new okhttp3.Callback() {
                        @Override
                        public void onFailure(okhttp3.Call call, IOException e) {
                            Log.w(TAG, String.format("Request the measurements API %s failed", resource), e);
                            if (request.getHandler() != null) {
                                request.getHandler().onFailure(e, null);
                            }
                        }

                        @Override
                        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
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
                            Log.d(TAG, String.format("Request the measurements API %s complete. Payload: %s", resource, params.toString()));
                            if (request.getHandler() != null) {
                                request.getHandler().onSuccess(response.code(), new Response(responseJson));
                            }
                        }
                    });

        }, 1);
    }
}
