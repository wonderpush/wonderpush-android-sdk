package com.wonderpush.sdk;

import android.location.Location;

/**
 * A static helper class that will add parameters to a {@link ApiClient.Params} object depending on the resource
 * path and user configuration of the {@link WonderPush} object.
 */
class WonderPushRequestParamsDecorator {

    protected static void decorate(String resource, ApiClient.Params params) {
        // Always add the sdk version
        addParameterIfAbsent(params, "sdkVersion", WonderPush.SDK_VERSION);
    }

    private static void addParameterIfAbsent(ApiClient.Params params, String paramName, String paramValue) {
        if (null == params || null == paramName || null == paramValue)
            return;

        if (params.has(paramName))
            return;

        params.put(paramName, paramValue);
    }

    private static void addParameterIfAbsent(ApiClient.Params params, String paramName, Location paramValue) {
        if (null == paramValue)
            return;

        addParameterIfAbsent(params, paramName, "" + paramValue.getLatitude() + "," + paramValue.getLongitude());
    }

}
