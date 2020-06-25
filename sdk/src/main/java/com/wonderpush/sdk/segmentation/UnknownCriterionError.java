package com.wonderpush.sdk.segmentation;

class UnknownCriterionError extends SegmentationDSLError {

    public final ASTUnknownCriterionNode node;

    public UnknownCriterionError(ASTUnknownCriterionNode node) {
        super("Unknown criterion " + node.key);
        this.node = node;
    }

}
