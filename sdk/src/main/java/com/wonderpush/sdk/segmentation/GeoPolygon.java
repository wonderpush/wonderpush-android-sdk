package com.wonderpush.sdk.segmentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeoPolygon {

    public final List<GeoLocation> points;

    public GeoPolygon(List<GeoLocation> points) {
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    @Override
    public String toString() {
        return "GeoPolygon{" +
                "points=" + points +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoPolygon that = (GeoPolygon) o;

        return points.equals(that.points);
    }

    @Override
    public int hashCode() {
        return points.hashCode();
    }

}
