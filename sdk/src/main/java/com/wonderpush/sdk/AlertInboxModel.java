package com.wonderpush.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class AlertInboxModel extends AlertModel {

    // Modify forCurrentSettings() and clone() when adding a field below
    private List<CharSequence> lines;
    private CharSequence bigTitle;
    private CharSequence summaryText;
    // Modify forCurrentSettings() and clone() when adding a field above

    public AlertInboxModel() {
    }

    public AlertInboxModel(JSONObject inputJSON) {
        super(inputJSON);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        AlertInboxModel rtn = (AlertInboxModel) super.clone();
        if (lines != null) {
            rtn.lines = new LinkedList<>();
            for (CharSequence line : lines) {
                rtn.lines.add(line);
            }
        }
        return rtn;
    }

    @Override
    protected void fromJSONCommon(JSONObject wpAlert) {
        super.fromJSONCommon(wpAlert);
        setLines(wpAlert.optJSONArray("lines"));
        setBigTitle(wpAlert.optString("bigTitle", null));
        setSummaryText(wpAlert.optString("summaryText", null));
    }

    @Override
    protected void forCurrentSettingsInternal(AlertModel _from) {
        super.forCurrentSettingsInternal(_from);
        if (_from instanceof AlertInboxModel) {
            AlertInboxModel from = (AlertInboxModel) _from;
            if (from.getLines() != null) {
                setLines(from.getLines());
            }
            if (from.getBigTitle() != null) {
                setTitle(from.getBigTitle());
            }
            if (from.getSummaryText() != null) {
                setSubText(from.getSummaryText());
            }
        }
    }

    public List<CharSequence> getLines() {
        return lines;
    }

    public void setLines(List<CharSequence> lines) {
        this.lines = lines;
    }

    public void setLines(JSONArray linesJson) {
        if (linesJson == null) {
            setLines((List<CharSequence>) null);
        } else {
            List<CharSequence> lines = new LinkedList<>();
            for (int i = 0; i < linesJson.length(); ++i) {
                lines.add(handleHtml(linesJson.optString(i, null)));
            }
            setLines(lines);
        }
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
