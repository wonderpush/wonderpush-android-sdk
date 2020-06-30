package com.wonderpush.sdk.segmentation.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

@RunWith(Parameterized.class)
public class GeohashParseAcceptTest {

    @Parameterized.Parameters(name = "{index}: Test that {0} within ±{1} gives top={2}, right={3}, bottom={4}, left={5}, centerLat={6}, centerLon={7}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"ezs42", 1e-3, 42.627, -5.581, 42.583, -5.625, 42.605, -5.603}, // see: https://en.wikipedia.org/wiki/Geohash#Algorithm_and_example
        });
    }

    private final String geohash;
    private final double delta;
    private final double top;
    private final double right;
    private final double bottom;
    private final double left;
    private final double centerLat;
    private final double centerLon;

    public GeohashParseAcceptTest(String geohash, double delta, double top, double right, double bottom, double left, double centerLat, double centerLon) {
        this.geohash = geohash;
        this.delta = delta;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
    }

    @Test
    public void testItShouldParse() throws BadInputError {
        Geohash parsed = Geohash.parse(geohash);
        assertThat(parsed, instanceOf(Geohash.class));
        assertThat(parsed.geohash, equalTo(geohash));
        assertThat(parsed.top, greaterThan(parsed.bottom));
        assertThat(parsed.right, greaterThan(parsed.left));
        assertThat(parsed.top, closeTo(top, delta));
        assertThat(parsed.right, closeTo(right, delta));
        assertThat(parsed.bottom, closeTo(bottom, delta));
        assertThat(parsed.left, closeTo(left, delta));
        assertThat(parsed.getCenterLat(), lessThan(parsed.top));
        assertThat(parsed.getCenterLat(), greaterThan(parsed.bottom));
        assertThat(parsed.getCenterLat(), closeTo(centerLat, delta));
        assertThat(parsed.getCenterLon(), lessThan(parsed.right));
        assertThat(parsed.getCenterLon(), greaterThan(parsed.left));
        assertThat(parsed.getCenterLon(), closeTo(centerLon, delta));

        GeoLocation topLeft = parsed.getTopLeft();
        assertThat(topLeft, instanceOf(GeoLocation.class));
        assertThat(topLeft.lat, closeTo(top, delta));
        assertThat(topLeft.lon, closeTo(left, delta));
        GeoLocation topRight = parsed.getTopRight();
        assertThat(topRight, instanceOf(GeoLocation.class));
        assertThat(topRight.lat, closeTo(top, delta));
        assertThat(topRight.lon, closeTo(right, delta));
        GeoLocation bottomLeft = parsed.getBottomLeft();
        assertThat(bottomLeft, instanceOf(GeoLocation.class));
        assertThat(bottomLeft.lat, closeTo(bottom, delta));
        assertThat(bottomLeft.lon, closeTo(left, delta));
        GeoLocation bottomRight = parsed.getBottomRight();
        assertThat(bottomRight, instanceOf(GeoLocation.class));
        assertThat(bottomRight.lat, closeTo(bottom, delta));
        assertThat(bottomRight.lon, closeTo(right, delta));

        GeoBox geoBox = parsed.toGeoBox();
        assertThat(geoBox.top, closeTo(top, delta));
        assertThat(geoBox.right, closeTo(right, delta));
        assertThat(geoBox.bottom, closeTo(bottom, delta));
        assertThat(geoBox.left, closeTo(left, delta));
    }

}
