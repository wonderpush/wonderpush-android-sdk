package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

class GeoDateSource extends DataSource {

    public GeoDateSource(@Nullable InstallationSource parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return "geo.date";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitGeoDateSource(this);
    }

}
