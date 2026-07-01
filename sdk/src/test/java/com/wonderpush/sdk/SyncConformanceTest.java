package com.wonderpush.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Cross-SDK conformance harness for the sdk-sync pure logic.
 *
 * Modeled on wonderpush-ios-sdk {@code WPSyncConformanceTests.m}. Loads the vendored
 * {@code sync-conformance/*.vectors.json} fixtures (see {@code PIN.txt}) and asserts every case
 * matches the reference SDK exactly. A failure here means Android drifted from JS/iOS.
 *
 * The "Infinity"/"-Infinity"/"NaN" JSON string sentinels are mapped back to doubles inside
 * {@link SyncKnobs#fromJSON} (org.json cannot hold non-finite doubles), so no global pre-pass is
 * needed. JSON {@code null} is the null-missing sentinel.
 *
 * NOTE: contact-store.vectors.json is exercised by the contact-source tests (issue p0b), not here.
 */
public class SyncConformanceTest {

    private static JSONObject loadVectors(String filename) throws IOException, JSONException {
        String path = "sync-conformance/" + filename;
        try (InputStream in = SyncConformanceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertTrue("vendored conformance file not found on the test classpath: " + path, in != null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return new JSONObject(out.toString("UTF-8"));
        }
    }

    private static JSONArray casesOf(String filename) throws IOException, JSONException {
        return loadVectors(filename).getJSONArray("cases");
    }

    /** JSON null / absent -> Java null; otherwise pass through. */
    private static Object orNull(Object v) {
        return (v == null || v == JSONObject.NULL) ? null : v;
    }

    @Test
    public void testCompareVersionId() throws Exception {
        JSONArray cases = casesOf("compare-version-id.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            int r = SyncVersionId.compareVersionId(in.opt("a"), in.opt("b"));
            assertEquals(c.getString("name"), c.getInt("expected"), r);
        }
    }

    @Test
    public void testAcceptsResponse() throws Exception {
        JSONArray cases = casesOf("accepts-response.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject resp = c.getJSONObject("input").getJSONObject("response");
            JSONObject st = c.getJSONObject("input").getJSONObject("state");
            boolean r = SyncVersionId.acceptsResponse(
                    resp.optLong("version"), resp.opt("versionId"), resp.optLong("readDate"),
                    resp.has("data") ? resp.opt("data") : null,
                    st.optLong("lastVersion"), st.opt("lastVersionId"), st.optLong("lastReadDate"));
            assertEquals(c.getString("name"), c.getBoolean("expected"), r);
        }
    }

    @Test
    public void testClassifyResponse() throws Exception {
        JSONArray cases = casesOf("classify-response.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            SyncProcessor.Classification cl = SyncProcessor.classifyResponse(
                    in.optString("path", null), in.optString("method", null));
            assertDeepEquals(c.getString("name"), c.getJSONObject("expected"), cl.toJSON());
        }
    }

    @Test
    public void testComputeBackoffSleep() throws Exception {
        JSONArray cases = casesOf("compute-backoff-sleep.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            SyncKnobs knobs = SyncKnobs.fromJSON(in.getJSONObject("knobs"));
            double r = SyncFetchPolicy.computeBackoffSleep(in.getInt("attemptCount"), in.getDouble("rand"), knobs);
            double expected = c.getDouble("expected");
            assertEquals(c.getString("name"), expected, r, Math.abs(expected) * 1e-6 + 1e-9);
        }
    }

    @Test
    public void testShouldDebounceWeakSignal() throws Exception {
        JSONArray cases = casesOf("should-debounce-weak-signal.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            boolean r = SyncFetchPolicy.shouldDebounceWeakSignal(
                    in.getLong("now"), in.getLong("lastFetchAttemptedDate"), in.getDouble("debounceMs"));
            assertEquals(c.getString("name"), c.getBoolean("expected"), r);
        }
    }

    @Test
    public void testShouldRateLimitSource() throws Exception {
        JSONArray cases = casesOf("should-rate-limit-source.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            boolean r = SyncFetchPolicy.shouldRateLimitSource(
                    in.getLong("now"), in.getLong("lastFetchAttemptedDate"), in.getDouble("minIntervalMs"));
            assertEquals(c.getString("name"), c.getBoolean("expected"), r);
        }
    }

    @Test
    public void testMergeKnobs() throws Exception {
        JSONArray cases = casesOf("merge-knobs.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            SyncKnobs defaults = SyncKnobs.fromJSON(in.getJSONObject("defaults"));
            Object dataObj = in.opt("data");
            JSONObject data = dataObj instanceof JSONObject ? (JSONObject) dataObj : null;
            SyncKnobs merged = SyncKnobs.mergeKnobs(defaults, data);
            SyncKnobs expected = SyncKnobs.fromJSON(c.getJSONObject("expected"));
            assertEquals(c.getString("name"), expected, merged);
        }
    }

    @Test
    public void testIsStateStale() throws Exception {
        JSONArray cases = casesOf("is-state-stale.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            SyncSourceState state = SyncSourceState.fromJSON(in.getJSONObject("state"));
            SyncKnobs knobs = SyncKnobs.fromJSON(in.getJSONObject("knobs"));
            boolean r = SyncKnobs.isStateStale(state, knobs, in.getLong("now"));
            assertEquals(c.getString("name"), c.getBoolean("expected"), r);
        }
    }

    @Test
    public void testProcessSourceBlock() throws Exception {
        JSONArray cases = casesOf("process-source-block.vectors.json");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            JSONObject in = c.getJSONObject("input");
            SyncResponseBlock block = SyncResponseBlock.fromJSON(in.optJSONObject("block"));
            Number serverTime = (Number) orNull(in.opt("serverTime"));
            SyncSourceState state = SyncSourceState.fromJSON(in.getJSONObject("state"));
            SyncDecision decision = SyncProcessor.processSourceBlock(
                    block, serverTime, state, in.optString("mode", null));
            assertDeepEquals(c.getString("name"), c.getJSONObject("expected"), decision.toJSON());
        }
    }

    private static void assertDeepEquals(String name, JSONObject expected, JSONObject actual) {
        if (!JSONUtil.equals(expected, actual)) {
            fail(name + "\n  expected: " + expected + "\n  actual:   " + actual);
        }
    }
}
