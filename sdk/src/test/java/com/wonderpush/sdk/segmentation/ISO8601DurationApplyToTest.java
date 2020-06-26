package com.wonderpush.sdk.segmentation;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ISO8601DurationApplyToTest {

    private static final double DELTA = Float.MIN_NORMAL;

    private static Date date(int year, int month, int day, int hour, int minute, int seconds, int millis) {
        Calendar rtn = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        rtn.set(year, month, day, hour, minute, seconds);
        rtn.set(Calendar.MILLISECOND, millis);
        return rtn.getTime();
    }
    @Parameterized.Parameters(name = "{index}: Test that {0} applied on {1,date,yyyy-MM-dd'T'HH:mm:ss.SSSZ} gives {2,date,yyyy-MM-dd'T'HH:mm:ss.SSSZ}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new ISO8601Duration(true,  0, 0, 0, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  0,  0,  0, 0)},
                {new ISO8601Duration(true,  1, 0, 0, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2001,  0,  1,  0,  0,  0, 0)},
                {new ISO8601Duration(false, 1, 0, 0, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(1999,  0,  1,  0,  0,  0, 0)},
                {new ISO8601Duration(true,  0, 1, 0, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  1,  1,  0,  0,  0, 0)},
                {new ISO8601Duration(false, 0, 1, 0, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(1999, 11,  1,  0,  0,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 1, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  8,  0,  0,  0, 0)},
                {new ISO8601Duration(false, 0, 0, 1, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(1999, 11, 25,  0,  0,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 1, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  2,  0,  0,  0, 0)},
                {new ISO8601Duration(false, 0, 0, 0, 1, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(1999, 11, 31,  0,  0,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 0, 1, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  1,  0,  0, 0)},
                {new ISO8601Duration(false, 0, 0, 0, 0, 1, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(1999, 11, 31, 23,  0,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 0, 0, 1, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  0,  1,  0, 0)},
                {new ISO8601Duration(false, 0, 0, 0, 0, 0, 1, 0), date(2000, 0, 1, 0, 0, 0, 0), date(1999, 11, 31, 23, 59,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 0, 0, 0, 1), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  0,  0,  1, 0)},
                {new ISO8601Duration(false, 0, 0, 0, 0, 0, 0, 1), date(2000, 0, 1, 0, 0, 0, 0), date(1999, 11, 31, 23, 59, 59, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 0, 0, 0, .001f), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  0,  0,  0,   1)},
                {new ISO8601Duration(false, 0, 0, 0, 0, 0, 0, .001f), date(2000, 0, 1, 0, 0, 0, 0), date(1999, 11, 31, 23, 59, 59, 999)},
                // Test non-integer values
                {new ISO8601Duration(true,  .5f, 0, 0, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  6,  1,  0,  0,  0, 0)},
                {new ISO8601Duration(true,  0, .5f, 0, 0, 0, 0, 0), date(2001, 1, 1, 0, 0, 0, 0), date(2001,  1, 15,  0,  0,  0, 0)}, // half a month of February on non leap years is half of 28 days: 14 days
                {new ISO8601Duration(true,  0, .5f, 0, 0, 0, 0, 0), date(2001, 3, 1, 0, 0, 0, 0), date(2001,  3, 16,  0,  0,  0, 0)}, // half a month of April is half of 30 days: 15 days
                {new ISO8601Duration(true,  0, .5f, 0, 0, 0, 0, 0), date(2000, 1, 1, 0, 0, 0, 0), date(2000,  1, 15, 12,  0,  0, 0)}, // half a month of February on leap years is half of 29 days: 14 days + 12 hours
                {new ISO8601Duration(true,  0, 0, 1/7f, 0, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  2,  0,  0,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 0, .5f, 0, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1, 12,  0,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 0, .5f, 0, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  0, 30,  0, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 0, 0, .5f, 0), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  0,  0, 30, 0)},
                {new ISO8601Duration(true,  0, 0, 0, 0, 0, 0, .5f), date(2000, 0, 1, 0, 0, 0, 0), date(2000,  0,  1,  0,  0,  0, 500)},
        });
    }

    public final ISO8601Duration duration;
    public final Date fromDate;
    public final Date expectedDate;

    public ISO8601DurationApplyToTest(ISO8601Duration duration, Date fromDate, Date expectedDate) {
        this.duration = duration;
        this.fromDate = fromDate;
        this.expectedDate = expectedDate;
    }

    @Test
    public void testItShouldApply() {
        assertThat(new Date(duration.applyTo(fromDate.getTime())), equalTo(expectedDate));
    }

}
