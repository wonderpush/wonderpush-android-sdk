package com.wonderpush.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Leaf-level pure predicates for the sdk-sync response processor.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncVersionId} /
 * wonderpush-javascript-sdk {@code src/wonderpush/sync-versionid.ts}.
 *   - versionId total order: sdk-sync/docs/docs/sync/algorithm.md:259-264
 *   - acceptance check:      sdk-sync/docs/docs/sync/algorithm.md:224-227
 *
 * A "versionId" is the JS {@code number | string | null} union. In Java we carry it as
 * {@code Object}:
 *   - {@link Number} -> int64 version id
 *   - {@link String} -> string version id
 *   - {@code null} or {@link JSONObject#NULL} -> the "null-missing" sentinel
 */
class SyncVersionId {

    /** {@code null} and {@link JSONObject#NULL} are both the null-missing sentinel. */
    static boolean isMissing(Object v) {
        return v == null || v == JSONObject.NULL;
    }

    /**
     * Total order for mixed-type version identifiers: null/missing &lt; int64 &lt; string.
     * Numbers compare naturally; strings compare code-unit-wise, case-sensitive (Java
     * {@link String#compareTo} compares UTF-16 code units, matching the JS reference's {@code <}).
     * Returns -1 / 0 / 1.
     */
    static int compareVersionId(Object a, Object b) {
        boolean aMissing = isMissing(a);
        boolean bMissing = isMissing(b);
        if (aMissing && bMissing) return 0;
        if (aMissing) return -1;   // null < anything non-null
        if (bMissing) return 1;

        boolean aIsNumber = a instanceof Number;
        boolean bIsNumber = b instanceof Number;
        if (aIsNumber && !bIsNumber) return -1;   // int64 < string (arbitrary, per spec)
        if (!aIsNumber && bIsNumber) return 1;

        if (aIsNumber && bIsNumber) {
            long x = ((Number) a).longValue();
            long y = ((Number) b).longValue();
            return Long.compare(x, y);
        }

        int c = a.toString().compareTo(b.toString());
        return c < 0 ? -1 : (c > 0 ? 1 : 0);
    }

    /**
     * Decide whether to apply a payload-bearing response to local state. Two branches:
     *   (a) monotonic — strictly newer (version, versionId, readDate) tuple, or
     *   (b) empty-reset — version == 0 &amp;&amp; data is {@code {}} or {@code []} &amp;&amp; readDate &gt; lastReadDate.
     * Callers must only invoke this on responses that carry data and/or delta.
     */
    static boolean acceptsResponse(long version, Object versionId, long readDate, Object data,
                                   long lastVersion, Object lastVersionId, long lastReadDate) {
        // Branch (a): monotonic.
        if (version > lastVersion) return true;
        if (version == lastVersion) {
            int cmp = compareVersionId(versionId, lastVersionId);
            if (cmp > 0) return true;
            if (cmp == 0 && readDate > lastReadDate) return true;
        }
        // Branch (b): empty-reset — server affirms no data for the current identifier set.
        if (version == 0 && isEmptyDataPayload(data) && readDate > lastReadDate) {
            return true;
        }
        return false;
    }

    /** True iff data is {@code {}} (empty object) or {@code []} (empty array). */
    static boolean isEmptyDataPayload(Object data) {
        if (data instanceof JSONArray) return ((JSONArray) data).length() == 0;
        if (data instanceof JSONObject) return ((JSONObject) data).length() == 0;
        return false;
    }

    private SyncVersionId() {}
}
