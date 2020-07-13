package com.wonderpush.sdk.segmentation.parser;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class GeohashTest {

    @Test
    public void testItShouldReject() {
        try {
            Geohash.parse("u09#");
            Assert.fail();
        } catch (BadInputError ex) {
            assertThat(ex, instanceOf(BadInputError.class));
        }
    }

}
