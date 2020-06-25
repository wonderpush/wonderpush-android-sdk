package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class NotCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode child;

    public NotCriterionNode(ParsingContext context, ASTCriterionNode child) {
        super(context);
        this.child = child;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitNotCriterionNode(this);
    }

}
