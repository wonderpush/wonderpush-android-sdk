package com.wonderpush.sdk.segmentation.datasource;

import android.support.annotation.Nullable;

import com.wonderpush.sdk.segmentation.DataSource;
import com.wonderpush.sdk.segmentation.DataSourceVisitor;

public class GeoLocationSource extends DataSource {

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
