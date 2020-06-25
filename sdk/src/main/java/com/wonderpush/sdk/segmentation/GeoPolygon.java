package com.wonderpush.sdk.segmentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeoPolygon {

    public final List<GeoLocation> points;

    public GeoPolygon(List<GeoLocation> points) {
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

}
