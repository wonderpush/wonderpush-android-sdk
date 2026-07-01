package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;

import org.json.JSONObject;

import java.util.List;

/**
 * Resolves segmentation field criteria against the synced sdk-sync {@code contact} object.
 * Mirrors {@link InstallationVisitor}; falls back to an empty object when no contact is synced.
 */
class ContactVisitor extends BaseCriterionVisitor {

    public ContactVisitor(Segmenter.Data data) {
        super(data);
    }

    @Override
    public List<Object> visitFieldSource(FieldSource dataSource) {
        JSONObject contact = this.data.contact != null ? this.data.contact : new JSONObject();
        return this.visitFieldSourceWithObject(dataSource, contact);
    }

}
