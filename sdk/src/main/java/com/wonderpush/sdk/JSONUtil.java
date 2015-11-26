package com.wonderpush.sdk;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class JSONUtil {

    protected static void merge(JSONObject base, JSONObject diff) throws JSONException {
        if (base == null) throw new NullPointerException();
        if (diff == null) return;

        Iterator<String> it = diff.keys();
        while (it.hasNext()) {
            String key = it.next();
            Object vDiff = diff.get(key);
            if (!base.has(key)) {
                if (vDiff instanceof JSONObject) {
                    vDiff = new JSONObject(vDiff.toString());
                } else if (vDiff instanceof JSONArray) {
                    vDiff = new JSONArray(vDiff.toString());
                }
                base.put(key, vDiff);
            } else if (vDiff instanceof JSONObject) {
                Object vBase = base.get(key);
                if (vBase instanceof JSONObject) {
                    merge((JSONObject)vBase, (JSONObject)vDiff);
                } else {
                    base.put(key, vDiff);
                }
            } else if (vDiff instanceof JSONArray) {
                base.put(key, new JSONArray(vDiff.toString()));
            } else {
                base.put(key, vDiff);
            }
        }
    }

    protected static JSONObject diff(JSONObject from, JSONObject to) throws JSONException {
        if (from == null) {
            if (to == null) {
                return null;
            } else {
                return new JSONObject(to.toString());
            }
        } else if (to == null) {
            return null;
        }

        JSONObject rtn = new JSONObject();
        Iterator<String> it;

        it = from.keys();
        while (it.hasNext()) {
            String key = it.next();
            if (!to.has(key)) {
                rtn.put(key, JSONObject.NULL);
                continue;
            }
            Object vFrom = from.opt(key);
            Object vTo = to.opt(key);
            if (!equals(vFrom, vTo)) {
                if (vFrom instanceof JSONObject && vTo instanceof JSONObject) {
                    rtn.put(key, diff((JSONObject)vFrom, (JSONObject)vTo));
                } else if (vTo instanceof JSONObject) {
                    rtn.put(key, new JSONObject(vTo.toString()));
                } else if (vTo instanceof JSONArray) {
                    rtn.put(key, new JSONArray(vTo.toString()));
                } else {
                    rtn.put(key, vTo);
                }
            }
        }

        it = to.keys();
        while (it.hasNext()) {
            String key = it.next();
            if (from.has(key)) continue;
            Object vTo = to.opt(key);
            if (vTo instanceof JSONObject) {
                rtn.put(key, new JSONObject(vTo.toString()));
            } else if (vTo instanceof JSONArray) {
                rtn.put(key, new JSONArray(vTo.toString()));
            } else {
                rtn.put(key, vTo);
            }
        }

        return rtn;
    }

    protected static boolean equals(JSONObject a, JSONObject b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a.length() != b.length()) {
            return false;
        }

        Iterator<String> it;
        it = a.keys();
        while (it.hasNext()) {
            String key = it.next();
            if (!b.has(key) || !equals(a.opt(key), b.opt(key))) {
                return false;
            }
        }

        return true;
    }

    protected static boolean equals(JSONArray a, JSONArray b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a.length() != b.length()) {
            return false;
        }

        for (int i = 0; i < a.length(); ++i) {
            if (!equals(a.opt(i), b.opt(i))) {
                return false;
            }
        }

        return true;
    }

    protected static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a.getClass() != b.getClass()) {
            return false;
        } else if (a instanceof JSONObject) {
            return equals((JSONObject)a, (JSONObject)b);
        } else if (a instanceof JSONArray) {
            return equals((JSONArray)a, (JSONArray)b);
        } else {
            return a.equals(b);
        }
    }

}
