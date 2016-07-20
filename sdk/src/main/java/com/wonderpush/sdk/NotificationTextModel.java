package com.wonderpush.sdk;

import org.json.JSONObject;

class NotificationTextModel extends NotificationModel {

    private String message;

    public NotificationTextModel(String inputJSONString) {
        super(inputJSONString);
    }

    @Override
    protected void readFromJSONObject(JSONObject wpData) {
        message = JSONUtil.getString(wpData, "message");
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
