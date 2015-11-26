package com.wonderpush.sdk;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class ButtonModel {

    public String label;
    public List<ActionModel> actions;

    public ButtonModel() {
        actions = new ArrayList<>(0);
    }

    public ButtonModel(JSONObject data) {
        if (data == null) {
            return;
        }

        label = data.optString("label");
        JSONArray actions = data.optJSONArray("actions");
        int actionCount = actions != null ? actions.length() : 0;
        this.actions = new ArrayList<>(actionCount);
        for (int i = 0 ; i < actionCount ; ++i) {
            JSONObject action = actions.optJSONObject(i);
            this.actions.add(new ActionModel(action));
        }
    }

}
