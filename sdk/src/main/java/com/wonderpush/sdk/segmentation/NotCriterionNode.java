package com.wonderpush.sdk.segmentation;

class NotCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode child;

    public NotCriterionNode(ParsingContext context, ASTCriterionNode child) {
        super(context);
        this.child = child;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitNotCriterionNode(this);
    }

}
