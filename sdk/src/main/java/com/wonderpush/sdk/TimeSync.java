package com.wonderpush.sdk;

import android.os.SystemClock;

class TimeSync {

    private static long startupDateToServerDateOffset = 0;
    private static long startupDateToServerDateUncertainty = Long.MAX_VALUE;
    private static long deviceDateToServerDateOffset = 0;
    private static long deviceDateToServerDateUncertainty = Long.MAX_VALUE;
    private static long startupDateToDeviceDateOffset = Long.MAX_VALUE;

    /**
     * Get the current timestamp in milliseconds, UTC.
     * @return A timestamp in milliseconds
     */
    protected static long getTime() {
        // Initialization
        if (deviceDateToServerDateUncertainty == Long.MAX_VALUE) {
            deviceDateToServerDateUncertainty = WonderPushConfiguration.getDeviceDateSyncUncertainty();
            deviceDateToServerDateOffset = WonderPushConfiguration.getDeviceDateSyncOffset();
        }
        long currentTimeMillis = System.currentTimeMillis();
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long startupToDeviceOffset = currentTimeMillis - elapsedRealtime;
        if (startupDateToDeviceDateOffset == Long.MAX_VALUE) {
            startupDateToDeviceDateOffset = startupToDeviceOffset;
        }

        // Check device date consistency with startup date
        if (Math.abs(startupToDeviceOffset - startupDateToDeviceDateOffset) > 1000) {
            // System time has jumped (by at least 1 second), or has drifted with regards to elapsedRealtime.
            // Apply the offset difference to resynchronize the "device" sync offset onto the new system date.
            deviceDateToServerDateOffset -= startupToDeviceOffset - startupDateToDeviceDateOffset;
            WonderPushConfiguration.setDeviceDateSyncOffset(deviceDateToServerDateOffset);
            startupDateToDeviceDateOffset = startupToDeviceOffset;
        }

        if (startupDateToServerDateUncertainty <= deviceDateToServerDateUncertainty
                // Don't use the startup date if it has not been synced, use and trust last device date sync
                && startupDateToServerDateUncertainty != Long.MAX_VALUE) {
            return elapsedRealtime + startupDateToServerDateOffset;
        } else {
            return currentTimeMillis + deviceDateToServerDateOffset;
        }
    }

    /**
     * Synchronize time with the WonderPush servers.
     * @param elapsedRealtimeSend
     *            The time at which the request was sent.
     * @param elapsedRealtimeReceive
     *            The time at which the response was received.
     * @param serverDate
     *            The time at which the server received the request, as read in the response.
     * @param serverTook
     *            The time the server took to process the request, as read in the response.
     */
    protected static void syncTimeWithServer(long elapsedRealtimeSend, long elapsedRealtimeReceive, long serverDate, long serverTook) {
        if (serverDate == 0) {
            return;
        }

        // We have two synchronization sources:
        // - The "startup" sync, bound to the process lifecycle, using SystemClock.elapsedRealtime()
        //   This time source cannot be messed up with.
        //   It is only valid until the device reboots, at which time a new time origin is set.
        // - The "device" sync, bound to the system clock, using System.currentTimeMillis()
        //   This time source is affected each time the user changes the date and time,
        //   but it is not affected by timezone or daylight saving changes.
        // The "startup" sync must be saved into a "device" sync in order to persist between runs of the process.
        // The "startup" sync should only be stored in memory, and no attempt to count reboot should be taken.

        // Initialization
        if (deviceDateToServerDateUncertainty == Long.MAX_VALUE) {
            deviceDateToServerDateUncertainty = WonderPushConfiguration.getDeviceDateSyncUncertainty();
            deviceDateToServerDateOffset = WonderPushConfiguration.getDeviceDateSyncOffset();
        }
        long startupToDeviceOffset = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        if (startupDateToDeviceDateOffset == Long.MAX_VALUE) {
            startupDateToDeviceDateOffset = startupToDeviceOffset;
        }

        long uncertainty = (elapsedRealtimeReceive - elapsedRealtimeSend - serverTook) / 2;
        long offset = serverDate + serverTook / 2 - (elapsedRealtimeSend + elapsedRealtimeReceive) / 2;

        // We must improve the quality of the "startup" sync. We can trust elaspedRealtime() based measures.
        if (
            // Case 1. Lower uncertainty
                uncertainty < startupDateToServerDateUncertainty
                        // Case 2. Additional check for exceptional server-side time gaps
                        //         Calculate whether the two offsets agree within the total uncertainty limit
                        || Math.abs(offset - startupDateToServerDateOffset)
                        > uncertainty+startupDateToServerDateUncertainty
            // note the RHS overflows with the Long.MAX_VALUE initialization, but case 1 handles that
                ) {
            // Case 1. Take the new, more accurate synchronization
            // Case 2. Forget the old synchronization, time have changed too much
            startupDateToServerDateOffset = offset;
            startupDateToServerDateUncertainty = uncertainty;
        }

        // We must detect whether the "device" sync is still valid, otherwise we must update it.
        if (
            // Case 1. Lower uncertainty
                startupDateToServerDateUncertainty < deviceDateToServerDateUncertainty
                        // Case 2. Local clock was updated, or the two time sources have drifted from each other
                        || Math.abs(startupToDeviceOffset - startupDateToDeviceDateOffset) > startupDateToServerDateUncertainty
                        // Case 3. Time gap between the "startup" and "device" sync
                        || Math.abs(deviceDateToServerDateOffset - (startupDateToServerDateOffset - startupDateToDeviceDateOffset))
                        > deviceDateToServerDateUncertainty + startupDateToServerDateUncertainty
            // note the RHS overflows with the Long.MAX_VALUE initialization, but case 1 handles that
                ) {
            deviceDateToServerDateOffset = startupDateToServerDateOffset - startupDateToDeviceDateOffset;
            deviceDateToServerDateUncertainty = startupDateToServerDateUncertainty;
            WonderPushConfiguration.setDeviceDateSyncOffset(deviceDateToServerDateOffset);
            WonderPushConfiguration.setDeviceDateSyncUncertainty(deviceDateToServerDateUncertainty);
        }
    }

}
