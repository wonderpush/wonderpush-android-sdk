package com.wonderpush.sdk.segmentation.datasource;

import com.wonderpush.sdk.segmentation.DataSource;
import com.wonderpush.sdk.segmentation.DataSourceVisitor;

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
