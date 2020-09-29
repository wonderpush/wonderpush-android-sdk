package com.wonderpush.sdk;

import android.net.Uri;
import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;

public class WonderPushMeasurementsApiClient {
    private static final String TAG = WonderPushMeasurementsApiClient.class.getSimpleName();
    private static final OkHttpClient sClient = new OkHttpClient();
    public interface Handler {
        void onComplete(@Nullable Object result, int status, @Nullable Throwable error);
    }

    public static void post(String path, JSONObject bodyParam, Handler handler) {

        ApiClient.HttpMethod method = ApiClient.HttpMethod.POST;
        String resource = path.startsWith("/") ? path : "/" + path;
        String url = String.format("%s%s", WonderPush.MEASUREMENTS_API_URL, resource);
        String contentType = "application/x-www-form-urlencoded";
        ApiClient.Params params = new ApiClient.Params();
        params.add("body", bodyParam.toString());
        params.add("clientId", WonderPush.getClientId());
        params.add("devicePlatform", "Android");
        if (WonderPushConfiguration.getUserId() != null) {
            params.add("userId", WonderPushConfiguration.getUserId());
        }
        params.add("deviceId", WonderPushConfiguration.getDeviceId());
        ApiClient.BasicNameValuePair authorizationHeader = ApiClient.Request.getAuthorizationHeader(method, Uri.parse(url), params);
        WonderPush.safeDefer(new Runnable() {
            @Override
            public void run() {
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .header("Content-Type", contentType)
                        .post(params.getFormBody());
                if (authorizationHeader != null) {
                    requestBuilder.header(authorizationHeader.getName(), authorizationHeader.getValue());
                }
                sClient.newCall(requestBuilder.build())
                        .enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.w(TAG, String.format("Request the measurements API %s failed", path), e);
                                if (handler != null) handler.onComplete(null, Integer.MAX_VALUE, e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
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
            }
        }, 1);
    }
}
