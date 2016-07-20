package com.wonderpush.sdk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

abstract class NotificationModel {

    private static final String TAG = WonderPush.TAG;

    static class NotTargetedForThisInstallationException extends Exception {
        private static final long serialVersionUID = -1642569383307930845L;
        public NotTargetedForThisInstallationException(String detailMessage) {
            super(detailMessage);
        }
    }

    interface Builder {
        NotificationModel build(String inputJSONString);
    }

    enum Type {
        SIMPLE("simple", new Builder() {
            @Override
            public NotificationModel build(String inputJSONString) {
                return new NotificationSimpleModel(inputJSONString);
            }
        }),
        DATA("data", new Builder() {
            @Override
            public NotificationModel build(String inputJSONString) {
                return new NotificationDataModel(inputJSONString);
            }
        }),
        TEXT("text", new Builder() {
            @Override
            public NotificationModel build(String inputJSONString) {
                return new NotificationTextModel(inputJSONString);
            }
        }),
        HTML("html", new Builder() {
            @Override
            public NotificationModel build(String inputJSONString) {
                return new NotificationHtmlModel(inputJSONString);
            }
        }),
        MAP("map", new Builder() {
            @Override
            public NotificationModel build(String inputJSONString) {
                return new NotificationMapModel(inputJSONString);
            }
        }),
        URL("url", new Builder() {
            @Override
            public NotificationModel build(String inputJSONString) {
                return new NotificationUrlModel(inputJSONString);
            }
        }),
        ;

        private final String type;
        private final Builder builder;

        Type(String type, Builder builder) {
            this.type = type;
            this.builder = builder;
        }

        @Override
        public String toString() {
            return type;
        }

        public Builder getBuilder() {
            return builder;
        }

        public static Type fromString(String type) {
            if (type == null) {
                throw new NullPointerException();
            }
            for (Type notificationType : Type.values()) {
                if (type.equals(notificationType.toString())) {
                    return notificationType;
                }
            }
            throw new IllegalArgumentException("Constant \"" + type + "\" is not a known notification type");
        }
    }

    private final String inputJSONString;

    private String targetedInstallation;
    private String campaignId;
    private String notificationId;
    private Type type;
    private AlertModel alert;
    private String targetUrl;
    public List<ActionModel> actions = new ArrayList<>();
    private boolean receipt;

    // Common in-app message data
    private final AtomicReference<ButtonModel> choice = new AtomicReference<>();
    private final List<ButtonModel> buttons = new ArrayList<>(3);
    private String title;

    public static NotificationModel fromGCMBroadcastIntent(Intent intent)
            throws NotTargetedForThisInstallationException
    {
        try {
            Bundle extras = intent.getExtras();
            if (extras == null || extras.isEmpty()) { // has effect of unparcelling Bundle
                WonderPush.logDebug("Received broadcasted intent has no extra");
                return null;
            }
            String wpDataJson = extras.getString(WonderPushGcmClient.WONDERPUSH_NOTIFICATION_EXTRA_KEY);
            if (wpDataJson == null) {
                WonderPush.logDebug("Received broadcasted intent has no data for WonderPush");
                return null;
            }

            WonderPush.logDebug("Received broadcasted intent: " + intent);
            WonderPush.logDebug("Received broadcasted intent extras: " + extras.toString());
            for (String key : extras.keySet()) {
                WonderPush.logDebug("Received broadcasted intent extras " + key + ": " + extras.get(key));
            }

            try {
                JSONObject wpData = new JSONObject(wpDataJson);
                WonderPush.logDebug("Received broadcasted intent WonderPush data: " + wpDataJson);
                return fromGCMNotificationJSONObject(wpData, extras);
            } catch (JSONException e) {
                WonderPush.logDebug("data is not a well-formed JSON object", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while receiving a notification with intent " + intent, e);
        }
        return null;
    }

    public static NotificationModel fromLocalIntent(Intent intent) {
        if (NotificationManager.containsExplicitNotification(intent)) {

            String notifString = intent.getData().getQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER);
            if (notifString == null) {
                return null;
            }
            try {
                JSONObject notifParsed = new JSONObject(notifString);
                return NotificationModel.fromGCMNotificationJSONObject(notifParsed, null);
            } catch (JSONException e) {
                WonderPush.logDebug("data is not a well-formed JSON object", e);
            } catch (NotTargetedForThisInstallationException e) {
                WonderPush.logError("Notifications not targeted for this installation should have been filtered earlier", e);
            }

        } else if (NotificationManager.containsWillOpenNotification(intent)) {

            try {
                Intent pushIntent = intent.getParcelableExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_RECEIVED_PUSH_NOTIFICATION);
                return fromGCMBroadcastIntent(pushIntent);
            } catch (NotTargetedForThisInstallationException e) {
                WonderPush.logError("Notifications not targeted for this installation should have been filtered earlier", e);
            }

        }
        return null;
    }

