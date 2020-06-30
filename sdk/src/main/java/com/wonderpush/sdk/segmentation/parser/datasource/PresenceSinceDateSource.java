package com.wonderpush.sdk.segmentation.parser.datasource;

import android.support.annotation.Nullable;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;

public class PresenceSinceDateSource extends DataSource {

    public final boolean present;

    public PresenceSinceDateSource(@Nullable InstallationSource parent, boolean present) {
        super(parent);
        this.present = present;
    }

    @Override
    public String getName() {
        return "presence.sinceDate";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitPresenceSinceDateSource(this);
    }

}
