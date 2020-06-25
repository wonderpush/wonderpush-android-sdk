package com.wonderpush.sdk.segmentation;

class EventSource extends DataSource {

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
