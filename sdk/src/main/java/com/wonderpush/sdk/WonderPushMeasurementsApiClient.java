package com.wonderpush.sdk;

import android.net.Uri;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;

public class WonderPushMeasurementsApiClient {
    private static final String TAG = WonderPushMeasurementsApiClient.class.getSimpleName();
    private static final AsyncHttpClient sClient = new AsyncHttpClient(); // to allow any HTTPS certificate use: new AsyncHttpClient(true, 80, 443);
    public interface Handler {
        void onComplete(@Nullable Object result, int status, @Nullable Throwable error);
    }

    public static void post(String path, JSONObject bodyParam, Handler handler) {

        WonderPushRestClient.HttpMethod method = WonderPushRestClient.HttpMethod.POST;
        String resource = path.startsWith("/") ? path : "/" + path;
        String url = String.format("%s%s", WonderPush.MEASUREMENTS_API_URL, resource);
        String contentType = "application/x-www-form-urlencoded";
        RequestParams params = new RequestParams();
        params.add("body", bodyParam.toString());
        params.add("clientId", WonderPush.getClientId());
        params.add("devicePlatform", "Android");
        if (WonderPushConfiguration.getUserId() != null) {
            params.add("userId", WonderPushConfiguration.getUserId());
        }
        params.add("deviceId", WonderPushConfiguration.getDeviceId());
        BasicHeader authorizationHeader = WonderPushRestClient.Request.getAuthorizationHeader(method, Uri.parse(url), params);
        BasicHeader[] headers = authorizationHeader != null ? new BasicHeader[1] : new BasicHeader[0];
        if (authorizationHeader != null) headers[0] = authorizationHeader;
        WonderPush.safeDefer(new Runnable() {
            @Override
            public void run() {
                try {
                    sClient.post(null, url, headers, params.getEntity(null), contentType, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                            Log.d(TAG, String.format("Request the measurements API %s complete. Payload: %s", path, bodyParam.toString()));
                            if (handler != null) handler.onComplete(response, statusCode, null);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            Log.d(TAG, String.format("Request the measurements API %s complete. Payload: %s", path, bodyParam.toString()));
                            if (handler != null) handler.onComplete(response, statusCode, null);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            Log.w(TAG, String.format("Request the measurements API %s failed", path), throwable);
                            if (handler != null) handler.onComplete(null, statusCode, throwable);
                        }
                    });
                } catch (IOException e) {
                    if (handler != null) handler.onComplete(null, 0, e);
                }
            }
        }, 1);
    }
}
