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

    @Test
    public void assertStripNullsOnNull() throws JSONException {
        JSONUtil.stripNulls(null); // must not throw
    }

    @Test
    public void assertStripNullsConcurrentModificationException() throws JSONException {
        // Test for ConcurrentModificationException when reading key `c` after removing key `b`
        JSONObject obj = new JSONObject("{\"a\":1,\"b\":null,\"c\":3}");
        JSONUtil.stripNulls(obj);
        assertEquals(new JSONObject("{\"a\":1,\"c\":3}"), obj);
    }

    @Test
    public void assertStripNullsArraysUntouched() throws JSONException {
        JSONObject obj  = new JSONObject("{\"a\":[1,null,3,{\"a\":null}]}");
        JSONUtil.stripNulls(obj);
        assertEquals(new JSONObject("{\"a\":[1,null,3,{\"a\":null}]}"), obj);
    }

    @Test
    public void assertStripNullsSubObjects() throws JSONException {
        JSONObject obj = new JSONObject("{\"parent\":{\"a\":1,\"b\":null,\"c\":3}}");
        JSONUtil.stripNulls(obj);
        assertEquals(new JSONObject("{\"parent\":{\"a\":1,\"c\":3}}"), obj);
    }

    @Test
    public void testSimpleDiff() throws JSONException {

        assertEquals(new JSONObject("{\"a\": null, \"b\":2}"),
                JSONUtil.diff(new JSONObject("{\"a\":1}"), new JSONObject("{\"b\":2}")));

        assertEquals(new JSONObject("{\"a\": 2}"),
                JSONUtil.diff(new JSONObject("{\"a\":1}"), new JSONObject("{\"a\":2}")));

    }

    @Test
    public void testNoDiff() throws JSONException {

        assertEquals(new JSONObject("{}"),
                JSONUtil.diff(new JSONObject("{}"), new JSONObject("{}")));

        assertEquals(new JSONObject("{}"),
                JSONUtil.diff(new JSONObject("{\"a\":1}"), new JSONObject("{\"a\":1}")));

        assertEquals(new JSONObject("{}"),
                JSONUtil.diff(new JSONObject("{\"a\":[1]}"), new JSONObject("{\"a\":[1]}")));

        assertEquals(new JSONObject("{}"),
                JSONUtil.diff(new JSONObject("{\"a\":{\"b\":1}}"), new JSONObject("{\"a\":{\"b\":1}}")));
    }
    @Test
    public void testArrayDiff() throws JSONException {

        assertEquals(new JSONObject("{\"a\": [4,5,6]}"),
                JSONUtil.diff(new JSONObject("{\"a\":[1,2,3]}"), new JSONObject("{\"a\":[4,5,6]}")));

    }

    @Test
    public void testObjectDiff() throws JSONException {

        assertEquals(new JSONObject("{\"a\": {\"aa\":2}}"),
                JSONUtil.diff(new JSONObject("{\"a\": {\"aa\":1}}"), new JSONObject("{\"a\": {\"aa\":2}}")));

        assertEquals(new JSONObject("{\"a\": {\"aa\":null, \"bb\":2}}"),
                JSONUtil.diff(new JSONObject("{\"a\": {\"aa\":1}}"), new JSONObject("{\"a\": {\"bb\":2}}")));
    }
    @Test
    public void testMixedDiff() throws JSONException {

        assertEquals(new JSONObject("{\"a\": \"foo\"}"),
                JSONUtil.diff(new JSONObject("{\"a\": {\"aa\":1}}"), new JSONObject("{\"a\": \"foo\"}")));

    }

    @Test
    public void testNullDiff() throws JSONException {

        assertEquals(new JSONObject("{\"a\": null}"),
                JSONUtil.diff(new JSONObject("{\"a\": {\"aa\":1}}"), new JSONObject("{\"a\": null}")));

        assertEquals(new JSONObject("{\"a\": null}"),
                JSONUtil.diff(new JSONObject("{\"a\": {\"aa\":1}}"), new JSONObject("{}")));
    }

}
