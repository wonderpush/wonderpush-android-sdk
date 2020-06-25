package com.wonderpush.sdk.segmentation;

class GeoPolygonValueNode extends GeoAbstractAreaValueNode<GeoPolygon> {

    public GeoPolygonValueNode(ParsingContext context, GeoPolygon value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoPolygonValueNode(this);
    }

}
