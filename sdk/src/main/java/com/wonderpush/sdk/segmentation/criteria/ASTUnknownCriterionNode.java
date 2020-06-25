package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class ASTUnknownCriterionNode extends ASTCriterionNode {

    public final String key;
    public final Object value;

    public ASTUnknownCriterionNode(ParsingContext context, String key, Object value) {
        super(context);
        this.key = key;
        this.value = value;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitASTUnknownCriterionNode(this);
    }

}
