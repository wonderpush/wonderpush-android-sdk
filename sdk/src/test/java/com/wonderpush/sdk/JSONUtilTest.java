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

        assertEquals(new JSONObject("{\"a\": \"foo\"}"),
                JSONUtil.diff(new JSONObject("{\"a\": 1}"), new JSONObject("{\"a\": \"foo\"}")));

        assertEquals(new JSONObject("{\"a\": 1}"),
                JSONUtil.diff(new JSONObject("{\"a\": \"foo\"}"), new JSONObject("{\"a\": 1}")));

    }

    @Test
    public void testNullDiff() throws JSONException {

        assertEquals(new JSONObject("{\"a\": null}"),
                JSONUtil.diff(new JSONObject("{\"a\": {\"aa\":1}}"), new JSONObject("{\"a\": null}")));

        assertEquals(new JSONObject("{\"a\": null}"),
                JSONUtil.diff(new JSONObject("{\"a\": {\"aa\":1}}"), new JSONObject("{}")));
    }

    @Test
    public void testNumberClasses() throws JSONException {
        JSONObject a, b;

        // A few manual tests first

        a = new JSONObject();
        a.put("a", new Integer(3600));
        b = new JSONObject();
        b.put("a", new Long(3600L));
        assertEquals(new JSONObject(),
                JSONUtil.diff(a ,b));

        a = new JSONObject();
        a.put("a", 3600);
        b = new JSONObject();
        b.put("a", 3600L);
        assertEquals(new JSONObject(),
                JSONUtil.diff(a ,b));

        a = new JSONObject();
        a.put("a", 3600);
        b = new JSONObject();
        b.put("a", new Float(3600));
        assertEquals(new JSONObject(),
                JSONUtil.diff(a ,b));

        a = new JSONObject();
        a.put("a", 3600);
        b = new JSONObject();
        b.put("a", 3600D);
        assertEquals(new JSONObject(),
                JSONUtil.diff(a ,b));

        a = new JSONObject();
        a.put("a", new Float(3600));
        b = new JSONObject();
        b.put("a", 3600D);
        assertEquals(new JSONObject(),
                JSONUtil.diff(a ,b));

        // Exhaustive tests next
        // We compare a few differently-sized numbers across underlying types

        byte oneByte = 1;
        short oneShort = 1;
        int oneInt = 1;
        long oneLong = 1;
        float oneFloat = 1;
        double oneDouble = 1;
        // Test without boxing and with explicit boxing to compare against small interned numerics too
        Object[] ones = new Object[] {
                oneByte,
                new Byte(oneByte),
                oneShort,
                new Short(oneShort),
                oneInt,
                new Integer(oneInt),
                oneLong,
                new Long(oneLong),
                oneFloat,
                new Float(oneFloat),
                oneDouble,
                new Double(oneDouble),
        };

        // Compare bigger, non interned numbers
        short biggerShort = 32000;
        int biggerInt = 32000;
        long biggerLong = 32000;
        float biggerFloat = 32000;
        double biggerDouble = 32000;
        Object[] bigger = new Object[] {
                biggerShort,
                new Short(biggerShort),
                biggerInt,
                new Integer(biggerInt),
                biggerLong,
                new Long(biggerLong),
                biggerFloat,
                new Float(biggerFloat),
                biggerDouble,
                new Double(biggerDouble),
        };

        // Compare numbers that cannot be represented as non floating points
        float biggererFloat = Float.MAX_VALUE;
        double biggererDouble = Float.MAX_VALUE;
        Object[] biggerer = new Object[] {
                biggererFloat,
                new Float(biggererFloat),
                biggererDouble,
                new Double(biggererDouble),
        };

        Object[][] suites = new Object[][] {
                ones, bigger, biggerer,
        };

        // For each suite…
        for (Object[] suiteA : suites) {
            for (Object[] suiteB : suites) {
                // …compare each couple of values
                for (Object valueA : suiteA) {
                    for (Object valueB : suiteB) {
                        a = new JSONObject();
                        a.put("a", valueA);
                        b = new JSONObject();
                        b.put("a", valueB);
                        assertEquals(
                                suiteA == suiteB
                                        // If values are taken from the same suite, the diff should be empty
                                        ? new JSONObject()
                                        // If values are taken from different suites, the diff should not be empty
                                        : b,
                                JSONUtil.diff(a, b)
                        );
                    }
                }
            }
        }
    }

}
