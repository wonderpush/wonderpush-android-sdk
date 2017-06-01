package com.wonderpush.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WonderPushUserPreferences {

    private static final String DEFAULT_CHANNEL_NAME = "default";

    private static final String SERIALIZATION_FIELD_DEFAULT_CHANNEL_ID = "defaultChannelId";
    private static final String SERIALIZATION_FIELD_CHANNEL_GROUPS = "channelGroups";
    private static final String SERIALIZATION_FIELD_CHANNEL_PREFERENCES = "channelPreferences";

    private static String sDefaultChannelId;
    private static Map<String, WonderPushChannelGroup> sChannelGroups;
    private static Map<String, WonderPushChannelPreference> sChannelPreferences;

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
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while saving WonderPushUserPreferences", ex);
        }
    }

    public static synchronized String getDefaultChannelId() {
        return sDefaultChannelId;
    }

    public static synchronized void setDefaultChannelId(String id) {
        try {
            if (id == null || "".equals(id)) id = DEFAULT_CHANNEL_NAME; // FIXME "" too?
            if (!id.equals(sDefaultChannelId)) {
                sDefaultChannelId = id;
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while setting default channel id to " + id, ex);
        }
    }

    static synchronized void ensureDefaultAndroidNotificationChannelExists() {
        try {
            // TODO if (Android ≥ O) ensure notification channel existence
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while ensuring default channel exists", ex);
        }
    }

    public static synchronized WonderPushChannelGroup getChannelGroup(String groupId) {
        try {
            WonderPushChannelGroup rtn = sChannelGroups.get(groupId);
            if (rtn != null) {
                try {
                    rtn = (WonderPushChannelGroup) rtn.clone();
                } catch (CloneNotSupportedException ex) {
                    Log.e(WonderPush.TAG, "Unexpected error while cloning gotten channel preference " + rtn, ex);
                    return null;
                }
            }
            return rtn;
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while getting channel group " + groupId, ex);
            return null;
        }
    }

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

    public static synchronized WonderPushChannelPreference getChannelPreference(String channelId) {
        try {
            WonderPushChannelPreference rtn = sChannelPreferences.get(channelId);
            if (rtn != null) {
                try {
                    rtn = (WonderPushChannelPreference) rtn.clone();
                } catch (CloneNotSupportedException ex) {
                    Log.e(WonderPush.TAG, "Unexpected error while cloning gotten channel preference " + rtn, ex);
                    return null;
                }
            }
            return rtn;
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while getting channel preference " + channelId, ex);
            return null;
        }
    }

    public static synchronized void removeChannelPreference(String channelId) {
        try {
            if (_removeChannelPreference(channelId)) {
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while removing channel preference " + channelId, ex);
        }
    }

    private static synchronized boolean _removeChannelPreference(String channelId) {
        WonderPushChannelPreference prev = sChannelPreferences.remove(channelId);
        // TODO Android ≥ O remove channel
        return prev != null;
    }

    public static synchronized void putChannelPreference(WonderPushChannelPreference channelPreference) {
        try {
            if (_putChannelPreference(channelPreference)) {
                save();
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while putting channel preference " + channelPreference, ex);
        }
    }

    private static synchronized boolean _putChannelPreference(WonderPushChannelPreference channelPreference) {
        if (channelPreference == null) return false;
        WonderPushChannelPreference prev = sChannelPreferences.put(channelPreference.getId(), channelPreference);
        // TODO Android ≥ O create channel
        return prev == null || !prev.equals(channelPreference);
    }

    public static synchronized void setChannelPreferences(Collection<WonderPushChannelPreference> channelPreferences) {
        if (channelPreferences == null) return;
        boolean save = false;
        try {
            Set<String> channelIdsToRemove = new HashSet<>(sChannelPreferences.keySet());
            for (WonderPushChannelPreference channelPreference : channelPreferences) {
                if (channelPreference == null) continue;
                channelIdsToRemove.remove(channelPreference.getId());
                save = save || _putChannelPreference(channelPreference);
            }
            for (String groupId : channelIdsToRemove) {
                save = save || _removeChannelGroup(groupId);
            }
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while setting channel preferences " + channelPreferences, ex);
        } finally {
            try {
                if (save) {
                    save();
                }
            } catch (Exception ex) {
                Log.e(WonderPush.TAG, "Unexpected error while setting channel preferences " + channelPreferences, ex);
            }
        }
    }

}
