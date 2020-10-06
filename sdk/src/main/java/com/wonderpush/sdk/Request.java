package com.wonderpush.sdk;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import okhttp3.FormBody;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A serializable object that represents a request to the WonderPush API.
 */
class Request {
    private static final String TAG = ApiClient.class.getSimpleName();

    String mUserId;
    ApiClient.HttpMethod mMethod;
    Params mParams;
    ResponseHandler mHandler;
    String mResource;

    public Request(String userId, ApiClient.HttpMethod method, String resource, Params params, ResponseHandler handler) {
        mUserId = userId;
        mMethod = method;
        mParams = params;
        mHandler = handler;
        mResource = resource;
    }

    public Request(JSONObject data) throws JSONException {
        mUserId = data.has("userId") ? JSONUtil.getString(data, "userId") : WonderPushConfiguration.getUserId();
        try {
            mMethod = ApiClient.HttpMethod.valueOf(JSONUtil.getString(data, "method"));
        } catch (IllegalArgumentException ex) {
            mMethod = ApiClient.HttpMethod.values()[data.getInt("method")];
        }
        mResource = data.getString("resource");
        JSONObject paramsJson = data.getJSONObject("params");
        mParams = new Params();
        @SuppressWarnings("unchecked")
        Iterator<String> keys = paramsJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            mParams.put(key, paramsJson.getString(key));
        }
    }

    public JSONObject toJSON() {
        try {
            JSONObject result = new JSONObject();
            result.put("userId", mUserId);
            result.put("method", mMethod.name());
            result.put("resource", mResource);
            JSONObject params = new JSONObject();
            if (null != mParams) {
                for (BasicNameValuePair pair : mParams.getParamsList()) {
                    params.put(pair.getName(), pair.getValue());
                }
            }
            result.put("params", params);
            return result;
        } catch (JSONException e) {
            WonderPush.logError("Failed to serialize job", e);
            return null;
        }
    }

    public String getUserId() {
        return mUserId;
    }

    public ApiClient.HttpMethod getMethod() {
        return mMethod;
    }

    public Params getParams() {
        return mParams;
    }

    public ResponseHandler getHandler() {
        return mHandler;
    }

    public String getResource() {
        return mResource;
    }

    public void setMethod(ApiClient.HttpMethod mMethod) {
        this.mMethod = mMethod;
    }

    public void setParams(Params mParams) {
        this.mParams = mParams;
    }

    public void setHandler(ResponseHandler mHandler) {
        this.mHandler = mHandler;
    }

    public void setResource(String resource) {
        this.mResource = resource;
    }

    @Override
    protected Object clone() {
        return new Request(mUserId, mMethod, mResource, mParams, mHandler);
    }

    /**
     * Generates X-WonderPush-Authorization header with request signature
     *
     * @return The authorization header or null for GET requests
     */
    protected BasicNameValuePair getAuthorizationHeader() {
        return getAuthorizationHeader(mMethod, Uri.parse(String.format("%s%s", WonderPush.getBaseURL(), mResource)), mParams);
    }

    protected static BasicNameValuePair getAuthorizationHeader(ApiClient.HttpMethod method, Uri uri, Params params) {
        try {
            StringBuilder sb = new StringBuilder();

            // Step 1: add HTTP method uppercase
            sb.append(method.name().toUpperCase());
            sb.append('&');

            // Step 2: add the URI
            // Query string is stripped from resource
            sb.append(encode(String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getEncodedPath())));

            // Step 3: add URL encoded parameters
            sb.append('&');

            // Params from the URL
            List<BasicNameValuePair> unencodedParams = new ArrayList<>();
            Params queryStringParams = QueryStringParser.getRequestParams(uri.getQuery());
            if (queryStringParams != null) {
                unencodedParams.addAll(queryStringParams.getParamsList());
            }

            // Params from the request
            if (params != null) {
                unencodedParams.addAll(params.getParamsList());
            }

            // Encode and sort params
            List<BasicNameValuePair> encodedParams = new ArrayList<>(unencodedParams.size());
            for (BasicNameValuePair pair : unencodedParams) {
                encodedParams.add(new BasicNameValuePair(encode(pair.getName()), encode(pair.getValue())));
            }
            Collections.sort(encodedParams, new Comparator<BasicNameValuePair>() {
                @Override
                public int compare(BasicNameValuePair lhs, BasicNameValuePair rhs) {
                    int rtn = lhs.getName().compareTo(rhs.getName());
                    if (rtn == 0) {
                        rtn = lhs.getValue().compareTo(rhs.getValue());
                    }
                    return rtn;
                }
            });

            // Append to the clear signature
            boolean first = true;
            for (BasicNameValuePair pair : encodedParams) {
                if (first) {
                    first = false;
                } else {
                    sb.append("%26");
                }
                sb.append(encode(String.format("%s=%s", pair.getName(), pair.getValue())));
            }

            // Step 4: add body
            sb.append('&');
            // TODO: add the body here when we support other content types than application/x-www-form-urlencoded

            // Final step: Hash and format header
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(WonderPush.getClientSecret().getBytes("UTF-8"), mac.getAlgorithm());
            mac.init(secret);
            byte[] digest = mac.doFinal(sb.toString().getBytes());
            String sig = android.util.Base64.encodeToString(digest, Base64.DEFAULT).trim();
            String encodedSig = encode(sig.trim());
            return new BasicNameValuePair("X-WonderPush-Authorization", String.format("WonderPush sig=\"%s\", meth=\"0\"", encodedSig));
        } catch (Exception e) {
            Log.e(TAG, "Could not generate signature", e);
            return null;
        }
    }

    protected static String encode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    @Override
    public String toString() {
        return "" + mMethod + " " + mResource + "?" + mParams;
    }

    public static class BasicNameValuePair {
        private String name;
        private String value;
        public BasicNameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * A class that handles the parameter to provide to either an api call or a view.
     */
    static class Params implements Parcelable {

        public static final String TAG = "RequestParams";

        public Params(Parcel in) throws JSONException {
            JSONObject json = new JSONObject(in.readString());
            Iterator<?> it = json.keys();
            String key;
            while (it.hasNext()) {
                key = (String) it.next();
                this.put(key, json.optString(key));
            }
        }

        public String toString() {
            return getURLEncodedString();
        }

        public String getURLEncodedString() {
            Iterator<BasicNameValuePair> iter = getParamsList().iterator();
            StringBuffer buffer = new StringBuffer();
            while (iter.hasNext()) {
                BasicNameValuePair pair = iter.next();
                if (pair.getName() == null || pair.getValue() == null) continue;
                try {
                    buffer.append(URLEncoder.encode(pair.getName(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                }
                buffer.append("=");
                try {
                    buffer.append(URLEncoder.encode(pair.getValue(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                }
                if (iter.hasNext()) buffer.append("&");
            }
            return buffer.toString();
        }

        public JSONObject toJSONObject() {
            JSONObject result = new JSONObject();
            List<BasicNameValuePair> params = getParamsList();
            for (BasicNameValuePair parameter : params) {
                try {
                    result.put(parameter.getName(), parameter.getValue());
                } catch (JSONException e) {
                    WonderPush.logError("Failed to add parameter " + parameter, e);
                }
            }
            return result;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            destination.writeString(toJSONObject().toString());
        }

        public static final Creator<Params> CREATOR = new Creator<Params>() {
            public Params createFromParcel(Parcel in) {
                try {
                    return new Params(in);
                } catch (Exception e) {
                    Log.e(TAG, "Error while unserializing JSON from a WonderPush.RequestParams", e);
                    return null;
                }
            }

            public Params[] newArray(int size) {
                return new Params[size];
            }
        };

        protected final ConcurrentSkipListMap<String, String> urlParams = new ConcurrentSkipListMap<String, String>();
        protected final ConcurrentSkipListMap<String, Object> urlParamsWithObjects = new ConcurrentSkipListMap<String, Object>();

        /**
         * Constructs a new empty {@code RequestParams} instance.
         */
        public Params() {
            this((Map<String, String>) null);
        }

        /**
         * Constructs a new RequestParams instance containing the key/value string params from the
         * specified map.
         *
         * @param source the source key/value string map to add.
         */
        public Params(Map<String, String> source) {
            if (source != null) {
                for (Map.Entry<String, String> entry : source.entrySet()) {
                    put(entry.getKey(), entry.getValue());
                }
            }
        }

        /**
         * Constructs a new RequestParams instance and populate it with a single initial key/value
         * string param.
         *
         * @param key   the key name for the intial param.
         * @param value the value string for the initial param.
         */
        public Params(final String key, final String value) {
            this(new HashMap<String, String>() {{
                put(key, value);
            }});
        }

        /**
         * Adds a key/value string pair to the request.
         *
         * @param key   the key name for the new param.
         * @param value the value string for the new param.
         */
        public void put(String key, String value) {
            if (key != null && value != null) {
                urlParams.put(key, value);
            }
        }

        /**
         * Adds param with non-string value (e.g. Map, List, Set).
         *
         * @param key   the key name for the new param.
         * @param value the non-string value object for the new param.
         */
        public void put(String key, Object value) {
            if (key != null && value != null) {
                urlParamsWithObjects.put(key, value);
            }
        }

        /**
         * Adds string value to param which can have more than one value.
         *
         * @param key   the key name for the param, either existing or new.
         * @param value the value string for the new param.
         */
        public void add(String key, String value) {
            if (key != null && value != null) {
                Object params = urlParamsWithObjects.get(key);
                if (params == null) {
                    // Backward compatible, which will result in "k=v1&k=v2&k=v3"
                    params = new HashSet<String>();
                    this.put(key, params);
                }
                if (params instanceof List) {
                    ((List<Object>) params).add(value);
                } else if (params instanceof Set) {
                    ((Set<Object>) params).add(value);
                }
            }
        }

        /**
         * Removes a parameter from the request.
         *
         * @param key the key name for the parameter to remove.
         */
        public void remove(String key) {
            urlParams.remove(key);
            urlParamsWithObjects.remove(key);
        }

        /**
         * Check if a parameter is defined.
         *
         * @param key the key name for the parameter to check existence.
         * @return Boolean
         */
        public boolean has(String key) {
            return urlParams.get(key) != null ||
                    urlParamsWithObjects.get(key) != null;
        }

        protected List<BasicNameValuePair> getParamsList() {
            List<BasicNameValuePair> lparams = new LinkedList<BasicNameValuePair>();

            for (ConcurrentSkipListMap.Entry<String, String> entry : urlParams.entrySet()) {
                lparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            lparams.addAll(getParamsList(null, urlParamsWithObjects));

            return lparams;
        }

        protected FormBody getFormBody() {
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            for (BasicNameValuePair pair : getParamsList()) {
                formBodyBuilder.add(pair.getName(), pair.getValue());
            }
            return formBodyBuilder.build();
        }

        private List<BasicNameValuePair> getParamsList(String key, Object value) {
            List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
            if (value instanceof Map) {
                Map map = (Map) value;
                List list = new ArrayList<Object>(map.keySet());
                // Ensure consistent ordering in query string
                if (list.size() > 0 && list.get(0) instanceof Comparable) {
                    Collections.sort(list);
                }
                for (Object nestedKey : list) {
                    if (nestedKey instanceof String) {
                        Object nestedValue = map.get(nestedKey);
                        if (nestedValue != null) {
                            params.addAll(getParamsList(key == null ? (String) nestedKey : String.format(Locale.US, "%s[%s]", key, nestedKey),
                                    nestedValue));
                        }
                    }
                }
            } else if (value instanceof List) {
                List list = (List) value;
                int listSize = list.size();
                for (int nestedValueIndex = 0; nestedValueIndex < listSize; nestedValueIndex++) {
                    params.addAll(getParamsList(String.format(Locale.US, "%s[%d]", key, nestedValueIndex), list.get(nestedValueIndex)));
                }
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                int arrayLength = array.length;
                for (int nestedValueIndex = 0; nestedValueIndex < arrayLength; nestedValueIndex++) {
                    params.addAll(getParamsList(String.format(Locale.US, "%s[%d]", key, nestedValueIndex), array[nestedValueIndex]));
                }
            } else if (value instanceof Set) {
                Set set = (Set) value;
                for (Object nestedValue : set) {
                    params.addAll(getParamsList(key, nestedValue));
                }
            } else {
                params.add(new BasicNameValuePair(key, value.toString()));
            }
            return params;
        }
    }
}
