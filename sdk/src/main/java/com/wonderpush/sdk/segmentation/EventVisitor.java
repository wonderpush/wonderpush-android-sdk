package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;

import org.json.JSONObject;

import java.util.List;

class EventVisitor extends BaseCriterionVisitor {

    protected final JSONObject event;

    public EventVisitor(Segmenter.Data data, JSONObject event) {
        super(data);
        this.event = event;
    }

    @Override
    public List<Object> visitFieldSource(FieldSource dataSource) {
        return this.visitFieldSourceWithObject(dataSource, this.event);
    }

}
