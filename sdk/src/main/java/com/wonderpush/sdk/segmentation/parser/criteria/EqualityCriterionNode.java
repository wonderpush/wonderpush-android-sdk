package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class EqualityCriterionNode extends ASTCriterionNode {

    public final ASTValueNode<Object> value;

    public EqualityCriterionNode(ParsingContext context, ASTValueNode<Object> value) {
        super(context);
        this.value = value;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitEqualityCriterionNode(this);
    }

}