    public static NotificationModel fromGCMNotificationJSONObject(JSONObject wpData, Bundle extras)
            throws NotTargetedForThisInstallationException
    {
        try {
            // Get the notification type
            Type type;
            try {
                type = NotificationModel.Type.fromString(JSONUtil.getString(wpData, "type"));
            } catch (Exception ex) {
                WonderPush.logError("Failed to read notification type", ex);
                if (wpData.has("alert") || extras != null && extras.containsKey("alert")) {
                    type = NotificationModel.Type.SIMPLE;
                } else {
                    type = NotificationModel.Type.DATA;
                }
                WonderPush.logDebug("Inferred notification type: " + type);
            }
            if (type == null) {
                return null;
            }

            // Instantiate the appropriate non-abstract subclass
            NotificationModel rtn = type.getBuilder().build(wpData.toString());

            // Read common fields
            rtn.setType(type);
            rtn.setTargetedInstallation(JSONUtil.getString(wpData, "@"));
            rtn.setCampaignId(JSONUtil.getString(wpData, "c"));
            rtn.setNotificationId(JSONUtil.getString(wpData, "n"));
            rtn.setTargetUrl(JSONUtil.getString(wpData, "targetUrl"));
            rtn.setReceipt(wpData.optBoolean("receipt", true));

            // Read notification content
            JSONObject wpAlert = wpData.optJSONObject("alert");
            if (wpAlert != null) {
                rtn.setAlert(AlertModel.fromJSON(wpAlert));
            } else if (extras != null) {
                rtn.setAlert(AlertModel.fromOldFormatStringExtra(extras.getString("alert"))); // <= v1.1.0.0 format
            } else {
                // We are probably parsing an opened notification and the extras was not given.
                // We are not interested in showing a notification anyway, the in-app message is what's important now.
            }

            // Read actions
            JSONArray actions = wpData.optJSONArray("actions");
            int actionCount = actions != null ? actions.length() : 0;
            for (int i = 0 ; i < actionCount ; ++i) {
                JSONObject action = actions.optJSONObject(i);
                rtn.addAction(new ActionModel(action));
            }

            // Read common in-app fields
            rtn.setTitle(JSONUtil.getString(wpData, "title"));

            JSONArray buttons = wpData.optJSONArray("buttons");
            int buttonCount = buttons != null ? buttons.length() : 0;
            for (int i = 0 ; i < buttonCount ; ++i) {
                JSONObject wpButton = buttons.optJSONObject(i);
                if (wpButton == null) {
                    continue;
                }
                rtn.addButton(new ButtonModel(wpButton));
            }

            // Let the non-abstract subclass read the rest and return
            rtn.readFromJSONObject(wpData);
            return rtn;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while parsing a notification with JSON input " + wpData.toString(), e);
        }
        return null;
    }

    protected abstract void readFromJSONObject(JSONObject wpData);

    public NotificationModel(String inputJSONString) {
        this.inputJSONString = inputJSONString;
    }

    public String getInputJSONString() {
        return inputJSONString;
    }

    public String getTargetedInstallation() {
        return targetedInstallation;
    }

    public void setTargetedInstallation(String targetedInstallation) {
        this.targetedInstallation = targetedInstallation;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AlertModel getAlert() {
        return alert;
    }

    public void setAlert(AlertModel alert) {
        this.alert = alert;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public List<ActionModel> getActions() {
        return actions;
    }

    public void setActions(List<ActionModel> actions) {
        this.actions = actions;
    }

    public void addAction(ActionModel action) {
        if (action != null) {
            actions.add(action);
        }
    }

    public boolean getReceipt() {
        return receipt;
    }

    public void setReceipt(boolean receipt) {
        this.receipt = receipt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getButtonCount() {
        return buttons.size();
    }

    public ButtonModel getButton(int index) {
        return buttons.get(index);
    }

    public void addButton(ButtonModel button) {
        if (button != null) {
            buttons.add(button);
        }
    }

    public AtomicReference<ButtonModel> getChosenButton() {
        return choice;
    }

}
