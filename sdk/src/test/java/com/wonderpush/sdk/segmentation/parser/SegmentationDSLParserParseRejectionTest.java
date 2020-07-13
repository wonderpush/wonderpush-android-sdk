package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SegmentationDSLParserParseRejectionTest {

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    @Parameterized.Parameters(name = "{index}: Test that {0} fails to parse with error {1}")
    public static Iterable<Object[]> data() throws JSONException {
        return Arrays.asList(new Object[][]{
                {
                        new JSONObject("{ \"\": \"\" }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"eq\": 0 }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"any\": [] }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"all\": [] }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"prefix\": \"a\" }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"inside\": { \"geobox\": \"u\" } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": {} }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"\": \"\" } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"prefix\": 0 } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": [] } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": {} } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"someType1\": 0, \"someType2\": 0 } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"date\": false } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"date\": \"invalid\" } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"duration\": false } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"duration\": \"42 towels\" } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"duration\": \"P nope\" } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"duration\": \"nope\" } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"geolocation\": false } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"date\": 0 } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geobox\": 0 } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geobox\": {} } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geobox\": { \"foo\": 0 } } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geocircle\": 0 } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geocircle\": {} } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geopolygon\": { \"radius\": true } } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geopolygon\": { \"radius\": 0, \"foo\": 0 } } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geopolygon\": { \"radius\": 0, \"lat\": true, \"lon\": 0 } } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geopolygon\": { \"radius\": 0, \"lat\": 0, \"lon\": true } } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"inside\": { \"geopolygon\": { \"radius\": 0, \"center\": true } } } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"all\": {} } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"any\": {} } }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"and\": {} }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"and\": 0 }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"and\": [ null ] }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"and\": [ 0 ] }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"and\": [ false ] }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"and\": [ \"\" ] }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"and\": [ [] ] }"),
                        BadInputError.class,
                },
                {
                        new JSONObject("{ \"unknown criterion\": 42 }"),
                        UnknownCriterionError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"unknown criterion\": 42 } }"),
                        UnknownCriterionError.class,
                },
                {
                        new JSONObject("{ \".field\": { \"gt\": { \"unknown type\": 42 } } }"),
                        UnknownValueError.class,
                },
                {
                        // Only available with a field data source
                        new JSONObject("{ \"gt\": 0 }"),
                        BadInputError.class,
                },
                {
                        // Only available directly under an installation data source
                        new JSONObject("{ \".field\": { \"not\": { \"lastActivityDate\": { \"gte\": 0 } } } }"),
                        BadInputError.class,
                },
                {
                        // Only available directly under an installation data source
                        new JSONObject("{ \".field\": { \"not\": { \"presence\": { \"present\": true } } } }"),
                        BadInputError.class,
                },
                {
                        // Only available directly under an installation data source
                        new JSONObject("{ \".field\": { \"not\": { \"geo\": { } } } }"),
                        BadInputError.class,
                },
                {
                        // Missing `present`
                        new JSONObject("{ \"presence\": {} }"),
                        BadInputError.class,
                },
        });
    }

    public final JSONObject input;
    public final Class<? extends SegmentationDSLError> exception;

    public SegmentationDSLParserParseRejectionTest(JSONObject input, Class<? extends SegmentationDSLError> exception) {
        this.input = input;
        this.exception = exception;
    }

    @Test
    public void testItShouldFailOnBadInput() {
        try {
            parser.parse(input, new InstallationSource());
            fail("expected an error of class " + exception.getSimpleName());
        } catch (Exception ex) {
            assertThat(ex, instanceOf(exception));
        }
    }

}
