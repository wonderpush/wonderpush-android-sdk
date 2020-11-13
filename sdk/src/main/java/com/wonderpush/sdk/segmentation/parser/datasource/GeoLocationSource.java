package com.wonderpush.sdk.segmentation.parser.datasource;

import androidx.annotation.Nullable;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;

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
