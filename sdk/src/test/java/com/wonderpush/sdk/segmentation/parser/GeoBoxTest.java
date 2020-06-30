package com.wonderpush.sdk.segmentation.parser;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class GeoBoxTest {
    
    private static final double top = 1;
    private static final double right = 2;
    private static final double bottom = 3;
    private static final double left = 4;
    private static final GeoLocation topRight = new GeoLocation(top, right);
    private static final GeoLocation topLeft = new GeoLocation(top, left);
    private static final GeoLocation bottomRight = new GeoLocation(bottom, right);
    private static final GeoLocation bottomLeft = new GeoLocation(bottom, left);

    @Test
    public void testItShouldConstructFromTopRightBottomLeftProperly() {
        GeoBox instance = GeoBox.fromTopRightBottomLeft(top, right, bottom, left);
        MatcherAssert.assertThat(instance.top, equalTo(top));
        MatcherAssert.assertThat(instance.right, equalTo(right));
        MatcherAssert.assertThat(instance.bottom, equalTo(bottom));
        MatcherAssert.assertThat(instance.left, equalTo(left));
    }

    @Test
    public void testItShouldConstructFromTopRightAndBottomLeftProperly() {
        GeoBox instance = GeoBox.fromTopRightAndBottomLeft(topRight, bottomLeft);
        MatcherAssert.assertThat(instance.top, equalTo(top));
        MatcherAssert.assertThat(instance.right, equalTo(right));
        MatcherAssert.assertThat(instance.bottom, equalTo(bottom));
        MatcherAssert.assertThat(instance.left, equalTo(left));
    }

    @Test
    public void testItShouldConstructFromTopLeftAndBottomRightProperly() {
        GeoBox instance = GeoBox.fromTopLeftAndBottomRight(topLeft, bottomRight);
        MatcherAssert.assertThat(instance.top, equalTo(top));
        MatcherAssert.assertThat(instance.right, equalTo(right));
        MatcherAssert.assertThat(instance.bottom, equalTo(bottom));
        MatcherAssert.assertThat(instance.left, equalTo(left));
    }

}
