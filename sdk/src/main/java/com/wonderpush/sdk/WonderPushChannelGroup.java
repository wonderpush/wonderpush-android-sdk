package com.wonderpush.sdk;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A grouping of related notification channels. e.g., channels that all belong to a single account.
 *
 * <p>
 *     This class mimicks the {@link android.app.NotificationChannelGroup} class to permit
 *     exposing its benefits to devices prior to Android O.
 * </p>
 *
 * @see android.app.NotificationChannelGroup
 */
public class WonderPushChannelGroup implements Cloneable {

    private final String id;
    private String name;

    /**
     * Creates a notification channel group.
     * @param id The id of the group. Must be unique per package. the value may be truncated if it is too long.
     * @see android.app.NotificationChannelGroup#NotificationChannelGroup(java.lang.String, java.lang.CharSequence)
     */
    public WonderPushChannelGroup(@NonNull String id) {
        if (id == null) throw new NullPointerException("WonderPushChannel id cannot be null");
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
                .setName(JSONUtil.optString(input, "name"));
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

    /**
     * Returns the id of this channel group.
     * @return The id of this channel group.
     * @see android.app.NotificationChannelGroup#getId()
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the user visible name of this channel group.
     * @return The user visible name of this channel group.
     * @see android.app.NotificationChannelGroup#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user visible name of this channel group.
     * @param name The user visible name of the group.
     *             The recommended maximum length is 40 characters; the value may be truncated if it is too long.
     * @return The channel group object for chaining setters.
     * @see android.app.NotificationChannelGroup#getName()
     */
    public WonderPushChannelGroup setName(String name) {
        this.name = name;
        return this;
    }

}
