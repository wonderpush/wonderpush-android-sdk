package com.wonderpush.sdk.segmentation;

class PrefixCriterionNode extends ASTCriterionNode {

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
