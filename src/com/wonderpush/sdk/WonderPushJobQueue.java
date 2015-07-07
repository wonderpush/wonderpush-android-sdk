package com.wonderpush.sdk;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * A simple persistent job queue which saves itself on disk using SharedPreferences
 * and wraps an {@link ArrayBlockingQueue}.
 */
class WonderPushJobQueue {

    private static final String TAG = WonderPush.TAG;

    private static int DEFAULT_CAPACITY = 100;

    /**
     * Queued objects.
     */
    protected interface Job {

        public String getId();

        public JSONObject getJobDescription();

        public void repost();

    }

    private static WonderPushJobQueue sDefaultQueue = new WonderPushJobQueue("DefaultWonderPushJobQueue", DEFAULT_CAPACITY);

    /**
     * Returns the default job queue.
     */
    protected static WonderPushJobQueue getDefaultQueue() {
        return sDefaultQueue;
    }

    private String mQueueName;
    private Object mMutex = new Object();
    private ArrayBlockingQueue<InternalJob> mQueue;

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
        mQueue = new ArrayBlockingQueue<WonderPushJobQueue.InternalJob>(capacity);
        restore();
    }

    /**
     * Creates and stores a job in the queue based on the provided description
     *
     * @param jobDescription
     * @return The stored job or null if something went wrong (the queue is full for instance)
     */
    protected synchronized Job postJobWithDescription(JSONObject jobDescription) {
        if (0 == mQueue.remainingCapacity())
            return null;

        String jobId = UUID.randomUUID().toString();
        InternalJob job = new InternalJob(jobId, jobDescription);
        mQueue.add(job);
        save();
        return job;
    }

    /**
     * This call blocks until the next job is available.
     *
     * @throws InterruptedException
     */
    protected Job nextJob() throws InterruptedException {
        InternalJob job = mQueue.take();
        save();
        return job;
    }

    private String getPrefName() {
        return String.format("_wonderpush_job_queue_%s", mQueueName);
    }

    /**
     * Saves the job queue on disk.
     */
    protected void save() {
        synchronized (mMutex) {
            try {
                InternalJob jobs[] = new InternalJob[mQueue.size()];
                mQueue.toArray(jobs);
                JSONArray jsonArray = new JSONArray();
                for (int i = 0 ; i < jobs.length ; i++) {
                    jsonArray.put(jobs[i].toJSON());
                }

                SharedPreferences prefs = WonderPushConfiguration.getSharedPreferences();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(getPrefName(), jsonArray.toString());
                editor.commit();

            } catch (JSONException e) {
                Log.e(TAG, "Could not save job queue", e);
            } catch (Exception e) {
                Log.e(TAG, "Could not save job queue", e);
            }
        }
    }

    /**
     * Restores the job queue from its on-disk version.
     */
    protected void restore() {
        synchronized (mMutex) {
            try {
                SharedPreferences prefs = WonderPushConfiguration.getSharedPreferences();
                String jsonString = prefs.getString(getPrefName(), "[]");
                JSONArray jsonArray = new JSONArray(jsonString);

                mQueue.clear();

                for (int i = 0 ; i < jsonArray.length() ; i++) {
                    mQueue.add(new InternalJob(jsonArray.getJSONObject(i)));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Could not restore job queue", e);
            } catch (Exception e) {
                Log.e(TAG, "Could not restore job queue", e);
            }
        }
    }

    private class InternalJob implements Job {

        protected String mId;
        protected JSONObject mJobDescription;

        public InternalJob(String id, JSONObject description) {
            mId = id;
            mJobDescription = description;
        }

        public InternalJob(JSONObject json) throws JSONException {
            mId = json.getString("id");
            mJobDescription = json.getJSONObject("description");
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", mId);
            json.put("description", mJobDescription);
            return json;
        }

        public String getId() {
            return mId;
        }

        public JSONObject getJobDescription() {
            return mJobDescription;
        }

        public void repost() {
            mQueue.add(this);
            save();
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
