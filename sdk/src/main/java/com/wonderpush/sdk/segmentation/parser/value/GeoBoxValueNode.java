package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.GeoBox;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class GeoBoxValueNode extends GeoAbstractAreaValueNode<GeoBox> {

    public GeoBoxValueNode(ParsingContext context, GeoBox value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoBoxValueNode(this);
    }

}
