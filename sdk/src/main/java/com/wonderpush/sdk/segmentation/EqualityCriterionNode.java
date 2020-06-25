package com.wonderpush.sdk.segmentation;

class EqualityCriterionNode extends ASTCriterionNode {

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
