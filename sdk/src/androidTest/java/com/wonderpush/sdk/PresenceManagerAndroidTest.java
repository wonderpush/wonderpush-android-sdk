package com.wonderpush.sdk;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PresenceManagerAndroidTest {

    private List<Error> errors = new ArrayList<>();

    @Before
    public void setUp() {
        synchronized (errors) {
            errors.clear();
        }
    }

    @After
    public void tearDown() {
        synchronized (errors) {
            if (errors.size() > 0) {
                throw errors.get(0);
            }
        }
    }
    private void setTimeout(Runnable runnable, long delay) {

        new Thread(() -> {
            try {
                Thread.sleep(delay);
                try {
                    runnable.run();

                } catch (Error t) {
                    synchronized (errors) {
                        errors.add(t);
                    }
                }
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();
    }

    public static class MockPresenceDelegate implements PresenceManager.PresenceManagerAutoRenewDelegate {
        private PresenceManager.PresencePayload presenceToRenew;
        @Override
        public void autoRenewPresence(PresenceManager presenceManager, PresenceManager.PresencePayload presence) {
            presenceToRenew = presence;
        }
    }

    @Test
    public void testPresencePayloadElapsedTime() {
        Date fromDate = new Date();
        Date toDate = new Date(fromDate.getTime() + 10000);
        PresenceManager.PresencePayload payload = new PresenceManager.PresencePayload(fromDate, toDate);
        assertEquals(payload.getElapsedTime(), 10000);

        toDate = new Date(fromDate.getTime() - 10000);
        payload = new PresenceManager.PresencePayload(fromDate, toDate);;
        assertEquals(payload.getElapsedTime(), -10000);
    }
    @Test
    public void testPresencePayloadSerialization() throws JSONException {
        Date fromDate = new Date();
        Date untilDate = new Date(fromDate.getTime() + 10000);
        PresenceManager.PresencePayload payload = new PresenceManager.PresencePayload(fromDate, untilDate);

        JSONObject serialized = payload.toJSONObject();

        // Date s should be epoch milliseconds
        assertEquals(fromDate.getTime(), serialized.optLong("fromDate"));
        assertEquals(untilDate.getTime(), serialized.optLong("untilDate"));

        // Elapsed time in milliseconds
        assertEquals(10000, serialized.optLong("elapsedTime"));
    }

    @Test
    public void testPresenceDidStart() throws InterruptedException {
        Date now = new Date();
        PresenceManager manager = new PresenceManager(null, 10000, 0);
        PresenceManager.PresencePayload payload = manager.presenceDidStart();
        assertEquals(10000, payload.getElapsedTime());
        assertEquals(now.getTime(), payload.getFromDate().getTime(), 100);
    }

    @Test
    public void testPresenceDidStopNoStart() {
        PresenceManager manager = new PresenceManager(null,10000, 0);
        Date now = new Date();
        PresenceManager.PresencePayload payload = manager.presenceWillStop();
        assertEquals(0, payload.getElapsedTime());
        assertEquals(payload.getFromDate().getTime(), payload.getUntilDate().getTime());
        assertEquals(now.getTime(), payload.getFromDate().getTime(), 100);
    }

    @Test
    public void testPresenceDidStop() throws ExecutionException, InterruptedException {
        PresenceManager manager = new PresenceManager(null,10, 0);

        // Note: there shouldn't be a need to call presenceDidStart
        Date startDate = new Date();
        manager.presenceDidStart();

        CompletableFuture<Void> future = new CompletableFuture<>();
        // 100 ms later...
        setTimeout(() -> {
            Date stopDate = new Date ();
            PresenceManager.PresencePayload stopPayload = manager.presenceWillStop();

            assertEquals(startDate.getTime(), stopPayload.getFromDate().getTime(), 10);
            assertEquals(stopDate.getTime(), stopPayload.getUntilDate().getTime(), 10);
            future.complete(null);

        }, 100);
        try {
            future.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAutoRenew() throws ExecutionException, InterruptedException {
        MockPresenceDelegate delegate = new MockPresenceDelegate();
        PresenceManager manager = new PresenceManager(delegate,500, 100);

        Date startDate = new Date();
        manager.presenceDidStart();
        assertNull(delegate.presenceToRenew);

        CompletableFuture<Void> future = new CompletableFuture<>();
        setTimeout(() -> {
            // no auto-renew should have been attempted
            assertNull(delegate.presenceToRenew);

        }, 350);

        // Wait 410ms (auto-renew attempted at 0.5 - 0.1 = 400ms.
        setTimeout(() -> {
            // Auto-renew should have been attempted
            PresenceManager.PresencePayload autoRenewPayload = delegate.presenceToRenew;
            assertNotNull(autoRenewPayload);
            assertEquals(startDate.getTime(), autoRenewPayload.getFromDate().getTime(), 10);
            Date expectedUntilDate = new Date(startDate.getTime() + 400 + 500);
            assertEquals(expectedUntilDate.getTime(), autoRenewPayload.getUntilDate().getTime(), 10);

            // nil-out the presenceToRenew
            delegate.presenceToRenew = null;

        }, 410);

        // Wait 600ms and stop
        setTimeout(() -> {
            PresenceManager.PresencePayload stopPayload = manager.presenceWillStop();
            Date now = new Date();

            // Check startDate and untilDate
            assertEquals(startDate.getTime(), stopPayload.getFromDate().getTime(), 10);
            assertEquals(now.getTime(), stopPayload.getUntilDate().getTime(), 10);

            // No auto-renew should have been attempted
            assertNull(delegate.presenceToRenew);

        }, 600);

        // Wait 950ms and check we're stopped still
        setTimeout(() -> {
            // No auto-renew should have been attempted
            assertNull(delegate.presenceToRenew);
            future.complete(null);
        }, 950);

        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultiplePresenceDidStart() throws ExecutionException, InterruptedException {
        MockPresenceDelegate delegate = new MockPresenceDelegate();
        PresenceManager manager = new PresenceManager(delegate,300, 100);

        CompletableFuture<Void> future = new CompletableFuture<>();

        final AtomicReference<Date> lastStartDate = new AtomicReference<>();
        for (int i = 0; i < 5; i++) {
            setTimeout(() -> {
                assertNull(delegate.presenceToRenew);
                try {
                    manager.presenceDidStart();
                } catch (InterruptedException e) {
                    fail();
                }
                lastStartDate.set(new Date());

            }, 100);
        }
        // Wait 500 + (300 - 100 + 10) ms and check the timer fired
        setTimeout(() -> {
            assertNotNull(delegate.presenceToRenew);
            PresenceManager.PresencePayload stopPayload = manager.presenceWillStop();
            assertEquals((lastStartDate.get()).getTime(), stopPayload.getFromDate().getTime(), 10);
            future.complete(null);
        }, 710);

        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
