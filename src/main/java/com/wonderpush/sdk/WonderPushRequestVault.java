package com.wonderpush.sdk;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.http.NoHttpResponseException;
import org.json.JSONException;

import android.util.Log;

import com.wonderpush.sdk.WonderPush.Response;

/**
 * This class will make sure important {@link WonderPushRestClient.Request} objects are run eventually, even if the user
 * is currently offline and the app terminated.
 */
class WonderPushRequestVault {

    private static final String TAG = WonderPush.TAG;

    private static WonderPushRequestVault sDefaultVault;

    private static final int NORMAL_WAIT = 10 * 1000;
    private static final float BACKOFF_EXPONENT = 1.5f;
    private static final int MAXIMUM_WAIT = 5 * 60 * 1000;
    private static int sWait = NORMAL_WAIT;

    protected static WonderPushRequestVault getDefaultVault() {
        return sDefaultVault;
    }

    /**
     * Start the default vault.
     */
    protected static void initialize() {
        if (null == sDefaultVault) {
            sDefaultVault = new WonderPushRequestVault(WonderPushJobQueue.getDefaultQueue());
        }
    }

    private WonderPushJobQueue mJobQueue;
    private Thread mThread;

    WonderPushRequestVault(WonderPushJobQueue jobQueue) {
        mJobQueue = jobQueue;
        mThread = new Thread(getRunnable());
        mThread.start();
    }

    /**
     * Save a request in the vault for future retry
     *
     * @param request
     * @throws JSONException
     */
    protected void put(WonderPushRestClient.Request request) throws JSONException {
        mJobQueue.postJobWithDescription(request.toJSON());
    }

    private Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        // Sleep
                        Thread.sleep(sWait);

                        // Blocks
                        final WonderPushJobQueue.Job job = mJobQueue.nextJob();

                        final WonderPushRestClient.Request request;
                        try {
                            request = new WonderPushRestClient.Request(job.getJobDescription());
                        } catch (Exception e) {
                            Log.e(TAG, "Could not restore request", e);
                            continue;
                        }

                        request.setHandler(new WonderPush.ResponseHandler() {
                            @Override
                            public void onFailure(Throwable e, Response errorResponse) {
                                // Post back to job queue if this is a network error
                                if (e instanceof NoHttpResponseException
                                        || e instanceof UnknownHostException
                                        || e instanceof SocketException) {
                                    backoff();
                                    mJobQueue.post(job);
                                    return;
                                }
                            }

                            @Override
                            public void onSuccess(Response response) {
                                resetBackoff();
                            }
                        });
                        WonderPushRestClient.requestAuthenticated(request);
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, "Vault interrupted", e);
                }

            }
        };
    }

    private static void backoff() {
        sWait = Math.min(MAXIMUM_WAIT, Math.round(sWait * BACKOFF_EXPONENT));
        WonderPush.logDebug("Increasing backoff to " + sWait/1000.f + "s");
    }

    private static void resetBackoff() {
        sWait = NORMAL_WAIT;
    }

}
