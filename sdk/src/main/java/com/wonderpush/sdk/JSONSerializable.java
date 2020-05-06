package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONSerializable {

    JSONObject toJSON() throws JSONException;

}
