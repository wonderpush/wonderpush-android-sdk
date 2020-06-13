package com.wonderpush.sdk;

import org.json.JSONObject;

public class InternalEventTracker {

    InternalEventTracker() {}

    public void trackInternalEvent(String type, JSONObject eventData, JSONObject customData) {
        WonderPush.trackInternalEvent(type, eventData, customData);
    }

    public void trackInternalEvent(String type, JSONObject eventData) {
        WonderPush.trackInternalEvent(type, eventData);
    }

}
