package com.wonderpush.sdk.segmentation;

class JoinCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode child;

    public JoinCriterionNode(ParsingContext context, ASTCriterionNode child) {
        super(context);
        this.child = child;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitJoinCriterionNode(this);
    }

}
