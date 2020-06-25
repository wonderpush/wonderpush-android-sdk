package com.wonderpush.sdk.segmentation;

interface ASTValueVisitor<T> {

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
