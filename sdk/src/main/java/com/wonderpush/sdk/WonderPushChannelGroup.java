package com.wonderpush.sdk;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class WonderPushChannelGroup implements Cloneable {

    private final String id;
    private String name;

    public WonderPushChannelGroup(@NonNull String id) {
        if (id == null) throw new NullPointerException("WonderPushChannelPreference id cannot be null");
        this.id = id;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    JSONObject toJSON() throws JSONException {
        JSONObject rtn = new JSONObject();
        rtn.put("id", JSONUtil.wrap(id));
        rtn.put("name", JSONUtil.wrap(name));
        return rtn;
    }

    static WonderPushChannelGroup fromJSON(JSONObject input) throws JSONException {
        if (input == null) return null;
        return new WonderPushChannelGroup(input.getString("id"))
                .setName(input.optString("name", null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WonderPushChannelGroup that = (WonderPushChannelGroup) o;

        if (!id.equals(that.id)) return false;
        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        try {
            return this.getClass().getSimpleName() + toJSON();
        } catch (JSONException ex) {
            Log.w(WonderPush.TAG, "Failed to serialize " + this.getClass().getSimpleName() + " for toString()");
            return this.getClass().getSimpleName() + "{???}";
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WonderPushChannelGroup setName(String name) {
        this.name = name;
        return this;
    }

}
