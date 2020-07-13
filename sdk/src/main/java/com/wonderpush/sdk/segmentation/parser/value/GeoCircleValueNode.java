package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.GeoCircle;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class GeoCircleValueNode extends GeoAbstractAreaValueNode<GeoCircle> {

    public GeoCircleValueNode(ParsingContext context, GeoCircle value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoCircleValueNode(this);
    }

}
