package com.wonderpush.sdk.ratelimiter;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;

public class RateLimiterTest extends InstrumentationTestCase {

    public void setUp() {
        RateLimiter.initialize(getSharedPreferences());
    }

    private SharedPreferences getSharedPreferences() {
        Context context = getInstrumentation().getContext();
        return context.getSharedPreferences("RateLimiterTest", Context.MODE_PRIVATE);
    }

    public void testSimple() throws RateLimiter.MissingSharedPreferencesException, InterruptedException {
        RateLimit limit = new RateLimit("testLimit", 1000, 5);
        RateLimiter limiter = RateLimiter.getInstance();
        limiter.clear(limit);

        limiter.increment(limit); // 4 left
        assertFalse(limiter.isRateLimited(limit));
        waitFor(100); // 0.9s left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 3 left
        assertFalse(limiter.isRateLimited(limit));
        waitFor(100); // 0.8s left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 2 left
        assertFalse(limiter.isRateLimited(limit));
        waitFor(100); // 0.7s left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 1 left
        assertFalse(limiter.isRateLimited(limit));
        waitFor(100); // 0.6s left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 0 left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited!
        waitFor(100); // 0.5s left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited!
        waitFor(100); // 0.4s left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited!
        waitFor(100); // 0.3s left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited!
        waitFor(100); // 0.2s left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited!
        waitFor(100); // 100ms left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited!
        waitFor(100); // 0s left
        assertFalse(limiter.isRateLimited(limit));
    }

    public void testFloatingWindow() throws RateLimiter.MissingSharedPreferencesException, InterruptedException {
        RateLimit limit = new RateLimit("testLimit", 1000, 5);
        RateLimiter limiter = RateLimiter.getInstance();
        limiter.clear(limit);

        limiter.increment(limit); // 4 left
        assertFalse(limiter.isRateLimited(limit));

        waitFor(800);

        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 3 left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 2 left

        waitFor(100);

        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 1 left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 0 left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited

        waitFor(100);

        // 1 left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 0 left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited

        waitFor(700);

        assertTrue(limiter.isRateLimited(limit)); // Rate limited

        waitFor(100);

        // 2 left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 1 left
        assertFalse(limiter.isRateLimited(limit));
        limiter.increment(limit); // 0 left
        assertTrue(limiter.isRateLimited(limit)); // Rate limited
    }

    public void testStorage() throws InterruptedException {
        SharedPreferences prefs = getSharedPreferences();
        RateLimiter limiter1 = new RateLimiter(prefs);
        RateLimit limit = new RateLimit("testLimit", 500, 1);
        limiter1.clear(limit);
        limiter1.increment(limit);
        RateLimiter limiter2 = new RateLimiter(prefs);
        assertTrue(limiter2.isRateLimited(limit));
        waitFor(500);
        assertFalse(limiter2.isRateLimited(limit));
    }

    private void waitFor(long milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
}
