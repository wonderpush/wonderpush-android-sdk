package com.wonderpush.sdk;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manage Android notification channel and user preferences.
 *
 * <p>
 *     Starting with Android O, you will have to implement Notification Channels.
 *     This class helps you in this process.
 * </p>
 *
 * <p>
 *     <a href="https://developer.android.com/preview/features/notification-channels.html">Read more about Android O Notification Channels</a>.
 * </p>
 *
 * <p>If not using automatic initialization, make sure to call {@link WonderPush#initialize(Context)} before using this class.</p>
 *
 * <p>
 *     Prior to Android O, the SDK can handle every aspect of the notification.
 *     Starting with Android O, the user is in charge of the importance of notifications,
 *     whether they should vibrate or emit a sound, etc.<br>
 *     In order to ease the transition as well as bring Android O notification channels benefits
 *     to previous versions of Android, use this class to define channels.<br>
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

        sDefaultChannelId = JSONUtil.optString(inChannelPreferences, SERIALIZATION_FIELD_DEFAULT_CHANNEL_ID);
        if (sDefaultChannelId == null) sDefaultChannelId = DEFAULT_CHANNEL_NAME;

        {
            JSONObject inGroups = inChannelPreferences.optJSONObject(SERIALIZATION_FIELD_CHANNEL_GROUPS);
            sChannelGroups = new HashMap<>();
            if (inGroups != null && inGroups != JSONObject.NULL) {
                Iterator<String> it = inGroups.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    JSONObject value = inGroups.optJSONObject(key);
                    try {
                        WonderPushChannelGroup grp = WonderPushChannelGroup.fromJSON(inGroups.optJSONObject(key));
                        _putChannelGroup(grp);
                    } catch (JSONException ex) {
                        Log.e(WonderPush.TAG, "Failed to deserialize WonderPushChannelGroup from JSON: " + value, ex);
                    }
                }
            }
        }

        {
            JSONObject inChannels = inChannelPreferences.optJSONObject(SERIALIZATION_FIELD_CHANNELS);
            sChannels = new HashMap<>();
            if (inChannels != null && inChannels != JSONObject.NULL) {
                Iterator<String> it = inChannels.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    JSONObject value = inChannels.optJSONObject(key);
                    try {
                        WonderPushChannel pref = WonderPushChannel.fromJSON(inChannels.optJSONObject(key));
                        _putChannel(pref);
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

    static synchronized WonderPushChannel channelToUseForNotification(String desiredChannelId) {
        if (desiredChannelId == null) {
            // If no channel is set, use the default one
            desiredChannelId = WonderPushUserPreferences.getDefaultChannelId();
        }
        WonderPushChannel channel = WonderPushUserPreferences.getChannel(desiredChannelId);

        // Ensure channel existence in Android O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) WonderPush.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel androidNotificationChannel = notificationManager.getNotificationChannel(desiredChannelId);
            if (androidNotificationChannel == null) {
                Log.w(WonderPush.TAG, "Asked to use non-existent channel " + desiredChannelId + " falling back to the default channel " + WonderPushUserPreferences.getDefaultChannelId() + " for Android O");
                // Fallback to the default channel
                channel = null;
                desiredChannelId = WonderPushUserPreferences.getDefaultChannelId();
                WonderPushUserPreferences.ensureDefaultAndroidNotificationChannelExists();
            } // else: Channel exists in Android O but not in the SDK, that's fine
        }

        // Ensure we return a usable channel
        if (channel == null) {
            Log.w(WonderPush.TAG, "Using an empty channel configuration instead of the unknown channel " + desiredChannelId);
            // Use an empty channel configuration to not override anything SDK-side
            // That channel *is* (now) registered in Android O, or we are pre Android O
            channel = new WonderPushChannel(desiredChannelId, null);
        }

        return channel;
    }

    static synchronized void ensureDefaultAndroidNotificationChannelExists() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationManager notificationManager = (android.app.NotificationManager) WonderPush.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(getDefaultChannelId());
                if (existingChannel != null) return;
                // Create an empty default channel
                // Note that there is no need to getChannel(getDefaultChannelId()) as if it returns non-null, then the channel was also registered in the system
                WonderPushChannel defaultChannel = new WonderPushChannel(getDefaultChannelId(), null);
                defaultChannel.setName("Notifications");
                putChannel(defaultChannel);
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while ensuring default channel exists", ex);
        }
    }

    /**
     * Get a channel group.
     * @param groupId The identifier of the channel group to get.
     * @return The channel group, if it has previously been created using this class,
     *      {@code null} otherwise, in particular if an Android {@link android.app.NotificationChannelGroup}
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) WonderPush.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.deleteNotificationChannelGroup(groupId);
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelGroup oChannelGroup = null;
            try {
                android.app.NotificationManager notificationManager = (android.app.NotificationManager) WonderPush.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                oChannelGroup = new NotificationChannelGroup(channelGroup.getId(), channelGroup.getName());
                notificationManager.createNotificationChannelGroup(oChannelGroup);
            } catch (Exception ex) {
                WonderPush.logError("Failed to create notification channel group " + oChannelGroup, ex);
            }
        }
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
                if (_putChannelGroup(channelGroup)) save = true;
            }
            for (String groupId : groupIdsToRemove) {
                if (_removeChannelGroup(groupId)) save = true;
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
     *      {@code null} otherwise, in particular if an Android {@link android.app.NotificationChannel}
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) WonderPush.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.deleteNotificationChannel(channelId);
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel oChannel = null;
            try {
                android.app.NotificationManager notificationManager = (android.app.NotificationManager) WonderPush.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                oChannel = new NotificationChannel(channel.getId(), channel.getName(), NotificationManager.IMPORTANCE_DEFAULT);
                if (TextUtils.isEmpty(oChannel.getName())) {
                    // An empty name is not accepted by Android O when calling NotificationManager.createNotificationChannel()
                    // https://android.googlesource.com/platform/frameworks/base/+/5e3fb57af80f91dc882eab910e865e3c22ae02be/services/core/java/com/android/server/notification/RankingHelper.java#543
                    oChannel.setName(channel.getId());
                    if (TextUtils.isEmpty(oChannel.getName())) {
                        oChannel.setName("Notifications");
                    }
                }
                oChannel.setGroup(channel.getGroupId());
                oChannel.setDescription(channel.getDescription());
                if (channel.getBypassDnd() != null) {
                    oChannel.setBypassDnd(channel.getBypassDnd());
                }
                if (channel.getShowBadge() != null) {
                    oChannel.setShowBadge(channel.getShowBadge());
                }
                if (channel.getImportance() != null) {
                    oChannel.setImportance(channel.getImportance());
                }
                if (channel.getLights() != null) {
                    oChannel.enableLights(channel.getLights());
                }
                if (channel.getLightColor() != null) {
                    oChannel.setLightColor(channel.getLightColor());
                }
                if (channel.getVibrate() != null) {
                    oChannel.enableVibration(channel.getVibrate());
                }
                if (channel.getVibrationPattern() != null) {
                    oChannel.setVibrationPattern(channel.getVibrationPattern());
                }
                if (channel.getSound() != null) {
                    AudioAttributes aa = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    if (channel.getSoundUri() != null) {
                        oChannel.setSound(channel.getSoundUri(), aa);
                    } else {
                        oChannel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, aa);
                    }
                }
                if (channel.getLockscreenVisibility() != null) {
                    oChannel.setLockscreenVisibility(channel.getLockscreenVisibility());
                }
                notificationManager.createNotificationChannel(oChannel);
            } catch (Exception ex) {
                WonderPush.logError("Failed to create notification channel " + oChannel, ex);
            }
        }
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
                if (_putChannel(channel)) save = true;
            }
            for (String channelId : channelIdsToRemove) {
                if (_removeChannel(channelId)) save = true;
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

    /**
     * List every disabled notification channel.
     *
     * @return A set of notification channel ids that are disabled in the OS.
     */
    static synchronized Set<String> getDisabledChannelIds() {
        TreeSet<String> rtn = new TreeSet<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            android.app.NotificationManager notificationManager = (android.app.NotificationManager) WonderPush.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            Map<String, NotificationChannelGroup> groups = new HashMap<>();
            for (NotificationChannelGroup group : notificationManager.getNotificationChannelGroups()) {
                groups.put(group.getId(), group);
            }
            for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
                NotificationChannelGroup group = groups.get(channel.getGroup());
                if (channel.getImportance() == NotificationManager.IMPORTANCE_NONE || WonderPushCompatibilityHelper.isNotificationChannelGroupBlocked(group)) {
                    rtn.add(channel.getId());
                }
            }

        } else {

            for (WonderPushChannel channel : sChannels.values()) {
                if (channel.getImportance() != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    rtn.add(channel.getId());
                }
            }

        }

        return rtn;
    }

}
