package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueNode;
import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.GeoLocation;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class GeoLocationValueNode extends ASTValueNode<GeoLocation> {

    public GeoLocationValueNode(ParsingContext context, GeoLocation value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoLocationValueNode(this);
    }

}
