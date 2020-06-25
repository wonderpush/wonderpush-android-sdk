package com.wonderpush.sdk.segmentation;

class GeoBox {

    public final double top;
    public final double right;
    public final double bottom;
    public final double left;

    public GeoBox(double top, double right, double bottom, double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public static GeoBox fromTopRightBottomLeft(double top, double right, double bottom, double left) {
        return new GeoBox(top, right, bottom, left);
    }

    public static GeoBox fromTopRightAndBottomLeft(GeoLocation topRight, GeoLocation bottomLeft) {
        return new GeoBox(topRight.lat, topRight.lon, bottomLeft.lat, bottomLeft.lon);
    }

    public static GeoBox fromTopLeftAndBottomRight(GeoLocation topLeft, GeoLocation bottomRight) {
        return new GeoBox(topLeft.lat, bottomRight.lon, bottomRight.lat, topLeft.lon);
    }

}
