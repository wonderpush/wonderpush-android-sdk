package com.wonderpush.sdk.segmentation;

class GeoCircleValueNode extends GeoAbstractAreaValueNode<GeoCircle> {

    public GeoCircleValueNode(ParsingContext context, GeoCircle value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoCircleValueNode(this);
    }

}
