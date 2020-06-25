package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.value.ASTUnknownValueNode;

public class UnknownValueError extends SegmentationDSLError {

    public final ASTUnknownValueNode node;

    public UnknownValueError(ASTUnknownValueNode node) {
        super("Unknown value type " + node.key);
        this.node = node;
    }

}
