package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.GeoBox;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class GeoBoxValueNode extends GeoAbstractAreaValueNode<GeoBox> {

    public GeoBoxValueNode(ParsingContext context, GeoBox value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoBoxValueNode(this);
    }

}
