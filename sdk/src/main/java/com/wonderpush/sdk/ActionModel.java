package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ActionModel implements Cloneable {

    private static final String TAG = WonderPush.TAG;

    public enum Type {
        CLOSE("close"),
        TRACK_EVENT("trackEvent"),
        UPDATE_INSTALLATION("updateInstallation"),
        ADD_PROPERTY("addProperty"),
        REMOVE_PROPERTY("removeProperty"),
        RESYNC_INSTALLATION("resyncInstallation"),
        ADD_TAG("addTag"),
        REMOVE_TAG("removeTag"),
        REMOVE_ALL_TAGS("removeAllTags"),
        METHOD("method"),
        LINK("link"),
        RATING("rating"),
        MAP_OPEN("mapOpen"),
        _DUMP_STATE("_dumpState"),
        _OVERRIDE_SET_LOGGING("_overrideSetLogging"),
        _OVERRIDE_NOTIFICATION_RECEIPT("_overrideNotificationReceipt"),
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
    private JSONObject installation; // contains a "custom" key
    private JSONObject custom;
    private JSONArray tags;
    private Boolean appliedServerSide;
    private Boolean reset;
    private Boolean force;
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
        installation = data.optJSONObject("installation");
        custom = data.optJSONObject("custom");
        tags = data.optJSONArray("tags");
        appliedServerSide = optBool(data, "appliedServerSide", null);
        reset = optBool(data, "reset", null);
        force = optBool(data, "force", null);
        method = JSONUtil.getString(data, "method");
        methodArg = JSONUtil.getString(data, "methodArg");
    }

    private static Boolean optBool(JSONObject object, String field, Boolean defaultValue) {
        Object value = object.opt(field);
        if (!(value instanceof Boolean)) return defaultValue;
        return (Boolean) value;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        ActionModel rtn = (ActionModel) super.clone();
        if (event != null) {
            try {
                rtn.event = new JSONObject(event.toString());
            } catch (JSONException ignored) {}
        }
        if (installation != null) {
            try {
                rtn.installation = JSONUtil.deepCopy(installation);
            } catch (JSONException ignored) {}
        }
        if (custom != null) {
            try {
                rtn.custom = new JSONObject(custom.toString());
            } catch (JSONException ignored) {}
        }
        if (tags != null) {
            try {
                rtn.tags = new JSONArray(tags.toString());
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

    public JSONObject getInstallation() {
        return installation;
    }

    public void setInstallation(JSONObject installation) {
        this.installation = installation;
    }

    public JSONObject getCustom() {
        return custom;
    }

    public void setCustom(JSONObject custom) {
        this.custom = custom;
    }

    public JSONArray getTags() {
        return tags;
    }

    public void setTags(JSONArray tags) {
        this.tags = tags;
    }

    public Boolean getAppliedServerSide() {
        return appliedServerSide;
    }

    public boolean getAppliedServerSide(boolean defaultValue) {
        return appliedServerSide != null ? appliedServerSide : defaultValue;
    }

    public void setAppliedServerSide(Boolean appliedServerSide) {
        this.appliedServerSide = appliedServerSide;
    }

    public Boolean getForce() {
        return force;
    }

    public boolean getForce(boolean defaultValue) {
        return force != null ? force : defaultValue;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

    public Boolean getReset() {
        return reset;
    }

    public boolean getReset(boolean defaultValue) {
        return reset != null ? reset : defaultValue;
    }

    public void setReset(Boolean reset) {
        this.reset = reset;
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
