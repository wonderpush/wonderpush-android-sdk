package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ParsingContext;

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
