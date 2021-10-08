package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Monitors activities lifecycle operations, which are evidences of user interactions.
 * @link http://www.mjbshaw.com/2012/12/determining-if-your-android-application.html
 */
class ActivityLifecycleMonitor {

    public interface ResumeListener {
        void onResume(Activity activity);
    }

    private static final Monitor sSingleton = new Monitor();
    private static boolean sActivityLifecycleCallbacksRegistered;
    private static final WeakHashMap<Activity, Object> sTrackedActivities = new WeakHashMap<>();
    protected static void monitorActivitiesLifecycle() {
        if (!sActivityLifecycleCallbacksRegistered && WonderPush.sApplication != null) {
            WonderPush.sApplication.registerActivityLifecycleCallbacks(sSingleton);
            sActivityLifecycleCallbacksRegistered = true;
        }
    }

    static void onNextResume(ResumeListener listener) {
        sSingleton.onNextResume(listener);
    }

    protected static void addTrackedActivity(Activity activity) {
        sTrackedActivities.put(activity, null);
    }

    protected static Activity getCurrentActivity() {
        Activity candidate = null;
        if (sActivityLifecycleCallbacksRegistered
                && sSingleton.hasResumedActivities()) {
            candidate = sSingleton.getLastResumedActivity();
        }
        if (candidate == null) {
            for (Activity activity : sTrackedActivities.keySet()) {
                if (activity.hasWindowFocus() && !activity.isFinishing()) {
                    candidate = activity;
                    break;
                }
            }
        }
        return candidate;
    }

    protected static Activity getLastStoppedActivity() {
        Activity candidate = null;
        if (sActivityLifecycleCallbacksRegistered) {
            candidate = sSingleton.getLastStoppedActivity();
        }
        return candidate;
    }

    static class Monitor implements Application.ActivityLifecycleCallbacks {

        private void updatePresence(boolean start) {
            try {
                PresenceManager manager = WonderPush.getPresenceManager();
                if (start && manager.isCurrentlyPresent()) return;
                PresenceManager.PresencePayload payload = start
                        ? manager.presenceDidStart()
                        : manager.presenceWillStop();
                if (payload != null) {
                    JSONObject data = new JSONObject();
                    data.put("presence", payload.toJSONObject());
                    WonderPush.trackInternalEvent("@PRESENCE", data);
                }
            } catch (JSONException e) {
                WonderPush.logError("Could not serialize presence", e);
            } catch (InterruptedException e) {
                WonderPush.logError("Could not start presence", e);
            }

        }

        private int createCount;
        private int startCount;
        private int resumeCount;
        private int pausedCount;
        private int stopCount;
        private int destroyCount;

        private long createFirstDate;
        private long startFirstDate;
        private long resumeFirstDate;
        private long pausedLastDate;
        private long stopLastDate;
        private long destroyLastDate;

        private WeakReference<Activity> lastResumedActivityRef = new WeakReference<>(null);
        private WeakReference<Activity> lastStoppedActivityRef = new WeakReference<>(null);

        private List<ResumeListener> onNextResumeListeners = new ArrayList<>();
        synchronized void onNextResume(ResumeListener listener) {
            onNextResumeListeners.add(listener);
        }

        private synchronized void callOnNextResumeListeners(Activity activity) {
            List<ResumeListener> listeners = new ArrayList<>(onNextResumeListeners);
            onNextResumeListeners = new ArrayList<>();
            for (ResumeListener l : listeners) {
                l.onResume(activity);
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (!hasCreatedActivities()) {
                createFirstDate = TimeSync.getTime();
            }
            ++createCount;
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (createCount == 0) {
                // The monitor was probably setup inside a Activity.onCreate() call
                this.onActivityCreated(activity, null);
            }
            if (!hasStartedActivities()) {
                startFirstDate = TimeSync.getTime();
            }
            ++startCount;
            updatePresence(true);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (!hasResumedActivities()) {
                resumeFirstDate = TimeSync.getTime();
            }
            lastResumedActivityRef = new WeakReference<>(activity);
            ++resumeCount;
            WonderPush.injectAppOpenIfNecessary();
            updatePresence(true);
            WonderPush.showPotentialNotification(activity, activity.getIntent());
            WonderPushConfiguration.setLastInteractionDate(TimeSync.getTime());
            callOnNextResumeListeners(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            ++pausedCount;
            if (!hasResumedActivities()) {
                pausedLastDate = TimeSync.getTime();
            }
            WonderPushConfiguration.setLastInteractionDate(TimeSync.getTime());
        }

        @Override
        public void onActivityStopped(Activity activity) {
            ++stopCount;
            if (!hasStartedActivities()) {
                stopLastDate = TimeSync.getTime();
                try {
                    updatePresence(false);
                } catch (Exception e) {
                    Log.d(WonderPush.TAG, "Unexpected error while updating presence", e);
                }
            }
            if (!activity.isFinishing()) {
                lastStoppedActivityRef = new WeakReference<>(activity);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            ++destroyCount;
            if (!hasCreatedActivities()) {
                destroyLastDate = TimeSync.getTime();
            }
        }

        protected boolean hasResumedActivities() {
            return resumeCount > pausedCount;
        }

        protected boolean hasStartedActivities() {
            return startCount > stopCount;
        }

        protected boolean hasCreatedActivities() {
            return createCount > destroyCount;
        }

        protected Activity getLastResumedActivity() {
            return lastResumedActivityRef.get();
        }

        protected Activity getLastStoppedActivity() {
            return lastStoppedActivityRef.get();
        }

        protected long getCreateFirstDate() {
            return createFirstDate;
        }

        protected long getStartFirstDate() {
            return startFirstDate;
        }

        protected long getResumeFirstDate() {
            return resumeFirstDate;
        }

        protected long getPausedLastDate() {
            return pausedLastDate;
        }

        protected long getStopLastDate() {
            return stopLastDate;
        }

        protected long getDestroyLastDate() {
            return destroyLastDate;
        }

    }

}
