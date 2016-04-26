package com.wonderpush.sdk;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

class AlertModel implements Cloneable {

    private static final String TAG = WonderPush.TAG;

    private static final int MAX_ALLOWED_SOUND_FILESIZE = 1 * 1024 * 1024; // 1 MB

    private static boolean defaultVibrate = true;
    private static boolean defaultSound = true;
    private static boolean defaultLight = true;

    // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
    private static final int defaultNotificationColor;
    private static final int defaultNotificationLedOn;
    private static final int defaultNotificationLedOff;

    static {
        // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
        int _defaultNotificationColor = Color.WHITE;
        try {
            int config_defaultNotificationColor = Resources.getSystem().getIdentifier("config_defaultNotificationColor", "color", "android");
            if (config_defaultNotificationColor != 0)
                _defaultNotificationColor = WonderPushCompatibilityHelper.getColorResource(Resources.getSystem(), config_defaultNotificationColor);
        } catch (Exception ex) {
            WonderPush.logError("Failed to read config_defaultNotificationColor", ex);
        }
        defaultNotificationColor = _defaultNotificationColor;

        // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
        int _defaultNotificationLedOn = 1000;
        try {
            int config_defaultNotificationLedOn = Resources.getSystem().getIdentifier("config_defaultNotificationLedOn", "integer", "android");
            _defaultNotificationLedOn = Resources.getSystem().getInteger(config_defaultNotificationLedOn);
        } catch (Exception ex) {
            WonderPush.logError("Failed to read config_defaultNotificationLedOn", ex);
        }
        defaultNotificationLedOn = _defaultNotificationLedOn;

        // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
        int _defaultNotificationLedOff = 9000;
        try {
            int config_defaultNotificationLedOff = Resources.getSystem().getIdentifier("config_defaultNotificationLedOff", "integer", "android");
            _defaultNotificationLedOff = Resources.getSystem().getInteger(config_defaultNotificationLedOff);
        } catch (Exception ex) {
            WonderPush.logError("Failed to read config_defaultNotificationLedOff", ex);
        }
        defaultNotificationLedOff = _defaultNotificationLedOff;
    }

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
    private Integer color;
    private String group;
    private String sortKey;
    private Boolean localOnly;
    private Integer number;
    private Boolean onlyAlertOnce;
    private Long when;
    private Boolean showWhen;
    private Boolean usesChronometer;
    private Integer visibility;
    private Boolean vibrate;
    private long[] vibratePattern;
    private Boolean lights;
    private Integer lightsColor;
    private Integer lightsOn;
    private Integer lightsOff;
    private Boolean sound;
    private Uri soundUri;
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
        if (wpAlert.isNull("lights")) {
            rtn.setLights(null);
            rtn.setLightsColor((Integer) null);
            rtn.setLightsOn(null);
            rtn.setLightsOff(null);
        } else if (wpAlert.optJSONObject("lights") != null) {
            JSONObject lights = wpAlert.optJSONObject("lights");
            rtn.setLights(null);
            rtn.setLightsColor(lights.optString("color", null));
            if (!(lights.opt("on") instanceof Number)) {
                rtn.setLightsOn(null);
            } else {
                rtn.setLightsOn(lights.optInt("on", defaultNotificationLedOn));
            }
            if (!(lights.opt("off") instanceof Number)) {
                rtn.setLightsOff(null);
            } else {
                rtn.setLightsOff(lights.optInt("off", defaultNotificationLedOff));
            }
        } else {
            rtn.setLights(wpAlert.optBoolean("lights", defaultLight));
            rtn.setLightsColor((Integer) null);
            rtn.setLightsOn(null);
            rtn.setLightsOff(null);
        }
        if (wpAlert.isNull("vibrate")) {
            rtn.setVibrate(null);
            rtn.setVibratePattern(null);
        } else if (wpAlert.optJSONArray("vibrate") != null) {
            rtn.setVibrate(null);
            JSONArray vibrate = wpAlert.optJSONArray("vibrate");
            long[] pattern = new long[vibrate.length()];
            for (int i = 0; i < vibrate.length(); ++i) {
                pattern[i] = vibrate.optLong(i, 0);
            }
            rtn.setVibratePattern(pattern);
        } else {
            rtn.setVibrate(wpAlert.optBoolean("vibrate", defaultVibrate));
            rtn.setVibratePattern(null);
        }
        if (wpAlert.isNull("sound")) {
            rtn.setSound(null);
            rtn.setSoundUri((Uri) null);
        } else if (wpAlert.opt("sound") instanceof String) {
            rtn.setSound(null);
            rtn.setSoundUri(wpAlert.optString("sound", null));
        } else {
            rtn.setSound(wpAlert.optBoolean("sound", defaultSound));
            rtn.setSoundUri((Uri) null);
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
            if (getForeground().hasLights()) {
                rtn.setLights(getForeground().getLights());
            }
            if (getForeground().hasLightsColor()) {
                rtn.setLightsColor(getForeground().getLightsColor());
            }
            if (getForeground().hasLightsOn()) {
                rtn.setLightsOn(getForeground().getLightsOn());
            }
            if (getForeground().hasLightsOff()) {
                rtn.setLightsOff(getForeground().getLightsOff());
            }
            if (getForeground().hasVibrate()) {
                rtn.setVibrate(getForeground().getVibrate());
            }
            if (getForeground().getVibratePattern() != null) {
                rtn.setVibratePattern(getForeground().getVibratePattern());
            }
            if (getForeground().hasSound()) {
                rtn.setSound(getForeground().getSound());
            }
            if (getForeground().getSoundUri() != null) {
                rtn.setSoundUri(getForeground().getSoundUri());
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

    public boolean hasVibrate() {
        return vibrate != null;
    }

    public boolean getVibrate() {
        return vibrate == null ? defaultVibrate : vibrate.booleanValue();
    }

    public void setVibrate(Boolean vibrate) {
        this.vibrate = vibrate;
    }

    public long[] getVibratePattern() {
        return vibratePattern;
    }

    public void setVibratePattern(long[] vibratePattern) {
        this.vibratePattern = vibratePattern;
    }

    public boolean hasLights() {
        return lights != null;
    }

    public boolean getLights() {
        return lights == null ? defaultLight : lights.booleanValue();
    }

    public void setLights(Boolean lights) {
        this.lights = lights;
    }

    public boolean hasLightsColor() {
        return lightsColor != null;
    }

    public int getLightsColor() {
        return lightsColor == null ? defaultNotificationColor : lightsColor.intValue();
    }

    public void setLightsColor(Integer lightsColor) {
        this.lightsColor = lightsColor;
    }

    public void setLightsColor(String lightsColor) {
        try {
            setLightsColor(Color.parseColor(lightsColor));
        } catch (Exception ignored) { // IllegalArgumentException | NullPointerException
            setLightsColor((Integer) null);
        }
    }

    public boolean hasLightsOn() {
        return lightsOn != null;
    }

    public int getLightsOn() {
        return lightsOn == null ? defaultNotificationLedOn : lightsOn.intValue();
    }

    public void setLightsOn(Integer lightsOn) {
        this.lightsOn = lightsOn;
    }

    public boolean hasLightsOff() {
        return lightsOff != null;
    }

    public int getLightsOff() {
        return lightsOff == null ? defaultNotificationLedOff : lightsOff.intValue();
    }

    public void setLightsOff(Integer lightsOff) {
        this.lightsOff = lightsOff;
    }

    public boolean hasSound() {
        return sound != null;
    }

    public boolean getSound() {
        return sound == null ? defaultSound : sound.booleanValue();
    }

    public void setSound(Boolean sound) {
        this.sound = sound;
    }

    public Uri getSoundUri() {
        return soundUri;
    }

    public void setSoundUri(Uri soundUri) {
        if (soundUri != null) {
            String scheme = soundUri.getScheme().toLowerCase(Locale.ROOT);
            // Fetch external file (as the system RingtoneManager has no INTERNET permission, unlike us)
            // and convert it to a data URI
            if ("http".equals(scheme) || "https".equals(scheme)) {
                try {
                    String filename = Integer.toHexString(soundUri.toString().hashCode());
                    File cacheSoundsDir = new File(WonderPush.getApplicationContext().getCacheDir(), "sounds");
                    cacheSoundsDir.mkdirs();
                    File cached = new File(cacheSoundsDir, filename);
                    // TODO handle If-Modified-Since
                    if (!cached.exists()) {
                        WonderPush.logDebug("Sound: Will open URL: " + soundUri);
                        URLConnection conn = new URL(soundUri.toString()).openConnection();
                        InputStream is = (InputStream) conn.getContent();
                        WonderPush.logDebug("Sound: Content-Type: " + conn.getContentType());
                        WonderPush.logDebug("Sound: Content-Length: " + conn.getContentLength() + " bytes");
                        if (conn.getContentLength() > MAX_ALLOWED_SOUND_FILESIZE) {
                            throw new RuntimeException("Sound file too large (" + conn.getContentLength() + " is over " + MAX_ALLOWED_SOUND_FILESIZE + " bytes)");
                        }

                        FileOutputStream outputStream = new FileOutputStream(cached);
                        int read, ttl = 0;
                        byte[] buffer = new byte[2048];
                        while ((read = is.read(buffer)) != -1) {
                            ttl += read;
                            outputStream.write(buffer, 0, read);
                        }
                        outputStream.close();
                        WonderPush.logDebug("Sound: Finished reading " + ttl + " bytes");
                    }
                    soundUri = FileProvider.getUriForFile(
                            WonderPush.getApplicationContext(),
                            WonderPush.getApplicationContext().getPackageName() + ".wonderpush.fileprovider",
                            cached);
                    WonderPush.getApplicationContext().grantUriPermission("com.android.systemui", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    WonderPush.logDebug("Sound: new URI: " + soundUri);
                } catch (Exception ex) {
                    Log.e(WonderPush.TAG, "Failed to fetch sound from URI " + soundUri, ex);
                    setSound(true);
                    setSoundUri((Uri) null);
                    return;
                }
            }
        }
        this.soundUri = soundUri;
    }

    public void setSoundUri(String soundUri) {
        if (soundUri == null) {
            setSoundUri((Uri) null);
        } else {
            // Resolve as a raw resource
            int resId = WonderPush.getApplicationContext().getResources().getIdentifier(soundUri, "raw", WonderPush.getApplicationContext().getPackageName());
            WonderPush.logDebug("Resolving " + soundUri + " as raw resource of " + WonderPush.getApplicationContext().getPackageName() + ": " + resId);
            if (resId != 0) {
                setSoundUri(new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(WonderPush.getApplicationContext().getPackageName())
                        .path(String.valueOf(resId))
                        .build());
            } else {
                // Parse as Uri
                try {
                    setSoundUri(Uri.parse(soundUri));
                } catch (Exception ex) {
                    Log.e(WonderPush.TAG, "Failed to parse sound as uri: " + soundUri);
                    setSoundUri((Uri) null);
                    setSound(defaultSound);
                }
            }
        }
    }

}
