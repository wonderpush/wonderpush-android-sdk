package com.wonderpush.sdk;

import android.location.Location;

import androidx.core.app.NotificationManagerCompat;

/**
 * A static helper class that will add parameters to a {@link Request.Params} object depending on the resource
 * path and user configuration of the {@link WonderPush} object.
 */
class WonderPushRequestParamsDecorator {

    protected static void decorate(String resource, Request.Params params) {
        // Always add the sdk version
        addParameterIfAbsent(params, "sdkVersion", WonderPush.SDK_VERSION);

        // Always add the current reachability state
        addParameterIfAbsent(params, "_reachability", computeReachability());
    }

    /**
     * Computes the current reachability state (optIn/softOptOut/optOut) live, from the same
     * inputs as {@link WonderPushImpl#refreshSubscriptionStatus()}, rather than from the last
     * synced installation state, so it can't lag behind the device's actual current state.
     */
    protected static String computeReachability() {
        if (WonderPush.getPushToken() == null) {
            return "optOut";
        }
        boolean subscribed = WonderPushConfiguration.getNotificationEnabled()
                && NotificationManagerCompat.from(WonderPush.getApplicationContext()).areNotificationsEnabled();
        return subscribed ? "optIn" : "softOptOut";
    }

    private static void addParameterIfAbsent(Request.Params params, String paramName, String paramValue) {
        if (null == params || null == paramName || null == paramValue)
            return;

        if (params.has(paramName))
            return;

        params.put(paramName, paramValue);
    }

    private static void addParameterIfAbsent(Request.Params params, String paramName, Location paramValue) {
        if (null == paramValue)
            return;

        addParameterIfAbsent(params, paramName, "" + paramValue.getLatitude() + "," + paramValue.getLongitude());
    }

}
