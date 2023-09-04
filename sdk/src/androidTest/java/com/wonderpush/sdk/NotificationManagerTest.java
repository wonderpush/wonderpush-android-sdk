package com.wonderpush.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

// Note: to use the Bundle class, we have to run in the emulator
@RunWith(AndroidJUnit4.class)
public class NotificationManagerTest {
    @Test
    public void testNotificationPayloadFromIntent() throws JSONException {
        Bundle bundle;
        JSONObject result;
        Object val;

        // Simple
        bundle = new Bundle();
        bundle.putString("foo", "bar");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        assertEquals("bar", result.get("foo"));

        // Numbers do not get transformed
        bundle = new Bundle();
        bundle.putString("someInt", "123");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        assertEquals("123", result.get("someInt"));

        // nulls do get transformed
        // Note: this isn't really helpful as Firebase seems to filter-out null values from the original bundle
        bundle = new Bundle();
        bundle.putString("someNull", "null");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        assertEquals(JSONObject.NULL, result.get("someNull"));

        // trues do get transformed
        bundle = new Bundle();
        bundle.putString("someTrue", "true");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        assertEquals(Boolean.TRUE, result.get("someTrue"));

        // falses do get transformed
        bundle = new Bundle();
        bundle.putString("someFalse", "false");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        assertEquals(Boolean.FALSE, result.get("someFalse"));

        // Stuff that looks like JSONObjects might get transformed
        // If valid JSON
        bundle = new Bundle();
        bundle.putString("fooJson", "{\"foo\":\"bar\"}");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        val = result.get("fooJson");
        assertTrue(val instanceof JSONObject);
        assertEquals("{\"foo\":\"bar\"}", ((JSONObject)val).toString());

        // If almost valid JSON
        bundle = new Bundle();
        bundle.putString("fooJson", "{\"foo\":bar\"}");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        val = result.get("fooJson");
        assertTrue(val instanceof JSONObject);
        assertEquals("{\"foo\":\"bar\\\"\"}", ((JSONObject) val).toString());

        // If invalid JSON
        bundle = new Bundle();
        bundle.putString("fooJson", "{123}");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        assertEquals("{123}", result.get("fooJson"));

        // Stuff that looks like JSONArrays might get transformed
        // If valid JSON
        bundle = new Bundle();
        bundle.putString("fooJsonArray", "[\"foo\",\"bar\"]");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        val = result.get("fooJsonArray");
        assertTrue(val instanceof JSONArray);
        assertEquals("[\"foo\",\"bar\"]", ((JSONArray)val).toString());

        // If almost valid JSON
        bundle = new Bundle();
        bundle.putString("fooJsonArray", "[\"foo\",bar\"]");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        val = result.get("fooJsonArray");
        assertTrue(val instanceof JSONArray);
        assertEquals("[\"foo\",\"bar\\\"\"]", ((JSONArray)val).toString());

        // If invalid JSON
        bundle = new Bundle();
        bundle.putString("fooJsonArray", "[\"foo\":bar\"]");
        result = NotificationManager.notificationPayloadFromBundle(bundle);
        assertEquals("[\"foo\":bar\"]", result.get("fooJsonArray"));
    }
}
