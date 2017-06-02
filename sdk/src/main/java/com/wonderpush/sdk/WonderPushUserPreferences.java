package com.wonderpush.sdk;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Manage Android notification channel and user preferences.
 *
 * <p>
 *     Starting with Android O, you will have to implement Notification Channels.
 *     This class helps you in this process.
 * <p>
 *
 * </p>
 *     <a href="https://developer.android.com/preview/features/notification-channels.html">Read more about Android O Notification Channels</a>.
 * </p>
 *
 * <p>You must call {@link WonderPush#initialize(Context)} before using this class.</p>
 *
 * <p>
 *     Prior to Android O, the SDK can handle every aspect of the notification.
 *     Starting with Android O, the user is in charge of the importance of notifications,
 *     whether they should vibrate or emit a sound, etc.<br/>
 *     In order to ease the transition as well as bring Android O notification channels benefits
 *     to previous versions of Android, use this class to define channels.<br/>
 *     The channels and groups defined here will be matched to Android O channels as soon as the
 *     device OS is upgraded. The application is free to manage notification groups and channels
 *     by itself and it needs not make this class aware of them.
 * </p>
 */
public class WonderPushUserPreferences {

    private static final String DEFAULT_CHANNEL_NAME = "default";

    private static final String SERIALIZATION_FIELD_DEFAULT_CHANNEL_ID = "defaultChannelId";
    private static final String SERIALIZATION_FIELD_CHANNEL_GROUPS = "channelGroups";
    private static final String SERIALIZATION_FIELD_CHANNELS = "channels";

    private static String sDefaultChannelId;
    private static Map<String, WonderPushChannelGroup> sChannelGroups;
    private static Map<String, WonderPushChannel> sChannels;

    static void initialize() {
        try {
            load();
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while initializing WonderPushUserPreferences", ex);
        }
    }

    private static synchronized void load() {
        JSONObject inChannelPreferences = WonderPushConfiguration.getChannelPreferences();
        if (inChannelPreferences == null) inChannelPreferences = new JSONObject();

        sDefaultChannelId = inChannelPreferences.optString(SERIALIZATION_FIELD_DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME);

        {
            JSONObject inGroups = inChannelPreferences.optJSONObject(SERIALIZATION_FIELD_CHANNEL_GROUPS);
            sChannelGroups = new HashMap<>();
            if (inGroups != null) {
                Iterator<String> it = inGroups.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    JSONObject value = inGroups.optJSONObject(key);
                    try {
                        WonderPushChannelGroup grp = WonderPushChannelGroup.fromJSON(inGroups.optJSONObject(key));
                        sChannelGroups.put(grp.getId(), grp);
                    } catch (JSONException ex) {
                        Log.e(WonderPush.TAG, "Failed to deserialize WonderPushChannelGroup from JSON: " + value, ex);
                    }
                }
            }
        }

        {
            JSONObject inChannels = inChannelPreferences.optJSONObject(SERIALIZATION_FIELD_CHANNELS);
            sChannels = new HashMap<>();
            if (inChannels != null) {
                Iterator<String> it = inChannels.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    JSONObject value = inChannels.optJSONObject(key);
                    try {
                        WonderPushChannel pref = WonderPushChannel.fromJSON(inChannels.optJSONObject(key));
                        sChannels.put(pref.getId(), pref);
                    } catch (JSONException ex) {
                        Log.e(WonderPush.TAG, "Failed to deserialize WonderPushChannel from JSON: " + value, ex);
                    }
                }
            }
        }

        WonderPush.logDebug("UserPreferences: default channel id: " + sDefaultChannelId);
        WonderPush.logDebug("UserPreferences: channel groups:");
        for (WonderPushChannelGroup group : sChannelGroups.values()) {
            WonderPush.logDebug("- " + group.getId() + ": " + group);
        }
        WonderPush.logDebug("UserPreferences: channels:");
        for (WonderPushChannel channel : sChannels.values()) {
            WonderPush.logDebug("- " + channel.getId() + ": " + channel);
        }
    }

    private static synchronized void save() {
        try {
            JSONObject outChannelPreferences = new JSONObject();

            outChannelPreferences.put(SERIALIZATION_FIELD_DEFAULT_CHANNEL_ID, sDefaultChannelId);

            {
                JSONObject outGroups = new JSONObject();
                for (WonderPushChannelGroup group : sChannelGroups.values()) {
                    outGroups.put(group.getId(), group.toJSON());
                }
                outChannelPreferences.put(SERIALIZATION_FIELD_CHANNEL_GROUPS, outGroups);
            }

            {
                JSONObject outChannels = new JSONObject();
                for (WonderPushChannel channel : sChannels.values()) {
                    outChannels.put(channel.getId(), channel.toJSON());
                }
                outChannelPreferences.put(SERIALIZATION_FIELD_CHANNELS, outChannels);
            }

            //WonderPush.logDebug("UserPreferences: saving preferences: " + outChannelPreferences);
            WonderPushConfiguration.setChannelPreferences(outChannelPreferences);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Unexpected error while serializing channel preferences", ex);
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while saving WonderPushUserPreferences", ex);
        }
    }

    /**
     * Get the default channel id.
     *
     * <p>
     *     The default channel is used when a received notification is not explicitly bound to a channel,
     *     and as the default notification channel from Android O, as they are mandatory.
     * </p>
     *
     * <p>
     *     The default channel is initially created to an empty {@link WonderPushChannel},
     *     meaning that the SDK won't apply any changes to the notifications (prior to Android O).
     * </p>
     *
     * @return The default channel id.
     */
    public static synchronized String getDefaultChannelId() {
        return sDefaultChannelId;
    }

    /**
     * Set the default channel id.
     *
     * <p>
     *     This function does not enforce the existence of the given channel until a notification
     *     is to be posted to the default channel.
     *     This way you are free to call this function either before or after creating the given
     *     channel, either using this class or directly using Android O APIs.
     * </p>
     *
     * @param id The identifier of the default channel.
     */
    public static synchronized void setDefaultChannelId(String id) {
        try {
            if (_setDefaultChannelId(id)) {
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while setting default channel id to " + id, ex);
        }
    }

    private static synchronized boolean _setDefaultChannelId(String id) {
        if (id == null || "".equals(id)) id = DEFAULT_CHANNEL_NAME; // FIXME "" too?
        if (!id.equals(sDefaultChannelId)) {
            sDefaultChannelId = id;
            return true;
        }
        return false;
    }

    static synchronized void ensureDefaultAndroidNotificationChannelExists() {
        try {
            // TODO if (Android ≥ O) ensure notification channel existence
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while ensuring default channel exists", ex);
        }
    }

    /**
     * Get a channel group.
     * @param groupId The identifier of the channel group to get.
     * @return The channel group, if it has previously been created using this class,
     *      {@code null} otherwise, in particular if an Android {@link android.app.android.app.NotificationChannelGroup}
     *      exists but has not been registered with this class.
     */
    public static synchronized WonderPushChannelGroup getChannelGroup(String groupId) {
        try {
            WonderPushChannelGroup rtn = sChannelGroups.get(groupId);
            if (rtn != null) {
                try {
                    rtn = (WonderPushChannelGroup) rtn.clone();
                } catch (CloneNotSupportedException ex) {
                    Log.e(WonderPush.TAG, "Unexpected error while cloning gotten channel group " + rtn, ex);
                    return null;
                }
            }
            return rtn;
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while getting channel group " + groupId, ex);
            return null;
        }
    }

    /**
     * Remove a channel group.
     *
     * <p>Remove a channel group both from this class registry and from Android.</p>
     *
     * @param groupId The identifier of the channel group to remove.
     */
    public static synchronized void removeChannelGroup(String groupId) {
        try {
            if (_removeChannelGroup(groupId)) {
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while removing channel group " + groupId, ex);
        }
    }

    private static synchronized boolean _removeChannelGroup(String groupId) {
        WonderPushChannelGroup prev = sChannelGroups.remove(groupId);
        // TODO Android ≥ O remove channel group
        return prev != null;
    }

    /**
     * Create or update a channel group.
     *
     * <p>Creates or updates a channel group both in this class registry and in Android.</p>
     *
     * @param channelGroup The channel group to create or update.
     */
    public static synchronized void putChannelGroup(WonderPushChannelGroup channelGroup) {
        try {
            if (_putChannelGroup(channelGroup)) {
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while putting channel group " + channelGroup, ex);
        }
    }

    private static synchronized boolean _putChannelGroup(WonderPushChannelGroup channelGroup) {
        if (channelGroup == null) return false;
        WonderPushChannelGroup prev = sChannelGroups.put(channelGroup.getId(), channelGroup);
        // TODO Android ≥ O create channel group
        return prev == null || !prev.equals(channelGroup);
    }

    /**
     * Create, update and remove channel existing groups to match the given channel groups.
     *
     * <p>Creates, updates and removes channel groups both in this class registry and in Android.</p>
     *
     * <p>Any non listed, previously existing channel group will be removed.</p>
     *
     * @param channelGroups The channel groups to create or update.
     *                      Any non listed, previously existing channel group will be removed.
     */
    public static synchronized void setChannelGroups(Collection<WonderPushChannelGroup> channelGroups) {
        if (channelGroups == null) return;
        boolean save = false;
        try {
            Set<String> groupIdsToRemove = new HashSet<>(sChannelGroups.keySet());
            for (WonderPushChannelGroup channelGroup : channelGroups) {
                if (channelGroup == null) continue;
                groupIdsToRemove.remove(channelGroup.getId());
                save = save || _putChannelGroup(channelGroup);
            }
            for (String groupId : groupIdsToRemove) {
                save = save || _removeChannelGroup(groupId);
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while setting channel groups " + channelGroups, ex);
        } finally {
            try {
                if (save) {
                    save();
                }
            } catch (Exception ex) {
                Log.e(WonderPush.TAG, "Unexpected error while setting channel groups " + channelGroups, ex);
            }
        }
    }

    /**
     * Get a channel.
     * @param channelId The identifier of the channel to get.
     * @return The channel, if it has previously been created using this class,
     *      {@code null} otherwise, in particular if an Android {@link android.app.android.app.NotificationChannel}
     *      exists but has not been registered with this class.
     */
    public static synchronized WonderPushChannel getChannel(String channelId) {
        try {
            WonderPushChannel rtn = sChannels.get(channelId);
            if (rtn != null) {
                try {
                    rtn = (WonderPushChannel) rtn.clone();
                } catch (CloneNotSupportedException ex) {
                    Log.e(WonderPush.TAG, "Unexpected error while cloning gotten channel " + rtn, ex);
                    return null;
                }
            }
            return rtn;
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while getting channel " + channelId, ex);
            return null;
        }
    }

    /**
     * Remove a channel.
     *
     * <p>Remove a channel both from this class registry and from Android.</p>
     *
     * @param channelId The identifier of the channel to remove.
     */
    public static synchronized void removeChannel(String channelId) {
        try {
            if (_removeChannel(channelId)) {
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while removing channel " + channelId, ex);
        }
    }

    private static synchronized boolean _removeChannel(String channelId) {
        WonderPushChannel prev = sChannels.remove(channelId);
        // TODO Android ≥ O remove channel
        return prev != null;
    }

    /**
     * Create or update a channel.
     *
     * <p>Creates or updates a channel both in this class registry and in Android.</p>
     *
     * @param channel The channel to create or update.
     */
    public static synchronized void putChannel(WonderPushChannel channel) {
        try {
            if (_putChannel(channel)) {
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while putting channel " + channel, ex);
        }
    }

    private static synchronized boolean _putChannel(WonderPushChannel channel) {
        if (channel == null) return false;
        WonderPushChannel prev = sChannels.put(channel.getId(), channel);
        // TODO Android ≥ O create channel
        return prev == null || !prev.equals(channel);
    }

    /**
     * Create, update and remove channels to match the given channels.
     *
     * <p>Creates, updates and removes channels both in this class registry and in Android.</p>
     *
     * <p>Any non listed, previously existing channel will be removed.</p>
     *
     * @param channels The channels to create or update.
     *                 Any non listed, previously existing channel will be removed.
     */
    public static synchronized void setChannels(Collection<WonderPushChannel> channels) {
        if (channels == null) return;
        boolean save = false;
        try {
            Set<String> channelIdsToRemove = new HashSet<>(sChannels.keySet());
            for (WonderPushChannel channel : channels) {
                if (channel == null) continue;
                channelIdsToRemove.remove(channel.getId());
                save = save || _putChannel(channel);
            }
            for (String groupId : channelIdsToRemove) {
                save = save || _removeChannelGroup(groupId);
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while setting channels " + channels, ex);
        } finally {
            try {
                if (save) {
                    save();
                }
            } catch (Exception ex) {
                Log.e(WonderPush.TAG, "Unexpected error while setting channels " + channels, ex);
            }
        }
    }

}
