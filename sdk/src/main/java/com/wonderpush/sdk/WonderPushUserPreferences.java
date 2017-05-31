package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WonderPushUserPreferences {

    private static final String DEFAULT_CHANNEL_NAME = "default";

    private static final String SERIALIZATION_FIELD_DEFAULT_CHANNEL_ID = "defaultChannelId";
    private static final String SERIALIZATION_FIELD_CHANNEL_GROUPS = "channelGroups";
    private static final String SERIALIZATION_FIELD_CHANNEL_PREFERENCES = "channelPreferences";

    private static String sDefaultChannelId;
    private static Map<String, WonderPushChannelGroup> sChannelGroups;
    private static Map<String, WonderPushChannelPreference> sChannelPreferences;

    static void initialize() {
        load();
        ensureDefaultChannelExists();
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
            JSONObject inChannels = inChannelPreferences.optJSONObject(SERIALIZATION_FIELD_CHANNEL_PREFERENCES);
            sChannelPreferences = new HashMap<>();
            if (inChannels != null) {
                Iterator<String> it = inChannels.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    JSONObject value = inChannels.optJSONObject(key);
                    try {
                        WonderPushChannelPreference pref = WonderPushChannelPreference.fromJSON(inChannels.optJSONObject(key));
                        sChannelPreferences.put(pref.getId(), pref);
                    } catch (JSONException ex) {
                        Log.e(WonderPush.TAG, "Failed to deserialize WonderPushChannelPreference from JSON: " + value, ex);
                    }
                }
            }
        }

        WonderPush.logDebug("UserPreferences: default channel id: " + sDefaultChannelId);
        WonderPush.logDebug("UserPreferences: channel groups:");
        for (WonderPushChannelGroup group : sChannelGroups.values()) {
            WonderPush.logDebug("- " + group.getId() + ": " + group);
        }
        WonderPush.logDebug("UserPreferences: channel preferences:");
        for (WonderPushChannelPreference channel : sChannelPreferences.values()) {
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
                for (WonderPushChannelPreference channel : sChannelPreferences.values()) {
                    outChannels.put(channel.getId(), channel.toJSON());
                }
                outChannelPreferences.put(SERIALIZATION_FIELD_CHANNEL_PREFERENCES, outChannels);
            }

            //WonderPush.logDebug("UserPreferences: saving preferences: " + outChannelPreferences);
            WonderPushConfiguration.setChannelPreferences(outChannelPreferences);
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Unexpected error while serializing channel preferences", ex);
        }
    }

    public static synchronized String getDefaultChannelId() {
        return sDefaultChannelId;
    }

    public static synchronized void setDefaultChannelId(String id) {
        if (id == null || "".equals(id)) id = DEFAULT_CHANNEL_NAME; // FIXME "" too?
        if (!id.equals(sDefaultChannelId)) {
            sDefaultChannelId = id;
            save();
            ensureDefaultChannelExists();
        }
    }

    static synchronized void ensureDefaultChannelExists() {
        if (getChannelPreference(sDefaultChannelId) == null) {
            // Put an empty channel in WonderPush, that overrides nothing
            putChannelPreference(
                    new WonderPushChannelPreference(sDefaultChannelId, null)
                            .setName("Default")
            );
        }
        // TODO if (Android ≥ O) ensure notification channel existence
    }

    public static synchronized WonderPushChannelGroup getChannelGroup(String groupId) {
        WonderPushChannelGroup rtn = sChannelGroups.get(groupId);
        if (rtn != null) {
            try {
                rtn = (WonderPushChannelGroup) rtn.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
        return rtn;
    }

    public static synchronized void removeChannelGroup(String groupId) {
        WonderPushChannelGroup prev = sChannelGroups.remove(groupId);
        if (prev != null) {
            save();
        }
    }

    public static synchronized void putChannelGroup(WonderPushChannelGroup channelGroup) {
        if (channelGroup == null) return;
        WonderPushChannelGroup prev = sChannelGroups.put(channelGroup.getId(), channelGroup);
        if (prev == null || !prev.equals(channelGroup)) {
            save();
        }
    }

    public static synchronized void setChannelGroups(List<WonderPushChannelGroup> channelGroups) {
        if (channelGroups == null) return;
        Map<String, WonderPushChannelGroup> newChannelGroups = new HashMap<>();
        for (WonderPushChannelGroup channelGroup : channelGroups) {
            newChannelGroups.put(channelGroup.getId(), channelGroup);
        }
        if (!newChannelGroups.equals(sChannelGroups)) {
            sChannelGroups = newChannelGroups;
            save();
        }
    }

    public static synchronized WonderPushChannelPreference getChannelPreference(String channelId) {
        WonderPushChannelPreference rtn = sChannelPreferences.get(channelId);
        if (rtn != null) {
            try {
                rtn = (WonderPushChannelPreference) rtn.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
        return rtn;
    }

    public static synchronized void removeChannelPreferences(String channelId) {
        WonderPushChannelPreference prev = sChannelPreferences.remove(channelId);
        if (prev != null) {
            save();
        }
    }

    public static synchronized void putChannelPreference(WonderPushChannelPreference channelPreference) {
        if (channelPreference == null) return;
        WonderPushChannelPreference prev = sChannelPreferences.put(channelPreference.getId(), channelPreference);
        if (prev == null || !prev.equals(channelPreference)) {
            save();
        }
    }

    public static synchronized void setChannelPreferences(List<WonderPushChannelPreference> channelPreferences) {
        if (channelPreferences == null) return;
        Map<String, WonderPushChannelPreference> newChannelPreferences = new HashMap<>();
        for (WonderPushChannelPreference channelPreference : channelPreferences) {
            newChannelPreferences.put(channelPreference.getId(), channelPreference);
        }
        if (!newChannelPreferences.equals(sChannelPreferences)) {
            sChannelPreferences = newChannelPreferences;
            save();
        }
    }

}
