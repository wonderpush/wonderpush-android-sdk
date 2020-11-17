package com.wonderpush.sdk;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public abstract class SafeOkHttpCallback implements Callback {

    protected abstract void onFailureSafe(Call call, IOException e);
    protected abstract void onResponseSafe(Call call, Response response) throws IOException;

    @Override
    public void onFailure(Call call, IOException e) {
        try {
            onFailureSafe(call, e);
        } catch (Throwable t) {
            Log.e(WonderPush.TAG, "Unexpected exception", t);
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        try {
            onResponseSafe(call, response);
        } catch (IOException ex) {
            throw ex;
        } catch (Throwable t) {
            Log.e(WonderPush.TAG, "Unexpected exception", t);
        }
    }
}
