package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.parser.value.BooleanValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DurationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoBoxValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoCircleValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoLocationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoPolygonValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NullValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NumberValueNode;
import com.wonderpush.sdk.segmentation.parser.value.StringValueNode;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class SegmentationDSLParserParseFieldEqTest {

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    private static Date date(int year, int month, int day, int hour, int minute, int seconds, int millis) {
        Calendar rtn = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        rtn.set(year, month, day, hour, minute, seconds);
        rtn.set(Calendar.MILLISECOND, millis);
        return rtn.getTime();
    }

    @Parameterized.Parameters(name = "{index}: Test that {0} parses with comparator {1} and value {2}")
    public static Iterable<Object[]> data() throws JSONException, BadInputError {
        return Arrays.asList(new Object[][]{
                {
                        new JSONObject("{ \".field\": { \"eq\": null } }"),
                        NullValueNode.class,
                        null,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": true } }"),
                        BooleanValueNode.class,
                        true,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": false } }"),
                        BooleanValueNode.class,
                        false,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": 0 } }"),
                        NumberValueNode.class,
                        0,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": 9223372036854775807 } }"),
                        NumberValueNode.class,
                        Long.MAX_VALUE,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": 3.14 } }"),
                        NumberValueNode.class,
                        3.14,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": \"foo\" } }"),
                        StringValueNode.class,
                        "foo",
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": 1445470140000 } } }"),
                        DateValueNode.class,
                        1445470140000L,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020-02-03T04:05:06.007Z\" } } }"),
                        DateValueNode.class,
                        date(2020, 1, 3, 4, 5, 6, 7).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020-02-03T04:05:06.007\" } } }"),
                        DateValueNode.class,
                        date(2020, 1, 3, 4, 5, 6, 7).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020-02-03T04:05:06\" } } }"),
                        DateValueNode.class,
                        date(2020, 1, 3, 4, 5, 6, 0).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020-02-03T04:05\" } } }"),
                        DateValueNode.class,
                        date(2020, 1, 3, 4, 5, 0, 0).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020-02-03T04\" } } }"),
                        DateValueNode.class,
                        date(2020, 1, 3, 4, 0, 0, 0).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020-02-03\" } } }"),
                        DateValueNode.class,
                        date(2020, 1, 3, 0, 0, 0, 0).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020-02\" } } }"),
                        DateValueNode.class,
                        date(2020, 1, 1, 0, 0, 0, 0).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2020\" } } }"),
                        DateValueNode.class,
                        date(2020, 0, 1, 0, 0, 0, 0).getTime(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"date\": \"2015-10-21T16:29:00-07:00\" } } }"),
                        DateValueNode.class,
                        1445470140000L,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": 42 } } }"),
                        DurationValueNode.class,
                        42,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"P1W2DT3H4M5.6S\" } } }"),
                        DurationValueNode.class,
                        (long) (((((7 * 1 + 2) * 24 + 3) * 60 + 4) * 60 + 5.6) * 1000),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 ns\" } } }"),
                        DurationValueNode.class,
                        0.0000015,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 us\" } } }"),
                        DurationValueNode.class,
                        0.0015,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 ms\" } } }"),
                        DurationValueNode.class,
                        1.5,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 s\" } } }"),
                        DurationValueNode.class,
                        1500.,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 m\" } } }"),
                        DurationValueNode.class,
                        1500. * 60,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 h\" } } }"),
                        DurationValueNode.class,
                        1500. * 60 * 60,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 d\" } } }"),
                        DurationValueNode.class,
                        1500. * 60 * 60 * 24,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"duration\": \"1.5 w\" } } }"),
                        DurationValueNode.class,
                        1500. * 60 * 60 * 24 * 7,
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geolocation\": { \"lat\": 0, \"lon\": 0 } } } }"),
                        GeoLocationValueNode.class,
                        new GeoLocation(0, 0),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geolocation\": \"u09tunq\" } } }"),
                        GeoLocationValueNode.class,
                        Geohash.parse("u09tunq").toGeoLocation(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geobox\": \"u09tunq\" } } }"),
                        GeoBoxValueNode.class,
                        Geohash.parse("u09tunq").toGeoBox(),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geobox\": { \"topLeft\": { \"lat\": 2, \"lon\": 3 }, \"bottomRight\": { \"lat\": 1, \"lon\": 4 } } } } }"),
                        GeoBoxValueNode.class,
                        GeoBox.fromTopRightBottomLeft(2, 4, 1, 3),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geobox\": { \"topRight\": { \"lat\": 2, \"lon\": 4 }, \"bottomLeft\": { \"lat\": 1, \"lon\": 3 } } } } }"),
                        GeoBoxValueNode.class,
                        GeoBox.fromTopRightBottomLeft(2, 4, 1, 3),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geobox\": { \"top\": 2, \"right\": 4, \"bottom\": 1, \"left\": 3 } } } } }"),
                        GeoBoxValueNode.class,
                        GeoBox.fromTopRightBottomLeft(2, 4, 1, 3),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geocircle\": { \"radius\": 1, \"center\": {\"lat\": 2, \"lon\": 3 } } } } }"),
                        GeoCircleValueNode.class,
                        new GeoCircle(new GeoLocation(2, 3), 1),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geocircle\": { \"radius\": 1, \"center\": \"u09tunq\" } } } }"),
                        GeoCircleValueNode.class,
                        new GeoCircle(Geohash.parse("u09tunq").toGeoLocation(), 1),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geopolygon\": [ { \"lat\": 0, \"lon\": 1 }, { \"lat\": 2, \"lon\": 3 }, { \"lat\": 4, \"lon\": 5 } ] } } }"),
                        GeoPolygonValueNode.class,
                        new GeoPolygon(Arrays.asList(new GeoLocation(0, 1), new GeoLocation(2, 3), new GeoLocation(4, 5))),
                },
                {
                        new JSONObject("{ \".field\": { \"eq\": { \"geopolygon\": [ \"t\", \"u\", \"v\" ] } } }"),
                        GeoPolygonValueNode.class,
                        new GeoPolygon(Arrays.asList(Geohash.parse("t").toGeoLocation(), Geohash.parse("u").toGeoLocation(), Geohash.parse("v").toGeoLocation())),
                },
        });
    }

    public final JSONObject input;
    public final Class<ASTValueNode<?>> expectedClass;
    public final Object expectedValue;

    public SegmentationDSLParserParseFieldEqTest(JSONObject input, Class<ASTValueNode<?>> expectedClass, Object expectedValue) {
        this.input = input;
        this.expectedClass = expectedClass;
        this.expectedValue = expectedValue;
    }

    @Test
    public void testItShouldParseProperly() throws BadInputError, UnknownValueError, UnknownCriterionError {
        EqualityCriterionNode checkedAst = (EqualityCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource = (FieldSource) checkedAst.context.dataSource;
        assertThat(checkedDataSource.path.parts.length, is(1));
        assertThat(checkedDataSource.path.parts[0], is("field"));
        assertThat(checkedAst.value, instanceOf(expectedClass));
        assertThat(checkedAst.value.getValue(), is(expectedValue));
    }

}
