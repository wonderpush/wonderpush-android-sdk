package com.wonderpush.sdk.segmentation;

public class GeoCircle {

    public final GeoLocation center;
    public final double radiusMeters;

    public GeoCircle(GeoLocation center, double radiusMeters) {
        this.center = center;
        this.radiusMeters = radiusMeters;
    }

    @Override
    public String toString() {
        return "GeoCircle{" +
                "center=" + center +
                ", radiusMeters=" + radiusMeters +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoCircle geoCircle = (GeoCircle) o;

        if (Double.compare(geoCircle.radiusMeters, radiusMeters) != 0) return false;
        return center != null ? center.equals(geoCircle.center) : geoCircle.center == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = center != null ? center.hashCode() : 0;
        temp = Double.doubleToLongBits(radiusMeters);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
