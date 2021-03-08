package com.wonderpush.sdk;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

class AlertBigPictureModel extends AlertModel {

    // Modify forCurrentSettings() and clone() when adding a field below
    private Bitmap bigLargeIcon;
    private boolean hasBigLargeIcon;
    private Bitmap bigPicture;
    private CharSequence bigTitle;
    private CharSequence summaryText;
    // Modify forCurrentSettings() and clone() when adding a field above

    public AlertBigPictureModel(JSONObject inputJSON) {
        super(inputJSON);
    }

    public boolean hasBigLargeIcon() {
        return hasBigLargeIcon;
    }

    public void setHasBigLargeIcon(boolean value) {
        hasBigLargeIcon = value;
    }

    @Override
    protected void fromJSONCommon(JSONObject wpAlert) {
        super.fromJSONCommon(wpAlert);
        setBigLargeIcon(JSONUtil.getString(wpAlert, "bigLargeIcon"));
        setHasBigLargeIcon(wpAlert.has("bigLargeIcon"));
        setBigPicture(JSONUtil.getString(wpAlert, "bigPicture"));
        setBigTitle(JSONUtil.getString(wpAlert, "bigTitle"));
        setSummaryText(JSONUtil.getString(wpAlert, "summaryText"));
    }

    @Override
    protected void forCurrentSettingsInternal(AlertModel _from) {
        super.forCurrentSettingsInternal(_from);
        if (_from instanceof AlertBigPictureModel) {
            AlertBigPictureModel from = (AlertBigPictureModel) _from;
            if (from.hasBigLargeIcon()) {
                setBigLargeIcon(from.getBigLargeIcon());
                setHasBigLargeIcon(from.hasBigLargeIcon());
            }
            if (from.getBigPicture() != null) {
                setBigPicture(from.getBigPicture());
            }
            if (from.getBigTitle() != null) {
                setTitle(from.getBigTitle());
            }
            if (from.getSummaryText() != null) {
                setSubText(from.getSummaryText());
            }
        }
    }

    @Override
    public AlertModel getAlternativeIfNeeded() {
        // Fallback to BIG_TEXT if we could not fetch the big picture, to avoid a large blank space and a 1-lined text.
        if (getBigPicture() != null) {
            return null;
        }
        Log.d(WonderPush.TAG, "No big picture for a bigPicture notification, falling back to bigText");
        JSONObject inputJson;
        try {
            inputJson = new JSONObject(getInputJson().toString());
            getInputJson().put("type", AlertModel.Type.BIG_TEXT.toString());
        } catch (JSONException ex) {
            Log.e(WonderPush.TAG, "Failed to override notification alert type from bigPicture to bigText", ex);
            return null;
        }
        AlertModel rtn = AlertModel.Type.BIG_TEXT.getBuilder().build(inputJson);
        rtn.setType(AlertModel.Type.BIG_TEXT);
        return rtn;
    }

    public Bitmap getBigLargeIcon() {
        return bigLargeIcon;
    }

    public void setBigLargeIcon(Bitmap bigLargeIcon) {
        this.bigLargeIcon = bigLargeIcon;
        if (bigLargeIcon != null) {
            WonderPush.logDebug("Big large icon: " + bigLargeIcon.getWidth() + "x" + bigLargeIcon.getHeight());
        }
    }

    public void setBigLargeIcon(String bigLargeIcon) {
        setBigLargeIcon(resolveLargeIconFromString(bigLargeIcon, "Big large icon"));
    }

    public Bitmap getBigPicture() {
        return bigPicture;
    }

    public void setBigPicture(Bitmap bigPicture) {
        this.bigPicture = bigPicture;
        if (bigPicture != null) {
            WonderPush.logDebug("Big picture: " + bigPicture.getWidth() + "x" + bigPicture.getHeight());
        }
    }

    public void setBigPicture(String bigPicture) {
        setBigPicture(resolveBigPictureFromString(bigPicture, "Big picture"));
    }

    public CharSequence getBigTitle() {
        return bigTitle;
    }

    public void setBigTitle(CharSequence bigTitle) {
        this.bigTitle = handleHtml(bigTitle);
    }

    public CharSequence getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(CharSequence summaryText) {
        this.summaryText = handleHtml(summaryText);
    }

}
