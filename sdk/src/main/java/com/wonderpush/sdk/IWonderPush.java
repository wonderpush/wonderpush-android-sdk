package com.wonderpush.sdk;

import org.json.JSONObject;

import java.util.List;
import java.util.Set;

/**
 * The interface of all methods that can require user consent.
 */
interface IWonderPush {

    // Called when switching to this implementation
    void _activate();
    // Called when switching away from this implementation
    void _deactivate();

    //
    // Identifiers
    //

    String getAccessToken();
    String getDeviceId();
    String getInstallationId();
    String getPushToken();

    //
    // Subscribing users
    //

    void subscribeToNotifications();
    void unsubscribeFromNotifications();
    boolean isSubscribedToNotifications();
    @Deprecated
    boolean getNotificationEnabled();
    @Deprecated
    void setNotificationEnabled(boolean status);

    //
    // Segmentation
    //

    void trackEvent(String type);
    void trackEvent(String type, JSONObject customData);

    JSONObject getProperties();
    void putProperties(JSONObject properties);
    @Deprecated
    JSONObject getInstallationCustomProperties();
    @Deprecated
    void putInstallationCustomProperties(JSONObject customProperties);

    void setProperty(String field, Object value);
    void unsetProperty(String field);
    void addProperty(String field, Object value);
    void removeProperty(String field, Object value);
    Object getPropertyValue(String field);
    List<Object> getPropertyValues(String field);

    void addTag(String... tag);
    void removeTag(String... tag);
    void removeAllTags();
    Set<String> getTags();
    boolean hasTag(String tag);

}
