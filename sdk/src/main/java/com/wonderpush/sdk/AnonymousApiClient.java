package com.wonderpush.sdk;

public class AnonymousApiClient extends BaseApiClient {
    private static final String TAG = "WonderPush." + AnonymousApiClient.class.getSimpleName();
    private static AnonymousApiClient sInstance = new AnonymousApiClient();
    public static AnonymousApiClient getInstance() {
        return sInstance;
    }

    @Override
    protected void decorate(Request request) {
        Request.Params params = request.getParams();
        if (null == params) {
            params = new Request.Params();
            request.setParams(params);
        }
        params.remove("clientId");
        params.put("clientId", WonderPush.getClientId());
        params.remove("clientSecret");
        params.put("clientSecret", WonderPush.getClientSecret());
        params.remove("devicePlatform");
        params.put("devicePlatform", "Android");
        params.remove("deviceId");
        params.put("deviceId", WonderPushConfiguration.getDeviceId());
        params.remove("userId");
        params.put("userId", request.getUserId() != null ? request.getUserId() : "");
    }

    @Override
    protected String getTag() {
        return TAG;
    }

}
