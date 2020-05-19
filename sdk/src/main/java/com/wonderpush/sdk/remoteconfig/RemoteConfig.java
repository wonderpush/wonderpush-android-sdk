package com.wonderpush.sdk.remoteconfig;

import com.wonderpush.sdk.SimpleVersion;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

public class RemoteConfig {

    public static RemoteConfig with(@Nonnull JSONObject data, @Nonnull String version, @Nonnull Date fetchDate, long maxAge) {
        return new RemoteConfig(data, version, fetchDate, maxAge);
    }

    public static RemoteConfig with(@Nonnull JSONObject data, @Nonnull String version, @Nonnull Date fetchDate) {
        return new RemoteConfig(data, version, fetchDate, Constants.REMOTE_CONFIG_DEFAULT_MAXIMUM_CONFIG_AGE);
    }

    public static RemoteConfig with(@Nonnull JSONObject data, @Nonnull String version) {
        return new RemoteConfig(data, version, DateHelper.now(), Constants.REMOTE_CONFIG_DEFAULT_MAXIMUM_CONFIG_AGE);
    }

    @Nonnull
    private JSONObject data;
    @Nonnull
    private String version;
    @Nonnull
    private Date fetchDate;
    private long maxAge;

    private RemoteConfig(@Nonnull JSONObject data, @Nonnull String version, @Nonnull Date fetchDate, long maxAge) {
        this.data = data;
        SimpleVersion simpleVersion = new SimpleVersion(version);
        this.version = simpleVersion.isValid() ? simpleVersion.toString() : version;
        this.fetchDate = fetchDate;
        this.maxAge = maxAge;
    }

    /**
     * Tries to deserializes a remote config from the given string, usually generated by calling toString() on a RemoteConfig object.
     * @param remoteConfigString
     * @return
     */
    public static @Nullable RemoteConfig with(String remoteConfigString) {
        try {
            JSONObject json = new JSONObject(remoteConfigString);
            JSONObject data = json.getJSONObject("data");
            String version = json.getString("version");
            Long maxAge = json.getLong("maxAge");
            Long fetchTime = json.getLong("fetchDate");
            return RemoteConfig.with(data, version, new Date(fetchTime), maxAge);
        } catch (JSONException e) {
        }
        return null;
    }

    public boolean hasHigherVersionThan(RemoteConfig other) {
        return compareVersions(version, other.version) > 0;
    }

    public boolean isExpired() {
        return DateHelper.now().getTime() > (fetchDate.getTime() + maxAge);
    }

    /**
     * Compares version1 and version2.
     * @param version1
     * @param version2
     * @return 1 when version1 > version2, -1 when version1 < version2 and 0 when versions are equivalent
     */
    public static int compareVersions(String version1, String version2) {
        SimpleVersion ours = new SimpleVersion(version1);
        SimpleVersion theirs = new SimpleVersion(version2);
        return ours.compareTo(theirs);
    }

    public String getVersion() {
        return version;
    }

    public Date getFetchDate() {
        return fetchDate;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public JSONObject getData() {
        return data;
    }

    @Override
    public String toString() {
        try {
            JSONObject json = new JSONObject();
            json.put("data", data);
            json.put("version", version);
            json.put("fetchDate", fetchDate.getTime());
            json.put("maxAge", maxAge);
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }
}
