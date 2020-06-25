package com.wonderpush.sdk.segmentation;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.LinkedList;

class FieldSource extends DataSource {

    public final FieldPath path;

    public FieldSource(@Nullable DataSource parent, FieldPath path) {
        super(parent);
        this.path = path;
    }

    @Override
    public String getName() {
        return this.parent.getName() + "." + TextUtils.join(".", this.path.parts);
    }

    public FieldPath fullPath() {
        DataSource currentDataSource = this;
        LinkedList<String> parts = new LinkedList<>();
        while (currentDataSource != null) {
            if (currentDataSource instanceof FieldSource) {
                for (String part : ((FieldSource) currentDataSource).path.parts) {
                    parts.addFirst(part);
                }
            }
            currentDataSource = currentDataSource.parent;
        }
        return new FieldPath(parts.toArray(new String[0]));
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitFieldSource(this);
    }
}
