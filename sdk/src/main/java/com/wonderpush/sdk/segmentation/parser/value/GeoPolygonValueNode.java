package com.wonderpush.sdk.segmentation.parser.value;

import com.wonderpush.sdk.segmentation.parser.ASTValueVisitor;
import com.wonderpush.sdk.segmentation.parser.GeoPolygon;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class GeoPolygonValueNode extends GeoAbstractAreaValueNode<GeoPolygon> {

    public GeoPolygonValueNode(ParsingContext context, GeoPolygon value) {
        super(context, value);
    }

    @Override
    public <U> U accept(ASTValueVisitor<U> visitor) {
        return visitor.visitGeoPolygonValueNode(this);
    }

}
