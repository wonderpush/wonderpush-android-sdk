package com.wonderpush.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Named, non-blocking, cross-thread mutex with TTL-based reclaim.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncMutex}. Serializes explicit fetches per source.
 * {@link #tryLock} returns a non-zero token on success (0 == failed); {@link #unlock} only releases
 * if the token matches the current holder. The TTL lets a crashed/hung holder be reclaimed.
 */
class SyncMutex {

    private static final Map<String, SyncMutex> registry = new HashMap<>();
    private static final Object registryLock = new Object();

    static SyncMutex named(String name) {
        synchronized (registryLock) {
            SyncMutex mutex = registry.get(name);
            if (mutex == null) {
                mutex = new SyncMutex();
                registry.put(name, mutex);
            }
            return mutex;
        }
    }

    private boolean held = false;
    private long token = 0;      // identifies the current acquisition; bumped on each successful tryLock
    private long heldSince = 0;  // ms timestamp of the current acquisition, for TTL reclaim

    /** Returns a non-zero token on success, or 0 if the mutex is held and not expired. */
    synchronized long tryLock(long now, double ttlMs) {
        boolean expired = held && ttlMs > 0 && (double) (now - heldSince) >= ttlMs;
        if (!held || expired) {
            held = true;
            heldSince = now;
            if (++token == 0) token = 1;   // never hand out 0 (the "failed" sentinel) on wrap
            return token;
        }
        return 0;
    }

    /** Releases only if {@code token} matches the current acquisition. Returns whether it did. */
    synchronized boolean unlock(long token) {
        boolean released = held && token != 0 && token == this.token;
        if (released) held = false;
        return released;
    }
}
