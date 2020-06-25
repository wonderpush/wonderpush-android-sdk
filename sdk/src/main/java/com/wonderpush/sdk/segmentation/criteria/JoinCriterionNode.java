package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class JoinCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode child;

    public JoinCriterionNode(ParsingContext context, ASTCriterionNode child) {
        super(context);
        this.child = child;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitJoinCriterionNode(this);
    }

}
