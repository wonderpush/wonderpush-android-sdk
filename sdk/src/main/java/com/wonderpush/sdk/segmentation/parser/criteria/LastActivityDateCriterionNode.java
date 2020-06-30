package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
