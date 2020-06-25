package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;

class GeoLocationSource extends DataSource {

    public GeoLocationSource(@Nullable InstallationSource parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return "geo.location";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitGeoLocationSource(this);
    }

}
