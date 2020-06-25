package com.wonderpush.sdk.segmentation;

class LastActivityDateCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode dateComparison;

    public LastActivityDateCriterionNode(ParsingContext context, ASTCriterionNode dateComparison) {
        super(context);
        this.dateComparison = dateComparison;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitLastActivityDateCriterionNode(this);
    }

}
