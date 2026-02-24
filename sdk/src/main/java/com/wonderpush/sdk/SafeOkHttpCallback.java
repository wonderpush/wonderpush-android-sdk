package com.wonderpush.sdk;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.Response;

public abstract class SafeOkHttpCallback implements Callback {

    /**
     * Returns a {@link Dispatcher} backed by an executor that catches any {@link Throwable}
     * escaping a task. This prevents OkHttp's re-throw of non-{@link IOException}s thrown by
     * interceptors from crashing the app: OkHttp calls {@code onFailure} first (handled by
     * {@link SafeOkHttpCallback}), then unconditionally re-throws — this wrapper absorbs that
     * re-throw.
     */
    public static Dispatcher buildSafeDispatcher(String name) {
        if (name == null) name = "WpSafeOkHttp";
        final String _name = name;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r, _name + " Dispatcher");
                    t.setDaemon(false);
                    return t;
                }
        ) {
            @Override
            public void execute(Runnable command) {
                super.execute(() -> {
                    try {
                        command.run();
                    } catch (Throwable t) {
                        Log.e(WonderPush.TAG + "." + _name, "Interceptor threw non-IOException (already reported via onFailure)", t);
                    }
                });
            }
        };
        return new Dispatcher(executor);
    }

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
