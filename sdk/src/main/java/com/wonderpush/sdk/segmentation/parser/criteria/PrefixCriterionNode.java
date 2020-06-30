package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;
import com.wonderpush.sdk.segmentation.parser.value.StringValueNode;

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
