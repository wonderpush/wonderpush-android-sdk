package com.wonderpush.sdk.segmentation.parser.datasource;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;

/**
 * Data source for the sdk-sync {@code contact} object, so popup segments can match Brevo contact
 * attributes. Mirrors {@link InstallationSource}; the synced contact is supplied via the segmenter
 * data and resolved by the contact visitor.
 */
public class ContactSource extends DataSource {

    public ContactSource() {
        super(null);
    }

    public String getName() {
        return "contact";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitContactSource(this);
    }

}
