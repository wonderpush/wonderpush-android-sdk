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
                        // Sleep for 10 seconds
                        Thread.sleep(1000 * 10);

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
                                    mJobQueue.post(job);
                                    return;
                                }
                            }

                            @Override
                            public void onSuccess(Response response) {
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

}
