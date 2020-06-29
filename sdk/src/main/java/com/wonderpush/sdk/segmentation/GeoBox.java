package com.wonderpush.sdk.segmentation;

public class GeoBox {

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

    @Override
    public String toString() {
        return "GeoBox{" +
                "top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", left=" + left +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoBox geoBox = (GeoBox) o;

        if (Double.compare(geoBox.top, top) != 0) return false;
        if (Double.compare(geoBox.right, right) != 0) return false;
        if (Double.compare(geoBox.bottom, bottom) != 0) return false;
        return Double.compare(geoBox.left, left) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(top);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(right);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(bottom);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(left);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
