package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class MatchAllCriterionNode extends ASTCriterionNode {

    public MatchAllCriterionNode(ParsingContext context) {
        super(context);
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitMatchAllCriterionNode(this);
    }

}
