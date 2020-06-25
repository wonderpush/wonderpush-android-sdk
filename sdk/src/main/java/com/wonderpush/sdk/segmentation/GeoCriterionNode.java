package com.wonderpush.sdk.segmentation;

class GeoCriterionNode extends ASTCriterionNode {

    public final ASTCriterionNode locationComparison;
    public final ASTCriterionNode dateComparison;

    public GeoCriterionNode(ParsingContext context, ASTCriterionNode locationComparison, ASTCriterionNode dateComparison) {
        super(context);
        this.locationComparison = locationComparison;
        this.dateComparison = dateComparison;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitGeoCriterionNode(this);
    }

}
