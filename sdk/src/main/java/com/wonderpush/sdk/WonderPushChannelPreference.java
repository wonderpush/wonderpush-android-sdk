package com.wonderpush.sdk;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class WonderPushChannelPreference implements Cloneable {

    private final String id;
    private final String groupId;

    private String name;
    private String description;

    // TODO Expose once implemented
    // Android O Channels only
    //private Boolean bypassDnd;
    //private Boolean badge;

    // One-time in Android O Channels
    private Integer importance;
    private Boolean lights;
    private Integer lightColor;
    private Boolean vibrate;
    private long[] vibrationPattern;
    private Boolean sound; // if true and soundUri is null, use Notification.Builder.setDefaults(Notification.DEFAULT_SOUND) or assume Settings.System.DEFAULT_NOTIFICATION_URI for Android O NotificationChannel
    private Uri soundUri; // if sound is null, will only override the sound if the notification is noisy. Will become the Android O NotificationChannel sound upon migration (that is all notifications will become noisy.)
    private Integer lockscreenVisibility;

    // Pre Android O WonderPush addition (no op in Android O)
    private Boolean vibrateInSilentMode;

    // Any Android version
    private Integer color; // use TRANSPARENT to remove coloring
    private Boolean localOnly;

    public WonderPushChannelPreference(@NonNull String id, String groupId) {
        if (id == null) throw new NullPointerException("WonderPushChannelPreference id cannot be null");
        this.id = id;
        this.groupId = groupId;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        WonderPushChannelPreference rtn = (WonderPushChannelPreference) super.clone();
        rtn.vibrationPattern = vibrationPattern == null ? null : vibrationPattern.clone();
        return rtn;
    }

    JSONObject toJSON() throws JSONException {
        JSONObject rtn = new JSONObject();
        rtn.put("id", JSONUtil.wrap(id));
        rtn.put("groupId", JSONUtil.wrap(groupId));
        rtn.put("name", JSONUtil.wrap(name));
        rtn.put("description", JSONUtil.wrap(description));
        //rtn.put("bypassDnd", JSONUtil.wrap(bypassDnd));
        //rtn.put("badge", JSONUtil.wrap(badge));
        rtn.put("importance", JSONUtil.wrap(importance));
        rtn.put("lights", JSONUtil.wrap(lights));
        rtn.put("vibrate", JSONUtil.wrap(vibrate));
        rtn.put("vibrationPattern", JSONUtil.wrap(vibrationPattern));
        rtn.put("lightColor", JSONUtil.wrap(lightColor));
        rtn.put("lockscreenVisibility", JSONUtil.wrap(lockscreenVisibility));
        rtn.put("sound", JSONUtil.wrap(sound));
        rtn.put("soundUri", JSONUtil.wrap(soundUri));
        rtn.put("vibrateInSilentMode", JSONUtil.wrap(vibrateInSilentMode));
        rtn.put("color", JSONUtil.wrap(color));
        rtn.put("localOnly", JSONUtil.wrap(localOnly));
        return rtn;
    }

    static WonderPushChannelPreference fromJSON(JSONObject input) throws JSONException {
        if (input == null) return null;
        return new WonderPushChannelPreference(input.getString("id"), input.optString("groupId", null))
                .setName(input.optString("name", null))
                .setDescription(input.optString("description", null))
                //.setBypassDnd(JSONUtil.optBoolean(input, "bypassDnd"))
                //.setBadge(JSONUtil.optBoolean(input, "badge"))
                .setImportance(JSONUtil.optInteger(input, "importance"))
                .setLights(JSONUtil.optBoolean(input, "lights"))
                .setVibrate(JSONUtil.optBoolean(input, "vibrate"))
                .setVibrationPattern(JSONUtil.optLongArray(input, "vibrationPattern"))
                .setLightColor(JSONUtil.optInteger(input, "lightColor"))
                .setLockscreenVisibility(JSONUtil.optInteger(input, "lockscreenVisibility"))
                .setSound(JSONUtil.optBoolean(input, "sound"))
                .setSoundUri(JSONUtil.optUri(input, "soundUri"))
                .setVibrateInSilentMode(JSONUtil.optBoolean(input, "vibrateInSilentMode"))
                .setColor(JSONUtil.optInteger(input, "color"))
                .setLocalOnly(JSONUtil.optBoolean(input, "localOnly"))
        ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WonderPushChannelPreference that = (WonderPushChannelPreference) o;

        if (!id.equals(that.id)) return false;
        if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;
        //if (bypassDnd != null ? !bypassDnd.equals(that.bypassDnd) : that.bypassDnd != null)
        //    return false;
        //if (badge != null ? !badge.equals(that.badge) : that.badge != null) return false;
        if (importance != null ? !importance.equals(that.importance) : that.importance != null)
            return false;
        if (lights != null ? !lights.equals(that.lights) : that.lights != null) return false;
        if (vibrate != null ? !vibrate.equals(that.vibrate) : that.vibrate != null) return false;
        if (!Arrays.equals(vibrationPattern, that.vibrationPattern)) return false;
        if (lightColor != null ? !lightColor.equals(that.lightColor) : that.lightColor != null)
            return false;
        if (lockscreenVisibility != null ? !lockscreenVisibility.equals(that.lockscreenVisibility) : that.lockscreenVisibility != null)
            return false;
        if (sound != null ? !sound.equals(that.sound) : that.sound != null) return false;
        if (soundUri != null ? !soundUri.equals(that.soundUri) : that.soundUri != null) return false;
        if (vibrateInSilentMode != null ? !vibrateInSilentMode.equals(that.vibrateInSilentMode) : that.vibrateInSilentMode != null)
            return false;
        if (color != null ? !color.equals(that.color) : that.color != null) return false;
        return localOnly != null ? localOnly.equals(that.localOnly) : that.localOnly == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (groupId != null ? groupId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        //result = 31 * result + (bypassDnd != null ? bypassDnd.hashCode() : 0);
        //result = 31 * result + (badge != null ? badge.hashCode() : 0);
        result = 31 * result + (importance != null ? importance.hashCode() : 0);
        result = 31 * result + (lights != null ? lights.hashCode() : 0);
        result = 31 * result + (vibrate != null ? vibrate.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(vibrationPattern);
        result = 31 * result + (lightColor != null ? lightColor.hashCode() : 0);
        result = 31 * result + (lockscreenVisibility != null ? lockscreenVisibility.hashCode() : 0);
        result = 31 * result + (sound != null ? sound.hashCode() : 0);
        result = 31 * result + (soundUri != null ? soundUri.hashCode() : 0);
        result = 31 * result + (vibrateInSilentMode != null ? vibrateInSilentMode.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (localOnly != null ? localOnly.hashCode() : 0);
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

    public String getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public WonderPushChannelPreference setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WonderPushChannelPreference setDescription(String description) {
        this.description = description;
        return this;
    }

    //public Boolean getBypassDnd() {
    //    return bypassDnd;
    //}

    //public WonderPushChannelPreference setBypassDnd(Boolean bypassDnd) {
    //    this.bypassDnd = bypassDnd;
    //    return this;
    //}

    //public Boolean getBadge() {
    //    return badge;
    //}

    //public WonderPushChannelPreference setBadge(Boolean badge) {
    //    this.badge = badge;
    //    return this;
    //}

    public Integer getImportance() {
        return importance;
    }

    public WonderPushChannelPreference setImportance(Integer importance) {
        this.importance = importance;
        return this;
    }

    public Boolean getLights() {
        return lights;
    }

    public WonderPushChannelPreference setLights(Boolean lights) {
        this.lights = lights;
        return this;
    }

    public Boolean getVibrate() {
        return vibrate;
    }

    public WonderPushChannelPreference setVibrate(Boolean vibrate) {
        this.vibrate = vibrate;
        return this;
    }

    public long[] getVibrationPattern() {
        return vibrationPattern;
    }

    public WonderPushChannelPreference setVibrationPattern(long[] vibrationPattern) {
        this.vibrationPattern = vibrationPattern;
        return this;
    }

    public Integer getLightColor() {
        return lightColor;
    }

    public WonderPushChannelPreference setLightColor(Integer lightColor) {
        this.lightColor = lightColor;
        return this;
    }

    public Integer getLockscreenVisibility() {
        return lockscreenVisibility;
    }

    public WonderPushChannelPreference setLockscreenVisibility(Integer lockscreenVisibility) {
        this.lockscreenVisibility = lockscreenVisibility;
        return this;
    }

    public Boolean getSound() {
        return sound;
    }

    public WonderPushChannelPreference setSound(Boolean sound) {
        this.sound = sound;
        return this;
    }

    public Uri getSoundUri() {
        return soundUri;
    }

    public WonderPushChannelPreference setSoundUri(Uri soundUri) {
        this.soundUri = soundUri;
        return this;
    }

    public Boolean getVibrateInSilentMode() {
        return vibrateInSilentMode;
    }

    public WonderPushChannelPreference setVibrateInSilentMode(Boolean vibrateInSilentMode) {
        this.vibrateInSilentMode = vibrateInSilentMode;
        return this;
    }

    public Integer getColor() {
        return color;
    }

    public WonderPushChannelPreference setColor(Integer color) {
        this.color = color;
        return this;
    }

    public Boolean getLocalOnly() {
        return localOnly;
    }

    public WonderPushChannelPreference setLocalOnly(Boolean localOnly) {
        this.localOnly = localOnly;
        return this;
    }
}
