package com.wonderpush.sdk;

import android.app.Notification;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

class AlertModel implements Cloneable {

    private static final String TAG = WonderPush.TAG;

    // Modify forCurrentSettings() when adding a field below
    private String title;
    private String text;
    private String subText;
    private String info;
    private String ticker;
    private String tag;
    private boolean tagPresent;
    private int priority;
    private Boolean autoOpen;
    private Boolean autoDrop;
    private List<String> persons;
    private String category;
    private Integer color = NotificationCompat.COLOR_DEFAULT;
    private String group;
    private String sortKey;
    private Boolean localOnly;
    private Integer number;
    private Boolean onlyAlertOnce;
    private Long when;
    private Boolean showWhen;
    private Boolean usesChronometer;
    private Integer visibility;
    // Modify forCurrentSettings() when adding a field above
    private AlertModel foreground;

    public static AlertModel fromOldFormatStringExtra(String alert) {
        try {
            JSONObject wpAlert = new JSONObject();
            wpAlert.putOpt("text", alert);
            return fromJSON(wpAlert);
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

        if (wpAlert.isNull("priority")) {
            rtn.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        } else if (wpAlert.opt("priority") instanceof String) {
            rtn.setPriority(wpAlert.optString("priority"));
        } else {
            rtn.setPriority(wpAlert.optInt("priority", NotificationCompat.PRIORITY_DEFAULT));
        }


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

        if (wpAlert.isNull("priority")) {
            rtn.setPriority(NotificationCompat.PRIORITY_HIGH);
        } else if (wpAlert.opt("priority") instanceof String) {
            rtn.setPriority(wpAlert.optString("priority"));
        } else {
            rtn.setPriority(wpAlert.optInt("priority", NotificationCompat.PRIORITY_HIGH));
        }

        rtn.setForeground(null);
    }

    private static void fromJSON_common(AlertModel rtn, JSONObject wpAlert) {
        rtn.setTitle(wpAlert.optString("title", null));
        rtn.setText(wpAlert.optString("text", null));
        rtn.setSubText(wpAlert.optString("subText", null));
        rtn.setInfo(wpAlert.optString("info", null));
        rtn.setTicker(wpAlert.optString("ticker", null));
        rtn.setHasTag(wpAlert.has("tag"));
        rtn.setTag(wpAlert.optString("tag", null));
        if (wpAlert.has("autoOpen")) {
            rtn.setAutoOpen(wpAlert.optBoolean("autoOpen", false));
        } else {
            rtn.setAutoOpen(null);
        }
        if (wpAlert.has("autoDrop")) {
            rtn.setAutoDrop(wpAlert.optBoolean("autoDrop", false));
        } else {
            rtn.setAutoDrop(null);
        }
        rtn.setPersons(wpAlert.optJSONArray("persons"));
        rtn.setCategory(wpAlert.optString("category", null));
        rtn.setColor(wpAlert.optString("color", null));
        rtn.setGroup(wpAlert.optString("group", null));
        rtn.setSortKey(wpAlert.optString("sortKey", null));
        if (wpAlert.has("localOnly")) {
            rtn.setLocalOnly(wpAlert.optBoolean("localOnly", false));
        } else {
            rtn.setLocalOnly(null);
        }
        if (wpAlert.has("number")) {
            rtn.setNumber(wpAlert.optInt("number", 1));
        } else {
            rtn.setNumber(null);
        }
        if (wpAlert.has("onlyAlertOnce")) {
            rtn.setOnlyAlertOnce(wpAlert.optBoolean("onlyAlertOnce", false));
        } else {
            rtn.setOnlyAlertOnce(null);
        }
        if (wpAlert.has("when")) {
            rtn.setWhen(wpAlert.optLong("when", System.currentTimeMillis()));
        } else {
            rtn.setWhen(null);
        }
        if (wpAlert.has("showWhen")) {
            rtn.setShowWhen(wpAlert.optBoolean("showWhen", false));
        } else {
            rtn.setShowWhen(null);
        }
        if (wpAlert.has("usesChronometer")) {
            rtn.setUsesChronometer(wpAlert.optBoolean("usesChronometer", false));
        } else {
            rtn.setUsesChronometer(null);
        }
        if (wpAlert.isNull("visibility")) {
            rtn.setVisibility((Integer) null);
        } else if (wpAlert.opt("visibility") instanceof String) {
            rtn.setVisibility(wpAlert.optString("visibility"));
        } else {
            rtn.setVisibility(wpAlert.optInt("visibility", NotificationCompat.VISIBILITY_PRIVATE));
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
            if (getForeground().getSubText() != null) {
                rtn.setSubText(getForeground().getSubText());
            }
            if (getForeground().getInfo() != null) {
                rtn.setInfo(getForeground().getInfo());
            }
            if (getForeground().getTicker() != null) {
                rtn.setTicker(getForeground().getTicker());
            }
            rtn.setPriority(getForeground().getPriority());
            if (getForeground().hasAutoOpen()) {
                rtn.setAutoOpen(getForeground().getAutoOpen());
            }
            if (getForeground().hasAutoDrop()) {
                rtn.setAutoDrop(getForeground().getAutoDrop());
            }
            if (getForeground().getPersons() != null) {
                rtn.setPersons(getForeground().getPersons());
            }
            if (getForeground().getCategory() != null) {
                rtn.setCategory(getForeground().getCategory());
            }
            if (getForeground().hasColor()) {
                rtn.setColor(getForeground().getColor());
            }
            if (getForeground().getGroup() != null) {
                rtn.setGroup(getForeground().getGroup());
            }
            if (getForeground().getSortKey() != null) {
                rtn.setSortKey(getForeground().getSortKey());
            }
            if (getForeground().hasLocalOnly()) {
                rtn.setLocalOnly(getForeground().getLocalOnly());
            }
            if (getForeground().hasNumber()) {
                rtn.setNumber(getForeground().getNumber());
            }
            if (getForeground().hasOnlyAlertOnce()) {
                rtn.setOnlyAlertOnce(getForeground().getOnlyAlertOnce());
            }
            if (getForeground().hasWhen()) {
                rtn.setWhen(getForeground().getWhen());
            }
            if (getForeground().hasShowWhen()) {
                rtn.setShowWhen(getForeground().getShowWhen());
            }
            if (getForeground().hasUsesChronometer()) {
                rtn.setUsesChronometer(getForeground().getUsesChronometer());
            }
            if (getForeground().hasVisibility()) {
                rtn.setVisibility(getForeground().getVisibility());
            }
        }

        rtn.setForeground(null);

        return rtn;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        AlertModel rtn = (AlertModel) super.clone();
        if (foreground != null) {
            rtn.foreground = (AlertModel) foreground.clone();
        }
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

    public String getSubText() {
        return subText;
    }

    public void setSubText(String subText) {
        this.subText = subText;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
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

    public void setPriority(String priority) {
        if (priority == null) {
            setPriority(NotificationCompat.PRIORITY_DEFAULT);
        } else {
            // Use the value of the field with matching name
            try {
                setPriority(Notification.class.getField("PRIORITY_" + priority.toUpperCase(Locale.ROOT)).getInt(null));
            } catch (Exception ignored) {
            }
        }
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

    public boolean hasAutoDrop() {
        return autoDrop != null;
    }

    public boolean getAutoDrop() {
        return autoDrop != null && autoDrop.booleanValue();
    }

    public void setAutoDrop(Boolean autoDrop) {
        this.autoDrop = autoDrop;
    }

    public AlertModel getForeground() {
        return foreground;
    }

    public void setForeground(AlertModel foreground) {
        this.foreground = foreground;
    }

    public List<String> getPersons() {
        return persons;
    }

    public void setPersons(List<String> persons) {
        this.persons = persons;
    }

    public void setPersons(JSONArray personsJson) {
        if (personsJson != null) {
            List<String> persons = new LinkedList<>();
            for (int i = 0; i < personsJson.length(); ++i) {
                String person = personsJson.optString(i);
                if (person != null) {
                    persons.add(person);
                }
            }
            setPersons(persons);
        }

    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        boolean valid = category == null;
        if (!valid) {
            // Accept the value if it corresponds to one of the category constants' value
            for (Field field : Notification.class.getFields()) {
                try {
                    if (field.getName().startsWith("CATEGORY_") && category.equals(field.get(null))) {
                        // A category field has the provided value, keep it
                        valid = true;
                        break;
                    }
                } catch (Exception ignored) { // IllegalAccessException
                }
            }
        }
        if (!valid) {
            // Use the value of the field with matching name
            try {
                category = (String) Notification.class.getField("CATEGORY_" + category.toUpperCase(Locale.ROOT)).get(null);
                valid = true;
            } catch (Exception ignored) {} // IllegalAccessException | ClassCastException | NullPointerException
        }
        // Valid or not, keep the given value
        this.category = category;
    }

    public boolean hasColor() {
        return color != null;
    }

    public int getColor() {
        return color == null ? Notification.COLOR_DEFAULT : color.intValue();
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColor(String color) {
        try {
            setColor(Color.parseColor(color));
        } catch (Exception ignored) { // IllegalArgumentException | NullPointerException
            setColor(NotificationCompat.COLOR_DEFAULT);
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public boolean hasLocalOnly() {
        return localOnly != null;
    }

    public boolean getLocalOnly() {
        return localOnly != null && localOnly.booleanValue();
    }

    public void setLocalOnly(Boolean localOnly) {
        this.localOnly = localOnly;
    }

    public boolean hasNumber() {
        return number != null;
    }

    public int getNumber() {
        return number == null ? 0 : number.intValue();
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public boolean hasOnlyAlertOnce() {
        return onlyAlertOnce != null;
    }

    public boolean getOnlyAlertOnce() {
        return onlyAlertOnce != null && onlyAlertOnce.booleanValue();
    }

    public void setOnlyAlertOnce(Boolean onlyAlertOnce) {
        this.onlyAlertOnce = onlyAlertOnce;
    }

    public boolean hasWhen() {
        return when != null;
    }

    public long getWhen() {
        return when == null ? System.currentTimeMillis() : when.longValue();
    }

    public void setWhen(Long when) {
        this.when = when;
    }

    public boolean hasShowWhen() {
        return showWhen != null;
    }

    public boolean getShowWhen() {
        return showWhen != null && showWhen.booleanValue();
    }

    public void setShowWhen(Boolean showWhen) {
        this.showWhen = showWhen;
    }

    public boolean hasUsesChronometer() {
        return usesChronometer != null;
    }

    public boolean getUsesChronometer() {
        return usesChronometer != null && usesChronometer.booleanValue();
    }

    public void setUsesChronometer(Boolean usesChronometer) {
        this.usesChronometer = usesChronometer;
    }

    public boolean hasVisibility() {
        return visibility != null;
    }

    public int getVisibility() {
        return visibility == null ? 0 : visibility.intValue();
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public void setVisibility(String visibility) {
        if (visibility == null) {
            setVisibility((Integer) null);
        } else {
            // Use the value of the field with matching name
            try {
                setVisibility(Notification.class.getField("VISIBILITY_" + visibility.toUpperCase(Locale.ROOT)).getInt(null));
            } catch (Exception ignored) {
            }
        }
    }

}
