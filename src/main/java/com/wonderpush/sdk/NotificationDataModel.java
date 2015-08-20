package com.wonderpush.sdk;

import org.json.JSONObject;

class NotificationDataModel extends NotificationModel {

    public NotificationDataModel(String inputJSONString) {
        super(inputJSONString);
    }

    @Override
    protected void readFromJSONObject(JSONObject wpData) {
        // Noop
    }

}
