package com.wonderpush.sdk.segmentation;

public class FieldPath {

    public final String[] parts;

    public FieldPath(String[] parts) {
        this.parts = parts;
    }

    public static FieldPath parse(String dottedPath) {
        return new FieldPath(dottedPath.split("\\.", -1));
    }

}
