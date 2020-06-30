package com.wonderpush.sdk.segmentation.parser.datasource;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;

public class EventSource extends DataSource {

    public EventSource() {
        super(null);
    }

    public String getName() {
        return "event";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitEventSource(this);
    }

}
