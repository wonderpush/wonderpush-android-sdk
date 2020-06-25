package com.wonderpush.sdk.segmentation;

class InstallationSource extends DataSource {

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
