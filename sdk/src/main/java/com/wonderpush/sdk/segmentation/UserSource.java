package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

class UserSource extends DataSource {

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
