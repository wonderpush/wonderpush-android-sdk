package com.wonderpush.sdk;

import org.json.JSONObject;

import android.util.Log;

class ActionModel {

    private static final String TAG = WonderPush.TAG;

    public enum Type {
        CLOSE("close"),
        TRACK_EVENT("trackEvent"),
        UPDATE_INSTALLATION("updateInstallation"),
        METHOD("method"),
        LINK("link"),
        RATING("rating"),
        MAP_OPEN("mapOpen"),
        ;

        private String type;

        private Type(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }

        public static Type fromString(String type) {
            if (type == null) {
                throw new NullPointerException();
            }
            for (Type actionType : Type.values()) {
                if (type.equals(actionType.toString())) {
                    return actionType;
                }
            }
            throw new IllegalArgumentException("Constant \"" + type + "\" is not a known " + Type.class.getSimpleName());
        }
    }

    private Type type;
    private String url;
    private JSONObject event; // has "type" and optionally "custom" keys
    private JSONObject custom;
    private String method;
    private String methodArg;

    public ActionModel() {
    }

    public ActionModel(JSONObject data) {
        if (data == null) {
            return;
        }

        try {
            type = Type.fromString(data.optString("type", null));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unknown button action", e);
            type = null;
        }
        url = data.optString("url", null);
        event = data.optJSONObject("event");
        custom = data.optJSONObject("custom");
        method = data.optString("method", null);
        methodArg = data.optString("methodArg", null);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    protected JSONObject getEvent() {
        return event;
    }

    protected void setEvent(JSONObject event) {
        this.event = event;
    }

    public JSONObject getCustom() {
        return custom;
    }

    public void setCustom(JSONObject custom) {
        this.custom = custom;
    }

    protected String getMethod() {
        return method;
    }

    protected void setMethod(String method) {
        this.method = method;
    }

    protected String getMethodArg() {
        return methodArg;
    }

    protected void setMethodArg(String methodArg) {
        this.methodArg = methodArg;
    }

}
