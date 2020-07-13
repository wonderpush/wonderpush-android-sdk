package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.SegmentationDSLError;

public class UnknownCriterionError extends SegmentationDSLError {

    public final ASTUnknownCriterionNode node;

    public UnknownCriterionError(ASTUnknownCriterionNode node) {
        super("Unknown criterion " + node.key);
        this.node = node;
    }

}
