package com.wonderpush.sdk.segmentation;

class InsideCriterionNode extends ASTCriterionNode {

    public final GeoAbstractAreaValueNode value;

    public InsideCriterionNode(ParsingContext context, GeoAbstractAreaValueNode value) {
        super(context);
        this.value = value;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitInsideCriterionNode(this);
    }

}
