package com.wonderpush.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

/**
 * The interface of all methods that can require user consent.
 */
interface IWonderPush {

    // Called when switching to this implementation
    void _activate();
    // Called when switching away from this implementation
    void _deactivate();

    String getAccessToken();
    String getDeviceId();
    String getInstallationId();
    String getPushToken();

    boolean getNotificationEnabled();
    void setNotificationEnabled(boolean status);

    JSONObject getInstallationCustomProperties();
    void putInstallationCustomProperties(JSONObject customProperties);

    void trackEvent(String type);
    void trackEvent(String type, JSONObject customData);

}
