package com.wonderpush.sdk;

import org.json.JSONObject;

import android.support.v4.app.NotificationCompat;
import android.util.Log;

class AlertModel implements Cloneable {

    private static final String TAG = WonderPush.TAG;

    // Modify forCurrentSettings() when adding a field below
    private String title;
    private String text;
    private String tag;
    private boolean tagPresent;
    private int priority;
    private Boolean autoOpen;
    // Modify forCurrentSettings() when adding a field above
    private AlertModel foreground;

    public static AlertModel fromOldFormatStringExtra(String alert) {
        try {
            AlertModel rtn = new AlertModel();
            rtn.setText(alert);
            return rtn;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while parsing a notification alert with string input " + alert, e);
        }
        return null;
    }

    public static AlertModel fromJSON(JSONObject wpAlert) {
        try {
            AlertModel rtn = new AlertModel();
            fromJSON_toplevel(rtn, wpAlert);
            return rtn;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while parsing a notification alert with JSON input " + wpAlert.toString(), e);
        }
        return null;
    }

    private static void fromJSON_toplevel(AlertModel rtn, JSONObject wpAlert) {
        fromJSON_common(rtn, wpAlert);

        rtn.setPriority(wpAlert.optInt("priority", NotificationCompat.PRIORITY_DEFAULT));

        JSONObject wpAlertForeground = wpAlert.optJSONObject("foreground");
        if (wpAlertForeground == null) {
            wpAlertForeground = new JSONObject();
        }
        AlertModel foreground = new AlertModel();
        fromJSON_foreground(foreground, wpAlertForeground);
        rtn.setForeground(foreground);
    }

    private static void fromJSON_foreground(AlertModel rtn, JSONObject wpAlert) {
        fromJSON_common(rtn, wpAlert);

        rtn.setPriority(wpAlert.optInt("priority", NotificationCompat.PRIORITY_HIGH));
        rtn.setForeground(null);
    }

    private static void fromJSON_common(AlertModel rtn, JSONObject wpAlert) {
        rtn.setTitle(wpAlert.optString("title", null));
        rtn.setText(wpAlert.optString("text", null));
        rtn.setHasTag(wpAlert.has("tag"));
        rtn.setTag(wpAlert.isNull("tag") ? null : wpAlert.optString("tag", null));
        if (wpAlert.has("autoOpen")) {
            rtn.setAutoOpen(wpAlert.optBoolean("autoOpen", false));
        } else {
            rtn.setAutoOpen(null);
        }
    }

    public AlertModel() {
    }

    public AlertModel forCurrentSettings(boolean applicationIsForeground) {
        AlertModel rtn;
        try {
            rtn = (AlertModel) clone();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "Failed to clone an " + this.getClass().getSimpleName(), e);
            return null;
        }

        if (applicationIsForeground && getForeground() != null) {
            if (getForeground().getText() != null) {
                rtn.setText(getForeground().getText());
            }
            if (getForeground().getTitle() != null) {
                rtn.setTitle(getForeground().getTitle());
            }
            rtn.setPriority(getForeground().getPriority());
            if (getForeground().hasAutoOpen()) {
                rtn.setAutoOpen(getForeground().getAutoOpen());
            }
        }

        rtn.setForeground(null);

        return rtn;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        AlertModel rtn = (AlertModel) super.clone();
        rtn.foreground = (AlertModel) foreground.clone();
        return rtn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean hasTag() {
        return tagPresent;
    }

    public void setHasTag(boolean tagPresent) {
        this.tagPresent = tagPresent;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean hasAutoOpen() {
        return autoOpen != null;
    }

    public boolean getAutoOpen() {
        return autoOpen != null && autoOpen.booleanValue();
    }

    public void setAutoOpen(Boolean autoOpen) {
        this.autoOpen = autoOpen;
    }

    public AlertModel getForeground() {
        return foreground;
    }

    public void setForeground(AlertModel foreground) {
        this.foreground = foreground;
    }

}
