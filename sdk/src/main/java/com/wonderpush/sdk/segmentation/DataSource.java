package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

abstract class DataSource {

    @Nullable
    public final DataSource parent;

    public DataSource(@Nullable DataSource parent) {
        this.parent = parent;
    }

    abstract public String getName();

    public DataSource getRootDataSource() {
        if (this.parent == null) {
            return this;
        }
        return this.parent.getRootDataSource();
    }

    abstract public <T> T accept(DataSourceVisitor<T> visitor);
}
