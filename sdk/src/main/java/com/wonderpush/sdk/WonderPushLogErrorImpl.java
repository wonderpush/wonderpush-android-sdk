package com.wonderpush.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
    public void subscribeToNotifications() {
        this.log("subscribeToNotifications");
    }

    @Override
    public void unsubscribeFromNotifications() {
        this.log("unsubscribeFromNotifications");
    }

    @Override
    public boolean isSubscribedToNotifications() {
        this.log("isSubscribedToNotifications");
        return false;
    }

    @Override
    @Deprecated
    public boolean getNotificationEnabled() {
        this.log("getNotificationEnabled");
        return false;
    }

    @Override
    @Deprecated
    public void setNotificationEnabled(boolean status) {
        this.log("setNotificationEnabled");
    }

    @Override
    public JSONObject getProperties() {
        this.log("getProperties");
        return new JSONObject();
    }

    @Override
    public void putProperties(JSONObject properties) {
        this.log("putProperties");
    }

    @Override
    @Deprecated
    public JSONObject getInstallationCustomProperties() {
        this.log("getInstallationCustomProperties");
        return new JSONObject();
    }

    @Override
    @Deprecated
    public void putInstallationCustomProperties(JSONObject customProperties) {
        this.log("putInstallationCustomProperties");
    }

    @Override
    public void setProperty(String field, Object value) {
        this.log("setProperty");
    }

    @Override
    public void unsetProperty(String field) {
        this.log("unsetProperty");
    }

    @Override
    public void addProperty(String field, Object value) {
        this.log("addProperty");
    }

    @Override
    public void removeProperty(String field, Object value) {
        this.log("removeProperty");
    }

    @Override
    public Object getPropertyValue(String field) {
        this.log("getPropertyValue");
        return JSONObject.NULL;
    }

    @Override
    public List<Object> getPropertyValues(String field) {
        this.log("getPropertyValues");
        return Collections.emptyList();
    }

    @Override
    public void trackEvent(String type) {
        this.log("trackEvent");
    }

    @Override
    public void trackEvent(String type, JSONObject customData) {
        this.log("trackEvent");
    }

    @Override
    public void addTag(String... tag) {
        this.log("addTag");
    }

    @Override
    public void removeTag(String... tag) {
        this.log("removeTag");
    }

    @Override
    public void removeAllTags() {
        this.log("removeAllTags");
    }

    @Override
    public Set<String> getTags() {
        this.log("getTags");
        return new TreeSet<>();
    }

    @Override
    public boolean hasTag(String tag) {
        this.log("hasTag");
        return false;
    }

}
