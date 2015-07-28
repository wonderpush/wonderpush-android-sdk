package com.wonderpush.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

abstract class NotificationModel {

    private static final String TAG = WonderPush.TAG;

    static class NotTargetedForThisInstallationException extends Exception {
        private static final long serialVersionUID = -1642569383307930845L;
        public NotTargetedForThisInstallationException(String detailMessage) {
            super(detailMessage);
        }
    }

    static enum Type {
        SIMPLE("simple", NotificationSimpleModel.class),
        DATA("data", NotificationDataModel.class),
        TEXT("text", NotificationTextModel.class),
        HTML("html", NotificationHtmlModel.class),
        MAP("map", NotificationMapModel.class),
        URL("url", NotificationUrlModel.class),
        ;

        private String type;
        private Class<? extends NotificationModel> clazz;

        private Type(String type, Class<? extends NotificationModel> clazz) {
            this.type = type;
            this.clazz = clazz;
        }

        @Override
        public String toString() {
            return type;
        }

        public Class<? extends NotificationModel> getImplementation() {
            return clazz;
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

    // Common in-app message data
    private final AtomicReference<ButtonModel> choice = new AtomicReference<ButtonModel>();
    private final List<ButtonModel> buttons = new ArrayList<ButtonModel>(3);
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
        return null;
    }

    public static NotificationModel fromGCMNotificationJSONObject(JSONObject wpData, Bundle extras)
            throws NotTargetedForThisInstallationException
    {
        try {
            // Get the notification type
            Type type = null;
            try {
                type = NotificationModel.Type.fromString(wpData.optString("type", null));
            } catch (Exception ex) {
                WonderPush.logError("Failed to read notification type", ex);
                if (wpData.has("alert") || extras.containsKey("alert")) {
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
            Class<? extends NotificationModel> implementation = type.getImplementation();
            NotificationModel rtn = implementation.getConstructor(String.class).newInstance(wpData.toString());

            // Read common fields
            rtn.setType(type);
            rtn.setTargetedInstallation(wpData.optString("@", null));
            rtn.setCampaignId(wpData.optString("c", null));
            rtn.setNotificationId(wpData.optString("n", null));

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

            // Read common in-app fields
            rtn.setTitle(wpData.optString("title", null));

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
