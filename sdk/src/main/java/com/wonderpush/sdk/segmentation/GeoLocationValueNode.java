package com.wonderpush.sdk.segmentation;

class GeoLocationValueNode extends ASTValueNode<GeoLocation> {

    public GeoLocationValueNode(ParsingContext context, GeoLocation value) {
        super(context, value);
    }

    @Override
    <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoLocationValueNode(this);
    }

}
