package com.wonderpush.sdk.segmentation.datasource;

import android.support.annotation.Nullable;

import com.wonderpush.sdk.segmentation.DataSource;
import com.wonderpush.sdk.segmentation.DataSourceVisitor;

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
