package com.wonderpush.sdk.segmentation.datasource;

import android.support.annotation.Nullable;

import com.wonderpush.sdk.segmentation.DataSource;
import com.wonderpush.sdk.segmentation.DataSourceVisitor;

public class LastActivityDateSource extends DataSource {

    public LastActivityDateSource(@Nullable InstallationSource parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return "lastActivityDate";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitLastActivityDateSource(this);
    }

}
