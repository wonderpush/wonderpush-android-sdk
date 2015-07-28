package com.wonderpush.sdk;

import org.json.JSONObject;

class NotificationSimpleModel extends NotificationModel {

    public NotificationSimpleModel(String inputJSONString) {
        super(inputJSONString);
    }

    @Override
    protected void readFromJSONObject(JSONObject wpData) {
        // Noop
    }

}
