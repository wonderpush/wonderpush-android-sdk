package com.wonderpush.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-object store transforms for the {@code popups} sync source.
 *
 * Ported from wonderpush-ios-sdk {@code WPSyncPopupsStore}. Upsert by {@code id} favoring the highest
 * {@code updateDate} (latest wins, including a "deleted" tombstone superseding an older active item),
 * then drop expired items. First-seen order is preserved.
 */
class SyncPopupsStore {

    /** The item's version for dedupe/precedence is its {@code updateDate} (missing/non-numeric -> 0). */
    private static long popupVersion(JSONObject item) {
        Object v = item.opt("updateDate");
        return v instanceof Number ? ((Number) v).longValue() : 0;
    }

    /** Expired iff expirationDate is a number strictly less than now. No expirationDate -> never expires. */
    private static boolean isExpired(JSONObject item, long now) {
        Object exp = item.opt("expirationDate");
        return exp instanceof Number && ((Number) exp).longValue() < now;
    }

    private static JSONArray dedupeAndPrune(JSONArray items, long now) {
        Map<String, JSONObject> byId = new HashMap<>();
        List<String> order = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            Object it = items.opt(i);
            if (!(it instanceof JSONObject)) continue;   // skip malformed entries
            Object itemId = ((JSONObject) it).opt("id");
            if (!(itemId instanceof String)) continue;
            String id = (String) itemId;
            JSONObject existing = byId.get(id);
            if (existing == null) order.add(id);
            // Later items win ties (incoming is newer than what is already stored).
            if (existing == null || popupVersion((JSONObject) it) >= popupVersion(existing)) {
                byId.put(id, (JSONObject) it);
            }
        }
        JSONArray out = new JSONArray();
        for (String id : order) {
            JSONObject it = byId.get(id);
            if (!isExpired(it, now)) out.put(it);
        }
        return out;
    }

    /** Full reset of the stored list from a full data payload. */
    static JSONArray resetPopupsData(Object data, long now) {
        return dedupeAndPrune(data instanceof JSONArray ? (JSONArray) data : new JSONArray(), now);
    }

    /** Upsert an array of full items onto the current list. */
    static JSONArray applyPopupsDelta(Object current, Object delta, long now) {
        JSONArray base = current instanceof JSONArray ? (JSONArray) current : new JSONArray();
        JSONArray incoming = delta instanceof JSONArray ? (JSONArray) delta : new JSONArray();
        JSONArray combined = new JSONArray();
        for (int i = 0; i < base.length(); i++) combined.put(base.opt(i));
        for (int i = 0; i < incoming.length(); i++) combined.put(incoming.opt(i));
        return dedupeAndPrune(combined, now);
    }

    /** Empty-reset: wipe the entire list. */
    static JSONArray clearPopups() {
        return new JSONArray();
    }

    private SyncPopupsStore() {}
}
