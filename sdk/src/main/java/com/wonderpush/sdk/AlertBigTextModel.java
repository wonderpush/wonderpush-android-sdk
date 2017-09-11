package com.wonderpush.sdk;

import org.json.JSONObject;

class AlertBigTextModel extends AlertModel {

    // Modify forCurrentSettings() and clone() when adding a field below
    private CharSequence bigText;
    private CharSequence bigTitle;
    private CharSequence summaryText;
    // Modify forCurrentSettings() and clone() when adding a field above

    public AlertBigTextModel(JSONObject inputJSON) {
        super(inputJSON);
    }

    @Override
    protected void fromJSONCommon(JSONObject wpAlert) {
        super.fromJSONCommon(wpAlert);
        setBigTitle(JSONUtil.getString(wpAlert, "bigTitle"));
        setBigText(JSONUtil.getString(wpAlert, "bigText"));
        setSummaryText(JSONUtil.getString(wpAlert, "summaryText"));
    }

    @Override
    protected void forCurrentSettingsInternal(AlertModel _from) {
        super.forCurrentSettingsInternal(_from);
        if (_from instanceof AlertBigTextModel) {
            AlertBigTextModel from = (AlertBigTextModel) _from;
            if (from.getBigText() != null) {
                setText(from.getBigText());
            }
            if (from.getBigTitle() != null) {
                setTitle(from.getBigTitle());
            }
            if (from.getSummaryText() != null) {
                setSubText(from.getSummaryText());
            }
        }
    }

    public CharSequence getBigText() {
        return bigText;
    }

    public void setBigText(CharSequence bigText) {
        this.bigText = handleHtml(bigText);
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
