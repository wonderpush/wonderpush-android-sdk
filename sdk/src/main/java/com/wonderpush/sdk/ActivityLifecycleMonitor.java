package com.wonderpush.sdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Monitors activities lifecycle operations, which are evidences of user interactions.
 * @link http://www.mjbshaw.com/2012/12/determining-if-your-android-application.html
 */
class ActivityLifecycleMonitor {

    private static final Monitor sSingleton = new Monitor();
    private static boolean sActivityLifecycleCallbacksRegistered;
    private static final WeakHashMap<Activity, Object> sTrackedActivities = new WeakHashMap<>();

    protected static void monitorActivitiesLifecycle() {
        if (!sActivityLifecycleCallbacksRegistered && WonderPush.sApplication != null) {
            WonderPush.sApplication.registerActivityLifecycleCallbacks(sSingleton);
            sActivityLifecycleCallbacksRegistered = true;
        }
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

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (!hasCreatedActivities()) {
                createFirstDate = TimeSync.getTime();
            }
            ++createCount;
            WonderPush.showPotentialNotification(activity, activity.getIntent());
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
            WonderPush.onInteraction(false);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (!hasResumedActivities()) {
                resumeFirstDate = TimeSync.getTime();
            }
            lastResumedActivityRef = new WeakReference<>(activity);
            ++resumeCount;
            WonderPush.onInteraction(false);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            ++pausedCount;
            if (!hasResumedActivities()) {
                pausedLastDate = TimeSync.getTime();
            }
            WonderPush.onInteraction(true);
        }

        @Override
        public void onActivityStopped(Activity activity) {
            ++stopCount;
            if (!hasStartedActivities()) {
                stopLastDate = TimeSync.getTime();
            }
            if (!activity.isFinishing()) {
                lastStoppedActivityRef = new WeakReference<>(activity);
            }
            WonderPush.onInteraction(true);
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
