package com.wonderpush.sdk.segmentation.parser.datasource;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;

public class UserSource extends DataSource {

    public UserSource() {
        super(null);
    }

    public String getName() {
        return "user";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitUserSource(this);
    }

}
