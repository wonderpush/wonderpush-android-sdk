package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

class ActionModel implements Cloneable {

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

        private final String type;

        Type(String type) {
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
            type = Type.fromString(JSONUtil.getString(data, "type"));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unknown button action", e);
            type = null;
        }
        url = JSONUtil.getString(data, "url");
        event = data.optJSONObject("event");
        custom = data.optJSONObject("custom");
        method = JSONUtil.getString(data, "method");
        methodArg = JSONUtil.getString(data, "methodArg");
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        ActionModel rtn = (ActionModel) super.clone();
        if (event != null) {
            try {
                rtn.event = new JSONObject(event.toString());
            } catch (JSONException ignored) {}
        }
        if (custom != null) {
            try {
                rtn.custom = new JSONObject(custom.toString());
            } catch (JSONException ignored) {}
        }
        return rtn;
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
