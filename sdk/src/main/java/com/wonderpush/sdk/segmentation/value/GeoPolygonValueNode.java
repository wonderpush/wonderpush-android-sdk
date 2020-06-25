package com.wonderpush.sdk.segmentation.value;

import com.wonderpush.sdk.segmentation.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.GeoPolygon;
import com.wonderpush.sdk.segmentation.ParsingContext;

public class GeoPolygonValueNode extends GeoAbstractAreaValueNode<GeoPolygon> {

    public GeoPolygonValueNode(ParsingContext context, GeoPolygon value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoPolygonValueNode(this);
    }

}
