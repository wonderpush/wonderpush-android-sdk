package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.value.ASTUnknownValueNode;
import com.wonderpush.sdk.segmentation.value.BooleanValueNode;
import com.wonderpush.sdk.segmentation.value.DateValueNode;
import com.wonderpush.sdk.segmentation.value.DurationValueNode;
import com.wonderpush.sdk.segmentation.value.GeoBoxValueNode;
import com.wonderpush.sdk.segmentation.value.GeoCircleValueNode;
import com.wonderpush.sdk.segmentation.value.GeoLocationValueNode;
import com.wonderpush.sdk.segmentation.value.GeoPolygonValueNode;
import com.wonderpush.sdk.segmentation.value.NullValueNode;
import com.wonderpush.sdk.segmentation.value.NumberValueNode;
import com.wonderpush.sdk.segmentation.value.RelativeDateValueNode;
import com.wonderpush.sdk.segmentation.value.StringValueNode;

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
