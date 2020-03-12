package com.wonderpush.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

class DeferredFuture<V> {

    private static class State<V> {
        final boolean cancelled;
        final boolean interrupt;
        final boolean completed;
        final V value;
        final Throwable exception;

        State(boolean cancelled, boolean interrupt, boolean completed, V value, Throwable exception) {
            this.cancelled = cancelled;
            this.interrupt = interrupt;
            this.completed = completed;
            this.value = value;
            this.exception = exception;
        }
    }

    class FutureImpl implements Future<V> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return settleState(new State<V>(true, mayInterruptIfRunning, false, null, null));
        }

        @Override
        public boolean isCancelled() {
            State<V> curState = state.get();
            return curState.cancelled;
        }

        @Override
        public boolean isDone() {
            State<V> curState = state.get();
            return curState.cancelled || curState.completed;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            settled.await();
            return getReturn();
        }

        @Override
        public V get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (!settled.await(timeout, unit)) throw new TimeoutException();
            return getReturn();
        }

        private V getReturn() throws CancellationException, ExecutionException {
            State<V> curState = state.get();
            if (curState.cancelled) throw new CancellationException();
            assert curState.completed;
            if (curState.exception != null) throw new ExecutionException(curState.exception);
            return curState.value;
        }

    }

    private final Future<V> future = new FutureImpl();
    private final CountDownLatch settled = new CountDownLatch(1);
    private final AtomicReference<State<V>> state = new AtomicReference<>(new State<V>(false, false, false, null, null));

    public Future<V> getFuture() {
        return future;
    }

    public boolean set(@Nullable V value) {
        return settleState(new State<>(false, false, true, value, null));
    }

    public boolean setException(@NonNull Throwable ex) {
        return settleState(new State<V>(false, false, true, null, ex));
    }

    private boolean settleState(State<V> newState) {
        assert newState.cancelled != newState.completed; // a settled state is either cancelled or done, but not neither nor both
        State<V> curState = state.get();
        boolean couldSetState = !curState.cancelled && !curState.completed && state.compareAndSet(curState, newState);
        if (couldSetState) {
            settled.countDown();
        }
        return couldSetState;
    }

}
