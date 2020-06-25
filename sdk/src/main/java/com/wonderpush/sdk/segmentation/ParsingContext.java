package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

class ParsingContext {

    public final SegmentationDSLParser parser;
    @Nullable
    public final ParsingContext parentContext;
    public final DataSource dataSource;

    public ParsingContext(SegmentationDSLParser parser, @Nullable ParsingContext parentContext, DataSource dataSource) {
        this.parser = parser;
        this.parentContext = parentContext;
        this.dataSource = dataSource;
    }

    public ParsingContext withDataSource(DataSource dataSource) {
        return new ParsingContext(this.parser, this, dataSource);
    }

}
