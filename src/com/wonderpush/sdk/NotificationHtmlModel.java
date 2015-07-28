package com.wonderpush.sdk;

import org.json.JSONObject;

class NotificationHtmlModel extends NotificationModel {

    private String message;
    private String baseUrl;

    public NotificationHtmlModel(String inputJSONString) {
        super(inputJSONString);
    }

    @Override
    protected void readFromJSONObject(JSONObject wpData) {
        message = wpData.optString("message", null);
        baseUrl = wpData.optString("baseUrl", null);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
