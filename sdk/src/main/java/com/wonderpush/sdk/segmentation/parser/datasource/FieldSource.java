package com.wonderpush.sdk.segmentation.parser.datasource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;
import com.wonderpush.sdk.segmentation.parser.FieldPath;

import java.util.LinkedList;

public class FieldSource extends DataSource {

    public final FieldPath path;

    public FieldSource(@Nullable DataSource parent, FieldPath path) {
        super(parent);
        this.path = path;
    }

    @Override
    public String getName() {
        return this.parent.getName() + "." + join(".", this.path.parts);
    }

    /**
     * NOTE: Copied from TextUtils.join to avoid depending on an Android API during unit testing.
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param delimiter a CharSequence that will be inserted between the tokens. If null, the string
     *     "null" will be used as the delimiter.
     * @param tokens an array objects to be joined. Strings will be formed from the objects by
     *     calling object.toString(). If tokens is null, a NullPointerException will be thrown. If
     *     tokens is an empty array, an empty string will be returned.
     */
    public static String join(@NonNull CharSequence delimiter, @NonNull Object[] tokens) {
        final int length = tokens.length;
        if (length == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(tokens[0]);
        for (int i = 1; i < length; i++) {
            sb.append(delimiter);
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    public FieldPath fullPath() {
        DataSource currentDataSource = this;
        LinkedList<String> parts = new LinkedList<>();
        while (currentDataSource != null) {
            if (currentDataSource instanceof FieldSource) {
                String[] currParts = ((FieldSource) currentDataSource).path.parts;
                for (int i = currParts.length - 1; i >= 0; i--) {
                    parts.addFirst(currParts[i]);
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
