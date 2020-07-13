package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;

import java.util.List;

class InstallationVisitor extends BaseCriterionVisitor {

    public InstallationVisitor(Segmenter.Data data) {
        super(data);
    }

    @Override
    public List<Object> visitFieldSource(FieldSource dataSource) {
        return this.visitFieldSourceWithObject(dataSource, this.data.installation);
    }

}
