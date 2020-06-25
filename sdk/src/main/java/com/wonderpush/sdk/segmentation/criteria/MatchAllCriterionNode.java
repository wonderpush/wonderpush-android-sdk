package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class MatchAllCriterionNode extends ASTCriterionNode {

    public MatchAllCriterionNode(ParsingContext context) {
        super(context);
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitMatchAllCriterionNode(this);
    }

}
