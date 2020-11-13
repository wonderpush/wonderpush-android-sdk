package com.wonderpush.sdk.segmentation.parser.datasource;

import androidx.annotation.Nullable;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;

public class GeoDateSource extends DataSource {

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
