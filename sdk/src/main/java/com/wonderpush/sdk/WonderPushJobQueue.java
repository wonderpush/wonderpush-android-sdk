package com.wonderpush.sdk;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * A simple persistent job queue which saves itself on disk using SharedPreferences
 * and wraps an {@link ArrayBlockingQueue}.
 */
class WonderPushJobQueue {

    private static final String TAG = WonderPush.TAG;

    private static final int DEFAULT_CAPACITY = 32;

    /**
     * Queued objects.
     */
    protected interface Job {

        String getId();

        JSONObject getJobDescription();

        long getNotBeforeRealtimeElapsed();

    }

    private static final WonderPushJobQueue sDefaultQueue = new WonderPushJobQueue("DefaultWonderPushJobQueue", DEFAULT_CAPACITY);
    private static final WonderPushJobQueue sMeasurementsApiQueue = new WonderPushJobQueue("WonderPushMeasurementsApiJobQueue", DEFAULT_CAPACITY);

    /**
     * Returns the default job queue.
     */
    protected static WonderPushJobQueue getDefaultQueue() {
        return sDefaultQueue;
    }

    protected static WonderPushJobQueue getMeasurementsApiQueue() {
        return sMeasurementsApiQueue;
    }
    private final String mQueueName;
    private final PriorityBlockingQueue<Job> mQueue;

    /**
     * Creates a queue with the specified name
     *
     * @param queueName
     *            The name of the queue, which determines the queue's storage location
     * @param capacity
     *            The maximum number of jobs the queue can hold
     */
    WonderPushJobQueue(String queueName, int capacity) {
        mQueueName = queueName;
        mQueue = new PriorityBlockingQueue<>(capacity, new Comparator<Job>() {
            @Override
            public int compare(Job lhs, Job rhs) {
                // Sort by increasing time
                long diff = lhs.getNotBeforeRealtimeElapsed() - rhs.getNotBeforeRealtimeElapsed();
                // Then break ties using the ids
                if (diff == 0) {
                    diff = lhs.getId().compareTo(rhs.getId());
                }
                return (int) diff;
            }
        });
        restore();
    }

    /**
     * Creates and stores a job in the queue based on the provided description
     *
     * @return The stored job or null if something went wrong (the queue is full for instance)
     */
    protected Job postJobWithDescription(JSONObject jobDescription, long notBeforeRealtimeElapsed) {
        String jobId = UUID.randomUUID().toString();
        InternalJob job = new InternalJob(jobId, jobDescription, notBeforeRealtimeElapsed);
        return post(job);
    }

    /**
     * Stores an existing job in the queue.
     *
     * @return The input job or null if something went wrong (the queue is full for instance)
     */
    protected Job post(Job job) {
        if (mQueue.offer(job)) {
            save();
            return job;
        } else {
            return null;
        }
    }

    protected long peekNextJobNotBeforeRealtimeElapsed() {
        Job head = mQueue.peek();
        if (head == null) return Long.MAX_VALUE;
        return head.getNotBeforeRealtimeElapsed();
    }

    /**
     * This call blocks until the next job is available.
     *
     * @throws InterruptedException
     */
    protected Job nextJob() throws InterruptedException {
        Job job = mQueue.take();
        save();
        return job;
    }

    private String getPrefName() {
        return String.format("_wonderpush_job_queue_%s", mQueueName);
    }

    /**
     * Saves the job queue on disk.
     */
    protected synchronized void save() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Job job : mQueue) {
                if (!(job instanceof InternalJob)) continue;
                InternalJob internalJob = (InternalJob) job;
                jsonArray.put(internalJob.toJSON());
            }

            SharedPreferences prefs = WonderPushConfiguration.getSharedPreferences();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getPrefName(), jsonArray.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "Could not save job queue", e);
        } catch (Exception e) {
            Log.e(TAG, "Could not save job queue", e);
        }
    }

    /**
     * Restores the job queue from its on-disk version.
     */
    protected synchronized void restore() {
        try {
            SharedPreferences prefs = WonderPushConfiguration.getSharedPreferences();
            String jsonString = prefs.getString(getPrefName(), "[]");
            JSONArray jsonArray = new JSONArray(jsonString);

            mQueue.clear();

            for (int i = 0 ; i < jsonArray.length() ; i++) {
                try {
                    mQueue.add(new InternalJob(jsonArray.getJSONObject(i)));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed to restore malformed job", ex);
                } catch (Exception ex) {
                    Log.e(TAG, "Unexpected error while restoring a job", ex);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Could not restore job queue", e);
        } catch (Exception e) {
            Log.e(TAG, "Could not restore job queue", e);
        }
    }

    private static class InternalJob implements Job {

        protected String mId;
        protected JSONObject mJobDescription;
        protected long mNotBeforeRealtimeElapsed;

        public InternalJob(String id, JSONObject description, long notBeforeRealtimeElapsed) {
            mId = id;
            mJobDescription = description;
            mNotBeforeRealtimeElapsed = notBeforeRealtimeElapsed;
        }

        public InternalJob(JSONObject json) throws JSONException {
            mId = json.getString("id");
            mJobDescription = json.getJSONObject("description");
            mNotBeforeRealtimeElapsed = -1;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", mId);
            json.put("description", mJobDescription);
            // (Do not persist mNotBeforeRealtimeElapsed, such delay is not supposed to be saved after application death)
            return json;
        }

        public String getId() {
            return mId;
        }

        public JSONObject getJobDescription() {
            return mJobDescription;
        }

        @Override
        public long getNotBeforeRealtimeElapsed() {
            return mNotBeforeRealtimeElapsed;
        }

        @Override
        public int hashCode() {
            if (mId == null) return 0;
            return mId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof InternalJob)) return false;
            InternalJob other = (InternalJob) obj;

            if (mId == null) {
                if (other.mId != null)
                    return false;
            } else if (!mId.equals(other.mId))
                return false;

            return true;
        }

    }

}
