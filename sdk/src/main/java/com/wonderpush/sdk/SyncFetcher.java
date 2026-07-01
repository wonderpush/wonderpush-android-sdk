package com.wonderpush.sdk;

import java.util.Map;

/**
 * The explicit-fetch loop for the sdk-sync channel.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncFetcher} /
 * wonderpush-javascript-sdk {@code sync.js} (algorithm.md:245-257):
 *   1. weak-signal debounce + per-source rate-limit guards (skip if too soon),
 *   2. acquire the per-source mutex (skip if a fetch is already in flight),
 *   3. stamp lastFetchAttemptedDate + increment the failure count + persist BEFORE the call,
 *   4. exponential-backoff sleep,
 *   5. issue the GET (the response is processed by the incoming interceptor),
 *   6. on success reset the failure count; on failure leave it (longer next backoff),
 *   7. always release the mutex.
 *
 * The network and the clock are injected so the loop's timing/locking/bookkeeping is unit-testable.
 *
 * NOTE: multi-object paging continuations land with the popups source.
 */
class SyncFetcher implements SyncFetching {

    interface Scheduler {
        /** Run {@code block} after {@code delayMs} (immediately if &lt;= 0). */
        void schedule(double delayMs, Runnable block);
    }

    interface RandomProvider {
        /** A [0,1) value for backoff jitter. */
        double nextDouble();
    }

    private final SyncStateStore stateStore;
    private final SyncFetchTransport transport;

    /** Current time in ms. Default: server-adjusted clock. Overridable in tests. */
    SyncClock nowProvider;
    /** Run a block after a delay. Default: {@link WonderPush#safeDefer}. Overridable in tests. */
    Scheduler scheduler;
    /** Backoff jitter source. Default: {@link Math#random}. Overridable in tests. */
    RandomProvider randomProvider;

    SyncFetcher(SyncStateStore stateStore, SyncFetchTransport transport) {
        this.stateStore = stateStore;
        this.transport = transport;
        this.nowProvider = TimeSync::getTime;
        this.scheduler = (delayMs, block) -> {
            if (delayMs <= 0) block.run();
            else WonderPush.safeDefer(block, (long) delayMs);
        };
        this.randomProvider = Math::random;
    }

    private static void done(Completion completion, boolean attempted) {
        if (completion != null) completion.onComplete(attempted);
    }

    @Override
    public void fetchSource(final String source, final String userId, final String deviceId,
                            final org.json.JSONObject identifiers, final SyncKnobs knobs,
                            final boolean weak, final SyncFetchHint hint, final Completion completion) {
        String path = SyncFetchPolicy.explicitPathForSource(source);
        if (path == null) { done(completion, false); return; }   // unknown source

        SyncSourceState state = stateStore.loadSource(source, userId, deviceId);
        long now = nowProvider.now();

        // Step 1: guards. Weak signals are debounced; ALL triggers are subject to the rate-limit floor.
        if (weak && SyncFetchPolicy.shouldDebounceWeakSignal(now, state.lastFetchAttemptedDate,
                knobs.weakSyncSignalDebounceMs)) {
            done(completion, false); return;
        }
        if (SyncFetchPolicy.shouldRateLimitSource(now, state.lastFetchAttemptedDate,
                knobs.minSourceFetchIntervalMs)) {
            done(completion, false); return;
        }

        // Step 2: acquire the per-source fetch mutex (non-blocking — skip if already in flight).
        final SyncMutex mutex = SyncMutex.named("sync:" + source);
        final long token = mutex.tryLock(now, knobs.mutexTtlMs);
        if (token == 0) { done(completion, false); return; }

        // Step 3: stamp the attempt + compute backoff + bump the failure count, persisted BEFORE the call.
        double sleepMs = SyncFetchPolicy.computeBackoffSleep(state.lastFetchUnsuccessfulAttemptCount,
                randomProvider.nextDouble(), knobs);
        state.lastFetchAttemptedDate = now;
        state.lastFetchUnsuccessfulAttemptCount += 1;
        stateStore.saveState(state, source, userId, deviceId);

        final String finalPath = path;
        final Map<String, Object> params = SyncFetchPolicy.buildExplicitFetchParams(identifiers, state, hint);

        // Step 4: backoff sleep, then Step 5: the GET.
        scheduler.schedule(sleepMs, () -> transport.fetch(source, userId, finalPath, params, success -> {
            // Step 6: on success reset the failure count (re-load: the response interceptor may have
            // advanced lastVersion/lastReadDate). On failure leave the count (longer next backoff).
            if (success) {
                SyncSourceState latest = stateStore.loadSource(source, userId, deviceId);
                latest.lastFetchUnsuccessfulAttemptCount = 0;
                stateStore.saveState(latest, source, userId, deviceId);
            }
            // Step 7: always release the mutex (token-matched, so a stale release is impossible).
            mutex.unlock(token);
            done(completion, true);
        }));
    }
}
