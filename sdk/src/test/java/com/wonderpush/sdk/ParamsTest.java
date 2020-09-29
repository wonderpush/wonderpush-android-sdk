package com.wonderpush.sdk;

import org.junit.Test;
import static org.junit.Assert.*;

public class ParamsTest {

    @Test
    public void testRequestParams() {
        ApiClient.Params params = new ApiClient.Params();

        // Does putting twice overrides previous value?
        params.put("foo", "bar");
        params.put("foo", "baz");
        assertTrue(params.has("foo"));

        assertEquals("foo=baz", params.getURLEncodedString());
        params.remove("foo");
        assertEquals("", params.getURLEncodedString());

        // Does adding twice overrides previous values?
        params = new ApiClient.Params();
        params.add("b", "a");
        params.add("b", "b");
        params.add("a", "a");
        assertEquals("a=a&b=a&b=b", params.getURLEncodedString());
        assertEquals(3, params.getParamsList().size());
        assertEquals("a", params.getParamsList().get(0).getName());
        assertEquals("a", params.getParamsList().get(0).getValue());
        assertEquals("b", params.getParamsList().get(2).getName());
        assertEquals("b", params.getParamsList().get(2).getValue());
        assertTrue(params.has("a"));
        assertTrue(params.has("b"));
        params.remove("b");
        params.remove("a");
        assertEquals("", params.getURLEncodedString());
        params.add("b", "b");
        params.add("b", "a");
        params.add("a", "a");
        params.put("b", "c");
        params.put("a", "c");
        assertEquals("a=c&b=c&a=a&b=a&b=b", params.getURLEncodedString());
        params.remove("b");
        assertEquals("a=c&a=a", params.getURLEncodedString());


        // URL encoding
        params = new ApiClient.Params();
        params.put("foo bar", "baz toto");
        assertEquals("foo+bar=baz+toto", params.getURLEncodedString());
        assertEquals("foo bar", params.getParamsList().get(0).getName());
        assertEquals("baz toto", params.getParamsList().get(0).getValue());

        // Ordering
        params = new ApiClient.Params();
        params.put("b", "b");
        params.put("c", "c");
        params.put("a", "a");
        assertEquals("a=a&b=b&c=c", params.getURLEncodedString());
        assertEquals("a", params.getParamsList().get(0).getName());
        params.remove("b");
        assertEquals("a=a&c=c", params.getURLEncodedString());
        assertEquals("a", params.getParamsList().get(0).getName());

        // Null value
        params = new ApiClient.Params();
        params.put("a", "a");
        params.put("a", (String)null); // ignored
        assertEquals("a=a", params.getURLEncodedString());
        assertEquals(1, params.getParamsList().size());

        // Null key
        params = new ApiClient.Params();
        params.put(null, "a");
        assertEquals("", params.getURLEncodedString());
        assertEquals(0, params.getParamsList().size());

        // Single value constructor
        params = new ApiClient.Params("b", "b");
        params.put("b", "a");
        assertEquals("b=a", params.getURLEncodedString());

    }
}
