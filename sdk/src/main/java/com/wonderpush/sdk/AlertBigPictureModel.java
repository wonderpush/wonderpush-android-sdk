package com.wonderpush.sdk;

import android.graphics.Bitmap;

import org.json.JSONObject;

class AlertBigPictureModel extends AlertModel {

    // Modify forCurrentSettings() and clone() when adding a field below
    private Bitmap bigLargeIcon;
    private Bitmap bigPicture;
    private CharSequence bigTitle;
    private CharSequence summaryText;
    // Modify forCurrentSettings() and clone() when adding a field above

    public AlertBigPictureModel() {
    }

    public AlertBigPictureModel(JSONObject inputJSON) {
        super(inputJSON);
    }

    @Override
    protected void fromJSONCommon(JSONObject wpAlert) {
        super.fromJSONCommon(wpAlert);
        setBigLargeIcon(JSONUtil.getString(wpAlert, "bigLargeIcon"));
        setBigPicture(JSONUtil.getString(wpAlert, "bigPicture"));
        setBigTitle(JSONUtil.getString(wpAlert, "bigTitle"));
        setSummaryText(JSONUtil.getString(wpAlert, "summaryText"));
    }

    @Override
    protected void forCurrentSettingsInternal(AlertModel _from) {
        super.forCurrentSettingsInternal(_from);
        if (_from instanceof AlertBigPictureModel) {
            AlertBigPictureModel from = (AlertBigPictureModel) _from;
            if (from.getBigLargeIcon() != null) {
                setBigLargeIcon(from.getBigLargeIcon());
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
