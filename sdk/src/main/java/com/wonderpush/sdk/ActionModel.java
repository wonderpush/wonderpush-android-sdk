package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionModel implements Cloneable {

    private static final String TAG = WonderPush.TAG;

    public static List<ActionModel> from(JSONArray primaryActions) {
        if (null == primaryActions) return Collections.emptyList();
        List<ActionModel> result = new ArrayList<>();
        for (int i = 0; i < primaryActions.length(); i++) {
            JSONObject actionJson = primaryActions.optJSONObject(i);
            if (actionJson != null) result.add(new ActionModel(actionJson));
        }
        return result;
    }

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
        CLOSE_NOTIFICATIONS("closeNotifications"),
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
    private NotificationMapModel.Map map;
    private boolean hasChannel; // for CLOSE_NOTIFICATIONS
    private String channel; // for CLOSE_NOTIFICATIONS
    private boolean hasGroup; // for CLOSE_NOTIFICATIONS
    private String group; // for CLOSE_NOTIFICATIONS
    private boolean hasTag; // for CLOSE_NOTIFICATIONS
    private String tag; // for CLOSE_NOTIFICATIONS
    private boolean hasCategory; // for CLOSE_NOTIFICATIONS
    private String category; // for CLOSE_NOTIFICATIONS
    private boolean hasSortKey; // for CLOSE_NOTIFICATIONS
    private String sortKey; // for CLOSE_NOTIFICATIONS
    private JSONObject extras; // for CLOSE_NOTIFICATIONS

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
        hasChannel = data.has("channel");
        channel = JSONUtil.getString(data, "channel");
        hasGroup = data.has("group");
        group = JSONUtil.getString(data, "group");
        hasTag = data.has("tag");
        tag = JSONUtil.getString(data, "tag");
        hasCategory = data.has("category");
        category = JSONUtil.getString(data, "category");
        hasSortKey = data.has("sortKey");
        sortKey = JSONUtil.getString(data, "sortKey");
        extras = data.optJSONObject("extras");
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
        if (extras != null) {
            try {
                rtn.extras = new JSONObject(extras.toString());
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

    public NotificationMapModel.Map getMap() {
        return map;
    }

    public void setMap(NotificationMapModel.Map map) {
        this.map = map;
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

    public boolean hasChannel() {
        return hasChannel;
    }

    public void setHasChannel(boolean hasChannel) {
        this.hasChannel = hasChannel;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean hasGroup() {
        return hasGroup;
    }

    public void setHasGroup(boolean hasGroup) {
        this.hasGroup = hasGroup;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean hasTag() {
        return hasTag;
    }

    public void setHasTag(boolean hasTag) {
        this.hasTag = hasTag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean hasCategory() {
        return hasCategory;
    }

    public void setHasCategory(boolean hasCategory) {
        this.hasCategory = hasCategory;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean hasSortKey() {
        return hasSortKey;
    }

    public void setHasSortKey(boolean hasSortKey) {
        this.hasSortKey = hasSortKey;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public JSONObject getExtras() {
        return extras;
    }

    public void setExtras(JSONObject extras) {
        this.extras = extras;
    }

}
