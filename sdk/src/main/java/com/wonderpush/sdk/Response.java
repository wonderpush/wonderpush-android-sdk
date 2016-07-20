package com.wonderpush.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An HTTP response object
 */
class Response {

    JSONObject mJson;
    String mError;

    public Response(String responseContent) {
        try {
            mJson = new JSONObject(responseContent);
        } catch (JSONException e) {
            mError = responseContent;
        }
    }

    public Response(JSONObject responseJson) {
        mJson = responseJson;
    }

    public boolean isError() {
        return mJson == null || mJson.has("error");
    }

    public String getErrorMessage() {
        if (!isError())
            return null;

        if (mJson == null) {
            return mError;
        }

        JSONObject error = mJson.optJSONObject("error");
        if (error == null) {
            return null;
        }
        return JSONUtil.getString(error, "message");
    }

    public int getErrorStatus() {
        if (!isError() || mJson == null)
            return 0;

        JSONObject error = mJson.optJSONObject("error");
        if (error == null) {
            return 0;
        }
        return error.optInt("status", 0);
    }

    public int getErrorCode() {
        if (!isError() || mJson == null)
            return 0;

        JSONObject error = mJson.optJSONObject("error");
        if (error == null) {
            return 0;
        }
        return error.optInt("code", 0);
    }

    public JSONObject getJSONObject() {
        return mJson;
    }

    @Override
    public String toString() {
        if (mJson == null) {
            return mError;
        }
        return mJson.toString();
    }

}
