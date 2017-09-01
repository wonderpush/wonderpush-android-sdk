package com.wonderpush.sdk;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * A representation of settings that apply to a collection of similarly themed notifications.
 *
 * <p>
 *     <b>BETA</b> -
 *     <i>This API is marked beta and may change without prior notice to reflect any changes
 *     made to the Android O APIs.</i>
 * </p>
 *
 * <p>
 *     This class mimicks the {@link android.app.NotificationChannel} class to permit
 *     exposing its benefits to devices prior to Android O.
 *     Contrary to the latter, this class permits to override only a few aspects of the notification.
 * </p>
 *
 * <p>
 *     It also contains some additions that may no longer be available under Android O where the user
 *     is in full control of these settings, such as preventing a notification that usually makes a
 *     sound or vibrates to vibrate while the device is in silent mode.<br/>
 *     Some feature like the color should remain configurable using this class under Android O.
 * </p>
 *
 * @see android.app.NotificationChannel
 */
public class WonderPushChannel implements Cloneable {

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

    /**
     * Create a notification channel.
     *
     * @param id The id of the channel. Must be unique per package. The value may be truncated if it is too long.
     * @param groupId The id of a channel group. You should create the group prior to creating this channel.
     *
     * @see android.app.NotificationChannel#NotificationChannel(java.lang.String, java.lang.CharSequence, int)
     */
    public WonderPushChannel(@NonNull String id, String groupId) {
        if (id == null) throw new NullPointerException("WonderPushChannel id cannot be null");
        this.id = id;
        this.groupId = groupId;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        WonderPushChannel rtn = (WonderPushChannel) super.clone();
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

    static WonderPushChannel fromJSON(JSONObject input) throws JSONException {
        if (input == null) return null;
        return new WonderPushChannel(input.getString("id"), input.optString("groupId", null))
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

        WonderPushChannel that = (WonderPushChannel) o;

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

    /**
     * Returns the id of this channel.
     * @see android.app.NotificationChannel#getId()
     */
    public String getId() {
        return id;
    }

    /**
     * Returns what group this channel belongs to.
     *
     * This is used only for visually grouping channels in the UI.
     * Can be {@code null}.
     *
     * @return The id of the group this channel belongs to, if any.
     * @see android.app.NotificationChannel#setGroup(String)
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Returns the user visible name of this channel.
     * @see android.app.NotificationChannel#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user visible name of this channel.
     *
     * The recommended maximum length is 40 characters; the value may be truncated if it is too long.
     *
     * @param name The user visible name of this channel.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setName(CharSequence)
     */
    public WonderPushChannel setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the user visible description of this channel.
     *
     * @return The user visible description of this channel.
     * @see android.app.NotificationChannel#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the user visible description of this channel.
     *
     * The recommended maximum length is 300 characters; the value may be truncated if it is too long.
     *
     * @param description The user visible description of this channel.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setDescription(String)
     */
    public WonderPushChannel setDescription(String description) {
        this.description = description;
        return this;
    }

    //public Boolean getBypassDnd() {
    //    return bypassDnd;
    //}

    //public WonderPushChannel setBypassDnd(Boolean bypassDnd) {
    //    this.bypassDnd = bypassDnd;
    //    return this;
    //}

    //public Boolean getBadge() {
    //    return badge;
    //}

    //public WonderPushChannel setBadge(Boolean badge) {
    //    this.badge = badge;
    //    return this;
    //}

    /**
     * Returns the user specified importance for notifications posted to this channel.
     *
     * Example: {@link android.app.NotificationManager#IMPORTANCE_DEFAULT}
     *
     * @return The user specified importance for notifications posted to this channel.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#getImportance()
     */
    public Integer getImportance() {
        return importance;
    }

    /**
     * Sets the level of interruption of this notification channel.
     *
     * Example: {@link android.app.NotificationManager#IMPORTANCE_DEFAULT}
     *
     * @param importance The level of interruption of this notification channel.
     *                   Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setImportance(int)
     */
    public WonderPushChannel setImportance(Integer importance) {
        this.importance = importance;
        return this;
    }

    /**
     * Returns whether notifications posted to this channel trigger notification lights.
     * @return Whether notifications posted to this channel trigger notification lights.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#shouldShowLights()
     */
    public Boolean getLights() {
        return lights;
    }

    /**
     * Sets whether notifications posted to this channel should display notification lights, on devices that support that feature.
     * @param lights Whether notifications posted to this channel should display notification lights
     *               Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#enableLights(boolean)
     */
    public WonderPushChannel setLights(Boolean lights) {
        this.lights = lights;
        return this;
    }

    /**
     * Returns whether notifications posted to this channel always vibrate.
     * @return Whether notifications posted to this channel always vibrate.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#shouldVibrate()
     */
    public Boolean getVibrate() {
        return vibrate;
    }

    /**
     * Sets whether notification posted to this channel should vibrate.
     *
     * The vibration pattern can be set with {@link #setVibrationPattern(long[])}.
     *
     * @param vibrate Whether notification posted to this channel should vibrate.
     *                Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#enableVibration(boolean)
     */
    public WonderPushChannel setVibrate(Boolean vibrate) {
        this.vibrate = vibrate;
        return this;
    }

    /**
     * Returns the vibration pattern for notifications posted to this channel.
     * @return The vibration pattern for notifications posted to this channel.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#getVibrationPattern()
     */
    public long[] getVibrationPattern() {
        return vibrationPattern;
    }

    /**
     * Sets the vibration pattern for notifications posted to this channel.
     * @param vibrationPattern The vibration pattern for notifications posted to this channel.
     *                         Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setVibrationPattern(long[])
     */
    public WonderPushChannel setVibrationPattern(long[] vibrationPattern) {
        this.vibrationPattern = vibrationPattern;
        return this;
    }

    /**
     * Sets the notification light color for notifications posted to this channel,
     * if lights are enabled on this channel and the device supports that feature.
     *
     * @return The notification light color for notifications posted to this channel
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#getLightColor()
     */
    public Integer getLightColor() {
        return lightColor;
    }

    /**
     * Returns the notification light color for notifications posted to this channel.
     * @param lightColor The notification light color for notifications posted to this channel.
     *                   Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setLightColor(int)
     */
    public WonderPushChannel setLightColor(Integer lightColor) {
        this.lightColor = lightColor;
        return this;
    }

    /**
     * Returns whether or not notifications posted to this channel are shown on the lockscreen in full or redacted form.
     * @return Whether or not notifications posted to this channel are shown on the lockscreen in full or redacted form.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#getLockscreenVisibility()
     */
    public Integer getLockscreenVisibility() {
        return lockscreenVisibility;
    }

    /**
     * Sets whether notifications posted to this channel appear on the lockscreen or not, and if so, whether they appear in a redacted form.
     *
     * Example: {@link android.app.Notification#VISIBILITY_PUBLIC}.
     *
     * @param lockscreenVisibility Whether notifications posted to this channel appear on the lockscreen or not, and if so, whether they appear in a redacted form.
     *                             Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setLockscreenVisibility(int)
     */
    public WonderPushChannel setLockscreenVisibility(Integer lockscreenVisibility) {
        this.lockscreenVisibility = lockscreenVisibility;
        return this;
    }

    /**
     * Returns whether a sound should be played for notifications posted to this channel.
     * @return Whether a sound should be played for notifications posted to this channel.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#getSound()
     */
    public Boolean getSound() {
        return sound;
    }

    /**
     * Sets whether a sound should be played for notifications posted to this channel.
     * @param sound Whether a sound should be played for notifications posted to this channel.
     *              Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setSound(android.net.Uri, android.media.AudioAttributes)
     */
    public WonderPushChannel setSound(Boolean sound) {
        this.sound = sound;
        return this;
    }

    /**
     * Returns the notification sound for this channel.
     * @return The notification sound for this channel.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.NotificationChannel#getSound()
     */
    public Uri getSoundUri() {
        return soundUri;
    }

    /**
     * Sets the sound that should be played for notifications posted to this channel and its audio attributes.
     *
     * Notification channels with an importance of at least {@link android.app.NotificationManager#IMPORTANCE_DEFAULT} should have a sound.
     *
     * @param soundUri The sound that should be played for notifications posted to this channel.
     *                 You can use {@link android.provider.Settings.System#DEFAULT_NOTIFICATION_URI} to use the default notification sound.
     *                 Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.NotificationChannel#setSound(android.net.Uri, android.media.AudioAttributes)
     */
    public WonderPushChannel setSoundUri(Uri soundUri) {
        this.soundUri = soundUri;
        return this;
    }

    /**
     * Returns whether notifications posted to this channel vibrate if the device is in silent mode.
     *
     * <p>This is a WonderPush addition.</p>
     *
     * <p>
     *     The primary use of this flag is to permit notifications with sound and vibration in normal
     *     mode but prevent it from vibrating in all cases in silent mode.
     * </p>
     *
     * @return Whether notifications posted to this channel vibrate if the device is in silent mode.
     * Returns {@code null} if this setting does not override the received notifications setting.
     */
    public Boolean getVibrateInSilentMode() {
        return vibrateInSilentMode;
    }

    /**
     * Sets whether notifications posted to this channel vibrate if the device is in silent mode.
     *
     * <p>This is a WonderPush addition.</p>
     *
     * <p>
     *     The primary use of this flag is to permit notifications with sound and vibration in normal
     *     mode but prevent it from vibrating in all cases in silent mode.
     * </p>
     *
     * <p>
     *     Note: You can control this setting irrespective of the fact that a given notification
     *     should vibrate a make a sound in normal mode.
     * </p>
     *
     * @param vibrateInSilentMode Whether notifications posted to this channel vibrate if the device is in silent mode.
     *                            Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     */
    public WonderPushChannel setVibrateInSilentMode(Boolean vibrateInSilentMode) {
        this.vibrateInSilentMode = vibrateInSilentMode;
        return this;
    }

    /**
     * Returns the color to impose on all notifications posted to this channel.
     *
     * <p>This is independent of Android O notification channels and should continue to work under Android O.</p>
     *
     * @return The color to impose on all notifications posted to this channel.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.Notification.Builder#setColor(int)
     */
    public Integer getColor() {
        return color;
    }

    /**
     * Sets the color to impose on all notifications posted to this channel.
     *
     * <p>This is independent of Android O notification channels and should continue to work under Android O.</p>
     *
     * @param color The color to impose on all notifications posted to this channel.
     *              Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.Notification.Builder#setColor(int)
     */
    public WonderPushChannel setColor(Integer color) {
        this.color = color;
        return this;
    }

    /**
     * Returns whether notifications posted to this channel should be local to this device.
     *
     * <p>This is independent of Android O notification channels and should continue to work under Android O.</p>
     *
     * @return Whether notifications posted to this channel should be local to this device.
     * Returns {@code null} if this setting does not override the received notifications setting.
     * @see android.app.Notification.Builder#setLocalOnly(boolean)
     */
    public Boolean getLocalOnly() {
        return localOnly;
    }

    /**
     * Sets whether notifications posted to this channel should be local to this device.
     *
     * <p>This is independent of Android O notification channels and should continue to work under Android O.</p>
     *
     * @param localOnly Whether notifications posted to this channel should be local to this device.
     *                  Use {@code null} to not override the received notifications setting.
     * @return The channel object for chaining setters.
     * @see android.app.Notification.Builder#setLocalOnly(boolean)
     */
    public WonderPushChannel setLocalOnly(Boolean localOnly) {
        this.localOnly = localOnly;
        return this;
    }

}
