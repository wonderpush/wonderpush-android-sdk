package com.wonderpush.sdk.segmentation;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ISO8601DurationParseAcceptanceTest {

    private static final double DELTA = Float.MIN_NORMAL;

    @Parameterized.Parameters(name = "{index}: Test that {0} gives positive={1} years={2} months={3} weeks={4} days={5} hours={6} minutes={7} seconds={8}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"P", true, 0, 0, 0, 0, 0, 0, 0},
                {"PT", true, 0, 0, 0, 0, 0, 0, 0},
                {"-P", false, 0, 0, 0, 0, 0, 0, 0},
                {"-PT", false, 0, 0, 0, 0, 0, 0, 0},
                {"P1Y", true, 1, 0, 0, 0, 0, 0, 0},
                {"+P1Y", true, 1, 0, 0, 0, 0, 0, 0},
                {"-P1Y", false, 1, 0, 0, 0, 0, 0, 0},
                {"P1Y2M3W4DT5H6M7S", true, 1, 2, 3, 4, 5, 6, 7},
                {"P1.Y", true, 1, 0, 0, 0, 0, 0, 0},
                {"P0.5Y", true, .5f, 0, 0, 0, 0, 0, 0},
                {"P0,5Y", true, .5f, 0, 0, 0, 0, 0, 0},
                {"P000.5Y", true, .5f, 0, 0, 0, 0, 0, 0},
                {"P000,5Y", true, .5f, 0, 0, 0, 0, 0, 0},
                {"P1.500Y", true, 1.5f, 0, 0, 0, 0, 0, 0},
                {"P1,500Y", true, 1.5f, 0, 0, 0, 0, 0, 0},
        });
    }

    private final String input;
    private final boolean positive;
    private final float years;
    private final float months;
    private final float weeks;
    private final float days;
    private final float hours;
    private final float minutes;
    private final float seconds;

    public ISO8601DurationParseAcceptanceTest(String input, boolean positive, float years, float months, float weeks, float days, float hours, float minutes, float seconds) {
        this.input = input;
        this.positive = positive;
        this.years = years;
        this.months = months;
        this.weeks = weeks;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    @Test
    public void testItShouldParse() {
        ISO8601Duration parsed;
        try {
            parsed = ISO8601Duration.parse(input);
        } catch (BadInputError ex) {
            fail(ex.getMessage());
            return;
        }
        assertThat(parsed.positive, equalTo(positive));
        assertThat((double) parsed.years, Matchers.closeTo(years, DELTA));
        assertThat((double) parsed.months, Matchers.closeTo(months, DELTA));
        assertThat((double) parsed.weeks, Matchers.closeTo(weeks, DELTA));
        assertThat((double) parsed.days, Matchers.closeTo(days, DELTA));
        assertThat((double) parsed.hours, Matchers.closeTo(hours, DELTA));
        assertThat((double) parsed.minutes, Matchers.closeTo(minutes, DELTA));
        assertThat((double) parsed.seconds, Matchers.closeTo(seconds, DELTA));
    }

}
