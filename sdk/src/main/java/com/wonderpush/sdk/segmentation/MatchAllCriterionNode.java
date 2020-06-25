package com.wonderpush.sdk.segmentation;

class MatchAllCriterionNode extends ASTCriterionNode {

    public MatchAllCriterionNode(ParsingContext context) {
        super(context);
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitMatchAllCriterionNode(this);
    }

}
