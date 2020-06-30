package com.wonderpush.sdk.segmentation.parser;

public class GeoLocation {

    public final double lat;
    public final double lon;

    public GeoLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "GeoLocation{" +
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoLocation that = (GeoLocation) o;

        if (Double.compare(that.lat, lat) != 0) return false;
        return Double.compare(that.lon, lon) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
