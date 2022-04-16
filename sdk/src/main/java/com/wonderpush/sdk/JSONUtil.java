package com.wonderpush.sdk;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.wonderpush.sdk.inappmessaging.internal.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONUtil {

    public static void merge(JSONObject base, JSONObject diff) throws JSONException {
        merge(base, diff, true);
    }

    public static void merge(JSONObject base, JSONObject diff, boolean nullFieldRemoves) throws JSONException {
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
                if ((vDiff != null && vDiff != JSONObject.NULL) || !nullFieldRemoves) {
                    base.put(key, vDiff);
                }
            } else if (vDiff instanceof JSONObject) {
                Object vBase = base.get(key);
                if (vBase instanceof JSONObject) {
                    merge((JSONObject)vBase, (JSONObject)vDiff, nullFieldRemoves);
                } else {
                    base.put(key, vDiff);
                }
            } else if (vDiff instanceof JSONArray) {
                base.put(key, new JSONArray(vDiff.toString()));
            } else if ((vDiff == null || vDiff == JSONObject.NULL) && nullFieldRemoves) {
                base.remove(key);
            } else {
                base.put(key, vDiff);
            }
        }
    }

    public static JSONObject diff(JSONObject from, JSONObject to) throws JSONException {
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

    public static void stripNulls(JSONObject object) throws JSONException {
        if (object == null) return;
        Iterator<String> it = object.keys();
        while (it.hasNext()) {
            String key = it.next();
            Object value = object.opt(key);
            if (value == null || value == JSONObject.NULL) {
                it.remove();
            } else if (value instanceof JSONObject) {
                stripNulls((JSONObject) value);
            } // leave JSONArrays (and any sub-objects) untouched
        }
    }

    public static boolean equals(JSONObject a, JSONObject b) {
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

    public static boolean equals(JSONArray a, JSONArray b) {
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

    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a instanceof Number && b instanceof Number) {
            if (a instanceof Float || b instanceof Float
                    || a instanceof Double || b instanceof Double) {
                return ((Number) a).doubleValue() == ((Number) b).doubleValue();
            }
            return ((Number) a).longValue() == ((Number) b).longValue();
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

    public static String getString(JSONObject object, String field) {
        if (!object.has(field) || object.isNull(field)) {
            return null;
        } else {
            return object.optString(field, null);
        }
    }

    public static JSONObject deepCopy(JSONObject from) throws JSONException {
        if (from == null) {
            return null;
        }
        return new JSONObject(from.toString());
    }

    public static Object parseAllJSONStrings(Object base) {
        if (base instanceof JSONObject) {
            JSONObject rtn = new JSONObject();
            Iterator<String> it = ((JSONObject) base).keys();
            while (it.hasNext()) {
                String key = it.next();
                try {
                    rtn.put(key, parseAllJSONStrings(((JSONObject) base).opt(key)));
                } catch (JSONException ex) {
                    WonderPush.logError("Failed to copy key " + key, ex);
                }
            }
            return rtn;
        }
        if (base instanceof JSONArray) {
            JSONArray rtn = new JSONArray();
            for (int i = 0; i < ((JSONArray) base).length(); ++i) {
                try {
                    rtn.put(i, parseAllJSONStrings(((JSONArray) base).opt(i)));
                } catch (JSONException ex) {
                    WonderPush.logError("Failed to copy value at index " + i, ex);
                }
            }
            return rtn;
        }
        if (base instanceof String && ((String) base).startsWith("{") && ((String) base).endsWith("}")) {
            try {
                return parseAllJSONStrings(new JSONObject((String) base));
            } catch (JSONException ex) {}
        }
        if (base instanceof String && ((String) base).startsWith("[") && ((String) base).endsWith("]")) {
            try {
                return parseAllJSONStrings(new JSONArray((String) base));
            } catch (JSONException ex) {}
        }
        return base;
    }

    public static String optString(JSONObject object, String field) {
        if (object == null) return null;
        if (!object.has(field) || object.isNull(field)) return null;
        Object value = object.opt(field);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public static Boolean optBoolean(JSONObject object, String field) {
        if (object == null) return null;
        if (!object.has(field) || object.isNull(field)) return null;
        Object value = object.opt(field);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    public static Integer optInteger(JSONObject object, String field) {
        if (object == null) return null;
        if (!object.has(field) || object.isNull(field)) return null;
        Object value = object.opt(field);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    public static long[] optLongArray(JSONObject object, String field) {
        if (object == null) return null;
        if (!object.has(field) || object.isNull(field)) return null;
        JSONArray array = object.optJSONArray(field);
        if (array == null) return null;
        long[] rtn = new long[array.length()];
        for (int i = 0, l = array.length(); i < l; ++i) {
            Object value = array.opt(i);
            if (value instanceof Long) {
                rtn[i] = (Long) value;
            } else if (value instanceof Number) {
                rtn[i] = ((Number) value).longValue();
            } else {
                return null;
            }
        }
        return rtn;
    }

    public static Uri optUri(JSONObject object, String field) {
        if (object == null) return null;
        if (!object.has(field) || object.isNull(field)) return null;
        String value = object.optString(field, null);
        if (value == null) return null;
        return Uri.parse(value);
    }

    /**
     * @see JSONObject#wrap(Object)
     */
    public static Object wrap(Object o) {
        if (o == null) {
            return JSONObject.NULL;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }
        if (o.equals(JSONObject.NULL)) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JSONArray((Collection) o);
            } else if (o.getClass().isArray()) {
                return JSONArray(o);
            }
            if (o instanceof Map) {
                return new JSONObject((Map) o);
            }
            if (o instanceof Boolean ||
                    o instanceof Byte ||
                    o instanceof Character ||
                    o instanceof Double ||
                    o instanceof Float ||
                    o instanceof Integer ||
                    o instanceof Long ||
                    o instanceof Short ||
                    o instanceof String) {
                return o;
            }
            if (o.getClass().getPackage().getName().startsWith("java.")) {
                return o.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * @see JSONArray#JSONArray(Object)
     */
    public static JSONArray JSONArray(Object array) throws JSONException {
        if (!array.getClass().isArray()) {
            throw new JSONException("Not a primitive array: " + array.getClass());
        }
        final JSONArray rtn = new JSONArray();
        final int length = Array.getLength(array);
        for (int i = 0; i < length; ++i) {
            rtn.put(wrap(Array.get(array, i)));
        }
        return rtn;
    }

    public static <T> List<T> JSONArrayToList(JSONArray array, Class<T> typecheck, boolean removeNulls) {
        final int length = array == null ? 0 : array.length();
        ArrayList<T> rtn = new ArrayList<>(length);
        if (array != null) {
            for (int i = 0; i < length; ++i) {
                try {
                    Object item = array.get(i);
                    if (!typecheck.isInstance(item)) continue;
                    if (removeNulls && item == JSONObject.NULL) continue;
                    rtn.add(typecheck.cast(item));
                } catch (JSONException ex) {
                    Log.e(WonderPush.TAG, "Unexpected exception in JSONArrayToList", ex);
                } catch (ClassCastException ex) {
                    Log.e(WonderPush.TAG, "Unexpected exception in JSONArrayToList", ex);
                }
            }
        }
        return rtn;
    }

    public static Bundle toBundle(JSONObject input) {
        if (input == null) {
            return null;
        }
        Bundle rtn = new Bundle();
        Iterator<String> it = input.keys();
        while (it.hasNext()) {
            String key = it.next();
            Object value = input.opt(key);
            if (value == null || value == JSONObject.NULL) {
                rtn.putString(key, null);
            } else if (value instanceof String) {
                rtn.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                rtn.putBoolean(key, (Boolean) value);
            } else if (value instanceof Double) {
                rtn.putDouble(key, (Double) value);
            } else if (value instanceof Integer) {
                rtn.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                rtn.putLong(key, (Long) value);
            } else if (value instanceof Number) {
                rtn.putDouble(key, ((Number) value).doubleValue());
            } else if (value instanceof JSONObject) {
                rtn.putBundle(key, toBundle((JSONObject) value));
            } else if (value instanceof JSONArray) {
                // TODO Detect common type between all values and build an array of the appropriate type
                WonderPush.logError("Coercing a JSONArray into an String[] for JSONObject to Bundle conversion: " + input.toString());
                JSONArray jsonArray = (JSONArray) value;
                int length = jsonArray.length();
                String[] list = new String[length];
                for (int i = 0; i < length; ++i) {
                    list[i] = jsonArray.optString(i, null);
                }
                rtn.putStringArray(key, list);
            }
        }
        return rtn;
    }

    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException jsonException) {
                return false;
            }
        }
        return true;
    }

    public static String[] jsonStringToStringArray(String jsonString){
        try{
            ArrayList<String> stringArray = new ArrayList<String>();

            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                stringArray.add(jsonArray.getString(i));
            }

            return stringArray.toArray(new String[0]);
        }
        catch(Exception exception){
            Logging.loge(exception.getLocalizedMessage());
        }

        return new String[]{};
    }

    public static Object[] jsonStringToObjectArray(String jsonString){
        try{
            ArrayList<Object> objectArray = new ArrayList<Object>();

            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                objectArray.add(jsonArray.get(i));
            }

            return objectArray.toArray(new Object[0]);
        }
        catch(Exception exception){
            Logging.loge(exception.getLocalizedMessage());
        }

        return new Object[]{};
    }

}
