package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class LastActivityDateCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode dateComparison;

    public LastActivityDateCriterionNode(ParsingContext context, ASTCriterionNode dateComparison) {
        super(context);
        this.dateComparison = dateComparison;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitLastActivityDateCriterionNode(this);
    }

}
