package com.wonderpush.sdk;

import org.json.JSONObject;

class NotificationUrlModel extends NotificationModel {

    private String url;

    public NotificationUrlModel(String inputJSONString) {
        super(inputJSONString);
    }

    @Override
    protected void readFromJSONObject(JSONObject wpData) {
        setUrl(wpData.optString("url", null));
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
