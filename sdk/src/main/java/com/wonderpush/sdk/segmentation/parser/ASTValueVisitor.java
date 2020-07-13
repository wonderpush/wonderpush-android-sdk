package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.value.ASTUnknownValueNode;
import com.wonderpush.sdk.segmentation.parser.value.BooleanValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DurationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoBoxValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoCircleValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoLocationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoPolygonValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NullValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NumberValueNode;
import com.wonderpush.sdk.segmentation.parser.value.RelativeDateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.StringValueNode;

public interface ASTValueVisitor<T> {

    public T visitASTUnknownValueNode(ASTUnknownValueNode node);

    public T visitDateValueNode(DateValueNode node);

    public T visitDurationValueNode(DurationValueNode node);

    public T visitRelativeDateValueNode(RelativeDateValueNode node);

    public T visitGeoLocationValueNode(GeoLocationValueNode node);

    public T visitGeoBoxValueNode(GeoBoxValueNode node);

    public T visitGeoCircleValueNode(GeoCircleValueNode node);

    public T visitGeoPolygonValueNode(GeoPolygonValueNode node);

    public T visitBooleanValueNode(BooleanValueNode node);

    public T visitNullValueNode(NullValueNode node);

    public T visitNumberValueNode(NumberValueNode node);

    public T visitStringValueNode(StringValueNode node);

}
