package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.value.ASTUnknownValueNode;

public class UnknownValueError extends SegmentationDSLError {

    public final ASTUnknownValueNode node;

    public UnknownValueError(ASTUnknownValueNode node) {
        super("Unknown value type " + node.key);
        this.node = node;
    }

}
