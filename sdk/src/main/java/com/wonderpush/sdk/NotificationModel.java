package com.wonderpush.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.wonderpush.sdk.inappmessaging.model.Campaign;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class NotificationModel implements Parcelable {

    private static final String TAG = WonderPush.TAG;

    static final long DEFAULT_LAST_RECEIVED_NOTIFICATION_CHECK_DELAY = 7 * 86400 * 1000;

    public static class NotTargetedForThisInstallationException extends Exception {
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

    public static final Creator<NotificationModel> CREATOR = new Creator<NotificationModel>() {
        @Override
        public NotificationModel createFromParcel(Parcel in) {
            String json = in.readString();
            try {
                JSONObject parsed = new JSONObject(json);
                return NotificationModel.fromNotificationJSONObject(parsed);
            } catch (NotTargetedForThisInstallationException e) {
                Log.e(WonderPush.TAG, "Unexpected error: Cannot unparcel notification, not targeted at this installation", e);
            } catch (JSONException e) {
                Log.e(WonderPush.TAG, "Unexpected error: Cannot parse notification " + json, e);
            }
            return null;
        }

        @Override
        public NotificationModel[] newArray(int size) {
            return new NotificationModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(inputJSONString);
    }

    private final String inputJSONString;

    private String targetedInstallation;

    private long lastReceivedNotificationCheckDelay;
    private String campaignId;
    private String notificationId;
    private String viewId;
    private Type type;
    private AlertModel alert;
    private String targetUrl;
    public List<ActionModel> receiveActions = new ArrayList<>();
    public List<ActionModel> actions = new ArrayList<>();
    private boolean receipt;
    private boolean receiptUsingMeasurements;
    private InAppMessage inAppMessage;

    // Common in-app message data
    private final AtomicReference<ButtonModel> choice = new AtomicReference<>();
    private final List<ButtonModel> buttons = new ArrayList<>(3);
    private String title;

    public static NotificationModel fromLocalIntent(Intent intent, Context context) {
        if (NotificationManager.containsExplicitNotification(intent)) {

            String notifString = intent.getData().getQueryParameter(WonderPush.INTENT_NOTIFICATION_QUERY_PARAMETER);
            if (notifString == null) {
                return null;
            }
            try {
                JSONObject notifParsed = new JSONObject(notifString);
                return NotificationModel.fromNotificationJSONObject(notifParsed);
            } catch (JSONException e) {
                WonderPush.logDebug("data is not a well-formed JSON object", e);
            } catch (NotTargetedForThisInstallationException e) {
                WonderPush.logError("Notifications not targeted for this installation should have been filtered earlier", e);
            }

        } else if (NotificationManager.containsWillOpenNotification(intent)) {

            NotificationModel notif = intent.getParcelableExtra(WonderPush.INTENT_NOTIFICATION_WILL_OPEN_EXTRA_NOTIFICATION_MODEL);
            return notif;

        }
        return null;
    }

    public static NotificationModel fromNotificationJSONObject(JSONObject wpData)
            throws NotTargetedForThisInstallationException
    {
        try {
            // Read notification content
            JSONObject wpAlert = wpData.optJSONObject("alert");
            AlertModel alert = null;
            if (wpAlert != null) {
                alert = AlertModel.fromJSON(wpAlert);
            } else {
                // We are probably parsing an opened notification and the extras was not given.
                // We are not interested in showing a notification anyway, the in-app message is what's important now.
            }

            // Get the notification type
            Type type;
            try {
                type = NotificationModel.Type.fromString(JSONUtil.getString(wpData, "type"));
            } catch (Exception ex) {
                WonderPush.logError("Failed to read notification type", ex);
                if (alert != null && (alert.getText() != null || alert.getTitle() != null)) { // must be the same test as NotificationManager.buildNotification
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
            rtn.setAlert(alert);
            rtn.setTargetedInstallation(JSONUtil.getString(wpData, "@"));
            rtn.setCampaignId(JSONUtil.getString(wpData, "c"));
            rtn.setNotificationId(JSONUtil.getString(wpData, "n"));
            rtn.setViewId(JSONUtil.getString(wpData, "v"));
            rtn.setTargetUrl(JSONUtil.getString(wpData, "targetUrl"));
            rtn.setReceipt(wpData.optBoolean("receipt", false));
            rtn.setReceiptUsingMeasurements(wpData.optBoolean("receiptUsingMeasurements", false));
            rtn.setLastReceivedNotificationCheckDelay(wpData.optLong("lastReceivedNotificationCheckDelay", DEFAULT_LAST_RECEIVED_NOTIFICATION_CHECK_DELAY));


            // Read receive actions
            JSONArray receiveActions = wpData.optJSONArray("receiveActions");
            int receiveActionCount = receiveActions != null ? receiveActions.length() : 0;
            for (int i = 0 ; i < receiveActionCount ; ++i) {
                JSONObject action = receiveActions.optJSONObject(i);
                rtn.addReceiveAction(new ActionModel(action));
            }

            // Read at open actions
            JSONArray actions = wpData.optJSONArray("actions");
            int actionCount = actions != null ? actions.length() : 0;
            for (int i = 0 ; i < actionCount ; ++i) {
                JSONObject action = actions.optJSONObject(i);
                rtn.addAction(new ActionModel(action));
            }

            // Read in-app
            JSONObject inApp = wpData.optJSONObject("inApp");
            if (inApp != null) {
                JSONObject reporting = inApp.optJSONObject("reporting");
                JSONObject content = inApp.optJSONObject("content");
                if (reporting != null && content != null) {
                    NotificationMetadata notificationMetadata = new NotificationMetadata(JSONUtil.optString(reporting, "campaignId"), JSONUtil.optString(reporting, "notificationId"), JSONUtil.optString(reporting, "viewId"), false);
                    rtn.inAppMessage = Campaign.parseContent(notificationMetadata, new JSONObject(), content);
                }
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

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
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

    public List<ActionModel> getReceiveActions() {
        return receiveActions;
    }

    public void setReceiveActions(List<ActionModel> receiveActions) {
        this.receiveActions = receiveActions;
    }

    public void addReceiveAction(ActionModel action) {
        if (action != null) {
            receiveActions.add(action);
        }
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

    public boolean getReceiptUsingMeasurements() {
        return receiptUsingMeasurements;
    }

    public void setReceiptUsingMeasurements(boolean receiptUsingMeasurements) {
        this.receiptUsingMeasurements = receiptUsingMeasurements;
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

    public InAppMessage getInAppMessage() {
        return inAppMessage;
    }

    public long getLastReceivedNotificationCheckDelay() {
        return lastReceivedNotificationCheckDelay;
    }

    public void setLastReceivedNotificationCheckDelay(long lastReceivedNotificationCheckDelay) {
        this.lastReceivedNotificationCheckDelay = lastReceivedNotificationCheckDelay;
    }
}
