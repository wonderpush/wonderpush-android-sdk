package com.wonderpush.sdk.segmentation.parser;

public abstract class ASTCriterionNode {

    public final ParsingContext context;

    public ASTCriterionNode(ParsingContext context) {
        this.context = context;
    }

    public abstract <T> T accept(ASTCriterionVisitor<T> visitor);
}
