package com.wonderpush.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.message.BasicNameValuePair;

/**
 * A class that handles the parameter to provide to either an api call or a view.
 */
class RequestParams extends com.loopj.android.http.RequestParams implements Parcelable {

    public static final String TAG = "RequestParams";

    public RequestParams(Parcel in) throws JSONException {
        JSONObject json = new JSONObject(in.readString());
        Iterator<?> it = json.keys();
        String key;
        while (it.hasNext()) {
            key = (String) it.next();
            this.put(key, json.optString(key));
        }
    }

    /**
     * Constructs a new empty <code>RequestParams</code> instance.
     */
    public RequestParams() {
        super();
    }

    /**
     * Constructs a new RequestParams instance containing the key/value
     * string params from the specified map.
     *
     * @param source
     *            The source key/value string map to add.
     */
    public RequestParams(Map<String, String> source) {
        super(source);
    }

    /**
     * Constructs a new RequestParams instance and populate it with multiple
     * initial key/value string param.
     *
     * @param keysAndValues
     *            A sequence of keys and values. Objects are automatically
     *            converted to Strings (including the value {@code null}).
     * @throws IllegalArgumentException
     *            If the number of arguments isn't even.
     */
    public RequestParams(Object... keysAndValues) {
        super(keysAndValues);
    }

    /**
     * Constructs a new RequestParams instance and populate it with a single
     * initial key/value string param.
     *
     * @param key
     *            The key name for the intial param.
     * @param value
     *            The value string for the initial param.
     */
    public RequestParams(String key, String value) {
        super(key, value);
    }

    // Only redeclared for package private access
    @SuppressWarnings("EmptyMethod")
    @Override
    protected List<BasicNameValuePair> getParamsList() {
        return super.getParamsList();
    }

    public String getURLEncodedString() {
        return getParamString();
    }

    @Override
    public int describeContents() {
        return 0;
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
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeString(toJSONObject().toString());
    }

    public static final Creator<RequestParams> CREATOR = new Creator<RequestParams>() {
        public RequestParams createFromParcel(Parcel in) {
            try {
                return new RequestParams(in);
            } catch (Exception e) {
                Log.e(TAG, "Error while unserializing JSON from a WonderPush.RequestParams", e);
                return null;
            }
        }

        public RequestParams[] newArray(int size) {
            return new RequestParams[size];
        }
    };

}
