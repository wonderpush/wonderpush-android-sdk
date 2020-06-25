package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.GeoCircle;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class GeoCircleValueNode extends GeoAbstractAreaValueNode<GeoCircle> {

    public GeoCircleValueNode(ParsingContext context, GeoCircle value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoCircleValueNode(this);
    }

}
