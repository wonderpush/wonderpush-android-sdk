package com.wonderpush.sdk.segmentation.datasource;

import com.wonderpush.sdk.segmentation.DataSource;
import com.wonderpush.sdk.segmentation.DataSourceVisitor;

public class InstallationSource extends DataSource {

    public InstallationSource() {
        super(null);
    }

    public String getName() {
        return "installation";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitInstallationSource(this);
    }

}
