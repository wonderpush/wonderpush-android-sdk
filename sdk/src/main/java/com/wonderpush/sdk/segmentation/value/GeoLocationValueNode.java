package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueNode;
import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.GeoLocation;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class GeoLocationValueNode extends ASTValueNode<GeoLocation> {

    public GeoLocationValueNode(ParsingContext context, GeoLocation value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoLocationValueNode(this);
    }

}
