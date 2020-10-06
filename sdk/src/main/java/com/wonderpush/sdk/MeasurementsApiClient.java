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
    public interface Handler {
        void onComplete(@Nullable Object result, int status, @Nullable Throwable error);
    }

    public static void post(String path, JSONObject bodyParam, Handler handler) {

        ApiClient.HttpMethod method = ApiClient.HttpMethod.POST;
        String resource = path.startsWith("/") ? path : "/" + path;
        String url = String.format("%s%s", WonderPush.MEASUREMENTS_API_URL, resource);
        String contentType = "application/x-www-form-urlencoded";
        com.wonderpush.sdk.Request.Params params = new com.wonderpush.sdk.Request.Params();
        params.add("body", bodyParam.toString());
        params.add("clientId", WonderPush.getClientId());
        params.add("devicePlatform", "Android");
        if (WonderPushConfiguration.getUserId() != null) {
            params.add("userId", WonderPushConfiguration.getUserId());
        }
        params.add("deviceId", WonderPushConfiguration.getDeviceId());
        Request.BasicNameValuePair authorizationHeader = com.wonderpush.sdk.Request.getAuthorizationHeader(method, Uri.parse(url), params);
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
                            Log.w(TAG, String.format("Request the measurements API %s failed", path), e);
                            if (handler != null) handler.onComplete(null, Integer.MAX_VALUE, e);
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
                                if (handler != null) handler.onComplete(null, response.code(), e);
                                return;
                            }
                            Log.d(TAG, String.format("Request the measurements API %s complete. Payload: %s", path, bodyParam.toString()));
                            if (handler != null) handler.onComplete(responseJson, response.code(), null);
                        }
                    });

        }, 1);
    }
}
