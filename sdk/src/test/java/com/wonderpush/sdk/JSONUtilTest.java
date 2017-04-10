package com.wonderpush.sdk;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.AssertUtil;

class JSONUtilTest {

    static void assertEquals(JSONObject expected, JSONObject actual) {
        assertEquals(null, expected, actual);
    }

    static void assertEquals(String message, JSONObject expected, JSONObject actual) {
        if (!JSONUtil.equals(expected, actual)) {
            Assert.fail(AssertUtil.format(message, expected, actual));
        }
        Assert.assertTrue(true);
    }

}
