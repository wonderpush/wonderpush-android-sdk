package com.wonderpush.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

/**
 * Implementation of {@link IWonderPush} that does nothing but log an error and return a default value.
 *
 * This implementation is used when user consent is required and not provided.
 */
abstract class WonderPushLogErrorImpl implements IWonderPush {

    abstract protected void log(String method);

    @Override
    public void _activate() {
        // No-op
    }

    @Override
    public void _deactivate() {
        // No-op
    }

    @Override
    public String getAccessToken() {
        this.log("getAccessToken");
        return null;
    }

    @Override
    public String getDeviceId() {
        this.log("getDeviceId");
        return null;
    }

    @Override
    public String getInstallationId() {
        this.log("getInstallationId");
        return null;
    }

    @Override
    public String getPushToken() {
        this.log("getPushToken");
        return null;
    }

    @Override
    public boolean getNotificationEnabled() {
        this.log("getNotificationEnabled");
        return false;
    }

    @Override
    public void setNotificationEnabled(boolean status) {
        this.log("setNotificationEnabled");
    }

    @Override
    public JSONObject getInstallationCustomProperties() {
        this.log("getInstallationCustomProperties");
        return new JSONObject();
    }

    @Override
    public void putInstallationCustomProperties(JSONObject customProperties) {
        this.log("putInstallationCustomProperties");
    }

    @Override
    public void trackEvent(String type) {
        this.log("trackEvent");
    }

    @Override
    public void trackEvent(String type, JSONObject customData) {
        this.log("trackEvent");
    }

}
