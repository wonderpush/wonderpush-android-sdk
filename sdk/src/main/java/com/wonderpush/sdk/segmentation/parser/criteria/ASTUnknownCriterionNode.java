package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

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
