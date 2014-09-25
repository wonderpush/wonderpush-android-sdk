package com.wonderpush.sdk;

import android.location.Location;

/**
 * A static helper class that will add parameters to a {@link WonderPush.RequestParams} object depending on the resource
 * path and user configuration of the {@link WonderPush} object.
 */
class WonderPushRequestParamsDecorator {

    protected static void decorate(String resource, WonderPush.RequestParams params) {
        // Always add lang
        addParameterIfAbsent(params, "lang", WonderPush.getLang());

        // Always add location
        addParameterIfAbsent(params, "location", WonderPush.getLocation());

        // Always add the sdk version
        addParameterIfAbsent(params, "sdkVersion", WonderPush.SDK_VERSION);

        // Add the SID for web resources
        if (resource.startsWith("/web"))
            params.put("sid", WonderPushConfiguration.getSID());
    }

    private static void addParameterIfAbsent(WonderPush.RequestParams params, String paramName, String paramValue) {
        if (null == params || null == paramName || null == paramValue)
            return;

        if (params.has(paramName))
            return;

        params.put(paramName, paramValue);
    }

    private static void addParameterIfAbsent(WonderPush.RequestParams params, String paramName, Location paramValue) {
        if (null == paramValue)
            return;

        addParameterIfAbsent(params, paramName, "" + paramValue.getLatitude() + "," + paramValue.getLongitude());
    }

}
