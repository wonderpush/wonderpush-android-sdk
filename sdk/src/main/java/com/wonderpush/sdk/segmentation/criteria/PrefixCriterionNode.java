package com.wonderpush.sdk.segmentation.criteria;

import com.wonderpush.sdk.segmentation.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.ParsingContext;
import com.wonderpush.sdk.segmentation.value.StringValueNode;

public class PrefixCriterionNode extends ASTCriterionNode {

    public final StringValueNode value;

    public PrefixCriterionNode(ParsingContext context, StringValueNode value) {
        super(context);
        this.value = value;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitPrefixCriterionNode(this);
    }

}
