package com.wonderpush.sdk.segmentation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ISO8601DurationParseRejectTest {

    @Parameterized.Parameters(name = "{index}: Test that {0} throws a BadInputError")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {""},
                {" "},
                {" P1Y"},
                {"P1Y "},
                {"P1H"},
                {"P1S"},
                {"P.5Y"},
                {"P,5Y"},
        });
    }

    private String input;

    public ISO8601DurationParseRejectTest(String input) {
        this.input = input;
    }

    @Test
    public void testItShouldReject() {
        try {
            ISO8601Duration.parse(input);
            fail();
        } catch (BadInputError ex) {
            assertThat(ex, instanceOf(BadInputError.class));
        }
    }

}
