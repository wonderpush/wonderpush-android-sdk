package com.wonderpush.sdk;

/** Injectable millisecond clock, so the sync loop's timing is unit-testable without the real clock. */
interface SyncClock {
    long now();
}
