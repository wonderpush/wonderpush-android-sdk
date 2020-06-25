package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

class LastActivityDateSource extends DataSource {

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
