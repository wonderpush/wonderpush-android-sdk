package com.wonderpush.sdk;

import static org.junit.Assert.assertEquals;

import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WonderPushConfigurationRememberTrackedEventsTest {

   @BeforeClass
   public static void initStorage() {
      WonderPushConfiguration.initialize(ApplicationProvider.getApplicationContext());
   }

   @BeforeClass
   public static void initTime() {
      TimeSync.syncTimeWithServer(SystemClock.elapsedRealtime(), SystemClock.elapsedRealtime(), 1000000000000L, 0);
   }

   @AfterClass
   public static void resetTime() {
      TimeSync.syncTimeWithServer(SystemClock.elapsedRealtime(), SystemClock.elapsedRealtime(), System.currentTimeMillis(), 0);
   }

   @AfterClass
   public static void resetStorage() {
      WonderPushConfiguration.clearStorage(false, false);
   }

   @Before
   public void clearStorage() {
      WonderPushConfiguration.clearStorage(false, false);
   }

   @After
   public void restoreDefaultMaximums() {
      WonderPushConfiguration.setMaximumCollapsedLastBuiltinTrackedEventsCount(WonderPushConfiguration.DEFAULT_MAXIMUM_COLLAPSED_LAST_BUILTIN_TRACKED_EVENTS_COUNT);
      WonderPushConfiguration.setMaximumCollapsedLastCustomTrackedEventsCount(WonderPushConfiguration.DEFAULT_MAXIMUM_COLLAPSED_LAST_CUSTOM_TRACKED_EVENTS_COUNT);
      WonderPushConfiguration.setMaximumCollapsedOtherTrackedEventsCount(WonderPushConfiguration.DEFAULT_MAXIMUM_COLLAPSED_OTHER_TRACKED_EVENTS_COUNT);
      WonderPushConfiguration.setMaximumUncollapsedTrackedEventsCount(WonderPushConfiguration.DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_COUNT);
      WonderPushConfiguration.setMaximumUncollapsedTrackedEventsAgeMs(WonderPushConfiguration.DEFAULT_MAXIMUM_UNCOLLAPSED_TRACKED_EVENTS_AGE_MS);
   }

   @Test
   public void testAddEventAddsCollapsingLast() throws JSONException {
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}"));
      assertEquals(2, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}"));
      assertEquals(3, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}"));
      assertEquals(5, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test2\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"test2\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
   }

   @Test
   public void testAddEventWithCollapsingLast() throws JSONException {
      // Note: Adding events with collapsing=last is not customary, but it we're testing the implementation here.

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"last\"}"));
      assertEquals(1, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"last\"}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}"));
      assertEquals(2, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}"));
      assertEquals(2, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}");
   }

   @Test
   public void testAddEventWithCollapsingCampaign() throws JSONException {
      // "campaign" collapsing are treated as already collapsed (so we don't add a collapsing=last event)
      // We deduplicate them based on their campaignId

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"campaign\"}"));
      assertEquals(1, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"campaign\"}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"campaignId\":\"c2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"campaign\"}"));
      assertEquals(2, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test\",\"campaignId\":\"c2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"campaign\"}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}"));
      assertEquals(2, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"campaignId\":\"c2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}");
   }

   @Test
   public void testAddEventWithCollapsingUnhandled() throws JSONException {
      // Unhandled collapsing are treated as already collapsed (so we don't add a collapsing=last event)
      // They accumulate like uncollapsed events do, there's no known deduplication to apply

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"unhandled~collapsing\"}"));
      assertEquals(1, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"unhandled~collapsing\"}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"unhandled~collapsing\"}"));
      assertEquals(2, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"unhandled~collapsing\"}");

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"unhandled~collapsing\"}"));
      assertEquals(3, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"unhandled~collapsing\"}");
   }

   @Test
   public void testNoCollapsingInterference() throws JSONException {
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010000000,\"creationDate\":1000010000000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010001000,\"creationDate\":1000010001000,\"collapsing\":\"last\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010002000,\"creationDate\":1000010002000,\"collapsing\":\"campaign\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010003000,\"creationDate\":1000010003000,\"collapsing\":\"unhandled~collapsing\"}"));
      assertEquals(8, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010001000,\"creationDate\":1000010001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010002000,\"creationDate\":1000010002000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010003000,\"creationDate\":1000010003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010000000,\"creationDate\":1000010000000}");

      // Adding a collapsing=last event should only affect the same collapsing=last and identical type
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010004000,\"creationDate\":1000010004000,\"collapsing\":\"last\"}"));
      assertEquals(8, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010004000,\"creationDate\":1000010004000,\"collapsing\":\"last\"}"); // <- Only thing changed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010002000,\"creationDate\":1000010002000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010003000,\"creationDate\":1000010003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010000000,\"creationDate\":1000010000000}");

      // Adding a collapsing=campaign event should only affect the same collapsing=campaign and identical campaignId
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010005000,\"creationDate\":1000010005000,\"collapsing\":\"campaign\"}"));
      assertEquals(8, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010004000,\"creationDate\":1000010004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010003000,\"creationDate\":1000010003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010005000,\"creationDate\":1000010005000,\"collapsing\":\"campaign\"}"); // <- Only thing changed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010000000,\"creationDate\":1000010000000}");

      // Adding an unhandled collapsing event should merely add a new event
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010006000,\"creationDate\":1000010006000,\"collapsing\":\"unhandled~collapsing\"}"));
      assertEquals(9, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010004000,\"creationDate\":1000010004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010003000,\"creationDate\":1000010003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010005000,\"creationDate\":1000010005000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010006000,\"creationDate\":1000010006000,\"collapsing\":\"unhandled~collapsing\"}"); // <- Only thing added
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010000000,\"creationDate\":1000010000000}");

      // Adding an uncollapsed event should add a new event and only affect the collapsing=last and same type
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010007000,\"creationDate\":1000010007000}"));
      assertEquals(10, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010007000,\"creationDate\":1000010007000,\"collapsing\":\"last\"}"); // <- Thing changed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010003000,\"creationDate\":1000010003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010005000,\"creationDate\":1000010005000,\"collapsing\":\"campaign\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010006000,\"creationDate\":1000010006000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"test1\",\"campaignId\":\"c1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010000000,\"creationDate\":1000010000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"test2\",\"campaignId\":\"c2\",\"actionDate\":1000010007000,\"creationDate\":1000010007000}"); // <- Thing added
   }

   @Test
   public void testUncollapsedEventsSupernumeraryPruning() throws JSONException {
      WonderPushConfiguration.setMaximumUncollapsedTrackedEventsCount(5);

      // Add one too many uncollapsed event
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}"));

      // Ensure the oldest uncollapsed event is removed
      assertEquals(6, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");

      // One more uncollapsed event of another type should consume one place too
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}"));
      assertEquals(7, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}");

      // One more collapsed event should not, however
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"unhandled~collapsing\"}"));
      assertEquals(8, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}");
   }

   @Test
   public void testCollapsingLastCustomEventsSupernumeraryPruning() throws JSONException {
      WonderPushConfiguration.setMaximumCollapsedLastCustomTrackedEventsCount(5);

      // Add one too many uncollapsed event with different type
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}"));
      // Ensure the oldest collapsing=last event is removed to respect the max
      assertEquals(11, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}"); // uncollapsed-test1 was removed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(10).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");

      // One more uncollapsed event of a previous type should bump an existing collapsing=last event
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}"));
      assertEquals(12, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"last\"}"); // <- Only this one is bumped
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(10).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(11).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}"); // <- and uncollapsed event is tracked as we are below the corresponding maximum

      // Adding directly one collapsing=last should also remove the oldest collapsing=last to respect the max
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test7\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"last\"}"));
      assertEquals(12, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}"); // uncollapsed-test3 was removed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test7\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"last\"}"); // <- this one took its place
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(10).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(11).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}");
   }

   @Test
   public void testCollapsingLastBuiltinEventsSupernumeraryPruning() throws JSONException {
      WonderPushConfiguration.setMaximumCollapsedLastBuiltinTrackedEventsCount(5);

      // Add one too many uncollapsed event with different type
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}"));
      // Ensure the oldest collapsing=last event is removed to respect the max
      assertEquals(11, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}"); // @BUILTIN1 was removed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"@BUILTIN3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"@BUILTIN1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"@BUILTIN3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(10).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");

      // One more uncollapsed event of a previous type should bump an existing collapsing=last event
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}"));
      assertEquals(12, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"@BUILTIN3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"last\"}"); // <- Only this one is bumped
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"@BUILTIN1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"@BUILTIN3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(10).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(11).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}"); // <- and uncollapsed event is tracked as we are below the corresponding maximum

      // Adding directly one collapsing=last should also remove the oldest collapsing=last to respect the max
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN7\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"last\"}"));
      assertEquals(12, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}"); // @BUILTIN3 was removed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"@BUILTIN7\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"last\"}"); // <- this one took its place
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"@BUILTIN1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"@BUILTIN3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(10).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(11).toString(), "{\"type\":\"@BUILTIN2\",\"actionDate\":1000000006000,\"creationDate\":1000000006000}");
   }

   @Test
   public void testCollapsingOtherEventsSupernumeraryPruning() throws JSONException {
      WonderPushConfiguration.setMaximumCollapsedOtherTrackedEventsCount(5);

      // Add one too many unhandled-collapsed event with different type
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"unhandled~collapsing\"}"));
      // Ensure the oldest collapsed event is removed to respect the max
      assertEquals(5, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"unhandled~collapsing\"}"); // uncollapsed-test1 was removed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"unhandled~collapsing\"}");

      // One more collapsing=campaign event, even of a previous type, should remove the oldest one
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"campaign\",\"campaignId\":\"c1\"}"));
      assertEquals(5, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"unhandled~collapsing\"}"); // uncollapsed-test2 was removed
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000006000,\"creationDate\":1000000006000,\"collapsing\":\"campaign\",\"campaignId\":\"c1\"}");

      // One more collapsing=campaign event should replace its previous occurence, not removing any other event
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"campaign\",\"campaignId\":\"c1\"}"));
      assertEquals(5, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000007000,\"creationDate\":1000000007000,\"collapsing\":\"campaign\",\"campaignId\":\"c1\"}"); // <- this one was updated
   }

   @Test
   public void testIndependentEventsSupernumeraryPruning() throws JSONException {
      WonderPushConfiguration.setMaximumUncollapsedTrackedEventsCount(2);
      WonderPushConfiguration.setMaximumCollapsedLastBuiltinTrackedEventsCount(3);
      WonderPushConfiguration.setMaximumCollapsedLastCustomTrackedEventsCount(4);
      WonderPushConfiguration.setMaximumCollapsedOtherTrackedEventsCount(5);

      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN1\",\"actionDate\":1000000000010,\"creationDate\":1000000000010}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN2\",\"actionDate\":1000000001010,\"creationDate\":1000000001010}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN3\",\"actionDate\":1000000002010,\"creationDate\":1000000002010}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003010,\"creationDate\":1000000003010}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004010,\"creationDate\":1000000004010}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005010,\"creationDate\":1000000005010}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"unhandled~collapsing1\",\"actionDate\":1000000000020,\"creationDate\":1000000000020,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"unhandled~collapsing2\",\"actionDate\":1000000001020,\"creationDate\":1000000001020,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"unhandled~collapsing3\",\"actionDate\":1000000002020,\"creationDate\":1000000002020,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"unhandled~collapsing4\",\"actionDate\":1000000003020,\"creationDate\":1000000003020,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"unhandled~collapsing5\",\"actionDate\":1000000004020,\"creationDate\":1000000004020,\"collapsing\":\"unhandled~collapsing\"}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"unhandled~collapsing6\",\"actionDate\":1000000005020,\"creationDate\":1000000005020,\"collapsing\":\"unhandled~collapsing\"}"));

      for (JSONObject e : WonderPushConfiguration.getTrackedEvents()) {
         Log.i("XXXXXX", e.toString());
      }
      assertEquals(2+3+4+5, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"@BUILTIN4\",\"actionDate\":1000000003010,\"creationDate\":1000000003010,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"@BUILTIN5\",\"actionDate\":1000000004010,\"creationDate\":1000000004010,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005010,\"creationDate\":1000000005010,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":1000000003000,\"creationDate\":1000000003000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test5\",\"actionDate\":1000000004000,\"creationDate\":1000000004000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(6).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(7).toString(), "{\"type\":\"unhandled~collapsing2\",\"actionDate\":1000000001020,\"creationDate\":1000000001020,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(8).toString(), "{\"type\":\"unhandled~collapsing3\",\"actionDate\":1000000002020,\"creationDate\":1000000002020,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(9).toString(), "{\"type\":\"unhandled~collapsing4\",\"actionDate\":1000000003020,\"creationDate\":1000000003020,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(10).toString(), "{\"type\":\"unhandled~collapsing5\",\"actionDate\":1000000004020,\"creationDate\":1000000004020,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(11).toString(), "{\"type\":\"unhandled~collapsing6\",\"actionDate\":1000000005020,\"creationDate\":1000000005020,\"collapsing\":\"unhandled~collapsing\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(12).toString(), "{\"type\":\"uncollapsed-test6\",\"actionDate\":1000000005000,\"creationDate\":1000000005000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(13).toString(), "{\"type\":\"@BUILTIN6\",\"actionDate\":1000000005010,\"creationDate\":1000000005010}");
   }

   @Test
   public void testUncollapsedEventsAgePruning() throws JSONException {
      WonderPushConfiguration.setMaximumUncollapsedTrackedEventsAgeMs(1000000000000L);

      // Add one too many uncollapsed event with different type
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}"));
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}"));
      assertEquals(6, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");

      // Advance time
      // NOTE: Time still flows, so if tests are slow (here more than 500ms), the results will be broken
      TimeSync.syncTimeWithServer(SystemClock.elapsedRealtime(), SystemClock.elapsedRealtime(), 2000000001500L, 0);

      // Implementation detail: The getter does not apply pruning, so the list is unchanged
      assertEquals(6, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");

      // Add one more event
      WonderPushConfiguration.rememberTrackedEvent(new JSONObject("{\"type\":\"uncollapsed-test4\",\"actionDate\":2000000000000,\"creationDate\":2000000000000}")); // yes, this event is 1500ms in the past, it's fine
      // Pruning should have been applied
      assertEquals(6, WonderPushConfiguration.getTrackedEvents().size());
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(0).toString(), "{\"type\":\"uncollapsed-test1\",\"actionDate\":1000000000000,\"creationDate\":1000000000000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(1).toString(), "{\"type\":\"uncollapsed-test2\",\"actionDate\":1000000001000,\"creationDate\":1000000001000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(2).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(3).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":2000000000000,\"creationDate\":2000000000000,\"collapsing\":\"last\"}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(4).toString(), "{\"type\":\"uncollapsed-test3\",\"actionDate\":1000000002000,\"creationDate\":1000000002000}");
      assertEquals(WonderPushConfiguration.getTrackedEvents().get(5).toString(), "{\"type\":\"uncollapsed-test4\",\"actionDate\":2000000000000,\"creationDate\":2000000000000}");
   }

}
