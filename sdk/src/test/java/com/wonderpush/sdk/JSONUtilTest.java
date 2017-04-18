package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.AssertUtil;
import org.junit.Test;

public class JSONUtilTest {

    static void assertEquals(JSONObject expected, JSONObject actual) {
        assertEquals(null, expected, actual);
    }

    static void assertEquals(String message, JSONObject expected, JSONObject actual) {
        if (!JSONUtil.equals(expected, actual)) {
            Assert.fail(AssertUtil.format(message, expected, actual));
        }
        Assert.assertTrue(true);
    }

    private void assertMerge(JSONObject expected, JSONObject base, JSONObject diff, boolean nullFieldRemoves) throws JSONException {
        JSONObject obj = JSONUtil.deepCopy(base);
        JSONUtil.merge(obj, diff, nullFieldRemoves);
        assertEquals(expected, obj);
    }

    @Test
    public void testMerge() throws JSONException {
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"existing_field\":2}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"existing_field\":1}"),
                new JSONObject("{\"existing_field\":2}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"existing_field\":[1,2]}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"existing_field\":[1]}"),
                new JSONObject("{\"existing_field\":[1,2]}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"existing_field\":{\"foo\":\"bar\",\"bar\":\"baz\"}}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"existing_field\":{\"foo\":\"bar\"}}"),
                new JSONObject("{\"existing_field\":{\"bar\":\"baz\"}}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"new_field\":2}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"new_field\":2}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"new_field\":[1,2]}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"new_field\":[1,2]}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"new_field\":{\"foo\":\"bar\"}}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"new_field\":{\"foo\":\"bar\"}}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"removed_field\":2}"),
                new JSONObject("{\"removed_field\":null}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"removed_field\":null}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"removed_field\":2}"),
                new JSONObject("{\"removed_field\":null}"),
                false
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"removed_nonexistent_field\":null}"),
                true
        );
        assertMerge(
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1,\"removed_nonexistent_field\":null}"),
                new JSONObject("{\"unchanged_null\":null,\"unchanged\":1}"),
                new JSONObject("{\"removed_nonexistent_field\":null}"),
                false
        );
    }

}
