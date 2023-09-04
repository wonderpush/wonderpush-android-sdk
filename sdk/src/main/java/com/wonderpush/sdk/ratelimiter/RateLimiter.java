package com.wonderpush.sdk.ratelimiter;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class RateLimiter {
    protected static final String TAG = "WonderPushRateLimiter";

    private static class RateLimiterData {
        @NonNull
        public final String key;
        @NonNull
        public final List<Long> incrementDates;

        public RateLimiterData(@NonNull String key) {
            this.key = key;
            this.incrementDates = new ArrayList<>();
        }

        public void removeIncrementsOlderThan(long timeToLive) {
            long start = new Date().getTime() - timeToLive;
            while (incrementDates.size() > 0 && incrementDates.get(0) < start) {
                incrementDates.remove(0);
            }
        }
    }

    public static class MissingSharedPreferencesException extends Exception {
    }

    private static RateLimiter sInstance;
    private static Callable<SharedPreferences> sSharedPreferencesProvider;

    public static void initialize(Callable<SharedPreferences> sharedPreferencesProvider) {
        sSharedPreferencesProvider = sharedPreferencesProvider;
    }

    public static RateLimiter getInstance() throws MissingSharedPreferencesException {
        if (sInstance == null) {
            try {
                sInstance = new RateLimiter(sSharedPreferencesProvider.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sInstance;
    }

    @NonNull
    private final SharedPreferences sharedPreferences;
    @NonNull
    private final Map<String, RateLimiterData> limiterData;

    protected RateLimiter(@NonNull SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.limiterData = new HashMap<>();

        String serialized = sharedPreferences.getString(SharedPreferencesKey, null);
        if (serialized != null) {
            try {
                JSONObject data = new JSONObject(serialized);
                Iterator<String> iterator = data.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    JSONArray incrementDates = data.getJSONArray(key);
                    RateLimiterData rateLimiterData = new RateLimiterData(key);
                    for (int i = 0; i < incrementDates.length(); i++) {
                        rateLimiterData.incrementDates.add(incrementDates.getLong(i));
                    }
                    this.limiterData.put(rateLimiterData.key, rateLimiterData);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Could not read rate limiter data", e);
            }
        }
    }

    private static final String SharedPreferencesKey = "__RateLimiter";

    private synchronized void save() {
        try {

            JSONObject data = new JSONObject();
            for (Map.Entry<String, RateLimiterData> entry : limiterData.entrySet()) {
                JSONArray incrementDates = new JSONArray();
                for (Long incrementDate : entry.getValue().incrementDates) {
                    incrementDates.put(incrementDate);
                }
                data.put(entry.getKey(), incrementDates);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SharedPreferencesKey, data.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "Could not save rate limiter data", e);
        }
    }

    public synchronized void increment(RateLimit rateLimit) {
        RateLimiterData existingData = limiterData.get(rateLimit.key);
        RateLimiterData data = existingData != null ? existingData : new RateLimiterData(rateLimit.key);

        // Remove all dates prior to the rateLimit's timeToLive
        data.removeIncrementsOlderThan(rateLimit.timeToLive);

        // Increment
        data.incrementDates.add(new Date().getTime());

        // Store
        limiterData.put(data.key, data);
        save();
    }

    public synchronized boolean isRateLimited(RateLimit rateLimit) {
        RateLimiterData data = limiterData.get(rateLimit.key);
        if (data == null) return false;

        // Remove all dates prior to the rateLimit's timeToLive
        data.removeIncrementsOlderThan(rateLimit.timeToLive);
        save();

        return data.incrementDates.size() >= rateLimit.limit;
    }

    public synchronized void clear(RateLimit rateLimit) {
        limiterData.remove(rateLimit.key);
        save();
    }

}
