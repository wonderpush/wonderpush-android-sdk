package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public abstract class ASTCriterionNode {

    public final ParsingContext context;

    public ASTCriterionNode(ParsingContext context) {
        this.context = context;
    }

    public abstract <T> T accept(ASTCriterionVisitor<T> visitor);
}
