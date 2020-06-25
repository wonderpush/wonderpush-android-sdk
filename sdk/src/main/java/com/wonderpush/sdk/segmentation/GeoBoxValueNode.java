package com.wonderpush.sdk.segmentation;

class GeoBoxValueNode extends GeoAbstractAreaValueNode<GeoBox> {

    public GeoBoxValueNode(ParsingContext context, GeoBox value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoBoxValueNode(this);
    }

}
