
package com.wonderpush.sdk.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class JSONParcelable implements Parcelable {

    public static final String TAG = "JSONParcelable";

    protected final JSONObject json;

    public JSONParcelable(Parcel in) throws JSONException {
        this(in.readString());
    }

    public JSONParcelable(String serializedJSON) throws JSONException {
        this(new JSONObject(serializedJSON));
    }

    public JSONParcelable(JSONObject json) {
        this.json = json;
    }

    public JSONParcelable() {
        this.json = new JSONObject();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(json.toString());
    }

    public JSONObject getJSONObject() {
        return json;
    }

    public static final Parcelable.Creator<JSONParcelable> CREATOR = new Parcelable.Creator<JSONParcelable>() {

        public JSONParcelable createFromParcel(Parcel in) {
            try {
                return new JSONParcelable(in);
            } catch (JSONException e) {
                Log.e(TAG, "Error while unserializing JSON from a JSONParcel", e);
                return null;
            }
        }

        public JSONParcelable[] newArray(int size) {
            return new JSONParcelable[size];
        }

    };

}
