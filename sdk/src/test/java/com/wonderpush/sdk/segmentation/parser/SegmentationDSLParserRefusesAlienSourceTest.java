package com.wonderpush.sdk.segmentation.parser;

import androidx.annotation.Nullable;

import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SegmentationDSLParserRefusesAlienSourceTest {

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    private final class AlienDataSource extends DataSource {

        public AlienDataSource(@Nullable DataSource parent) {
            super(parent);
        }

        @Override
        public String getName() {
            return "alien";
        }

        @Override
        public <T> T accept(DataSourceVisitor<T> visitor) {
            return null;
        }

    }

    @Parameterized.Parameters(name = "{index}: Test that {0} from alis dataSource throws")
    public static Iterable<Object[]> data() throws JSONException {
        return Arrays.asList(new Object[][]{
                {
                        new JSONObject("{ \"user\": {} }"),
                },
                {
                        new JSONObject("{ \"installation\": {} }"),
                },
                {
                        new JSONObject("{ \"event\": {} }"),
                },
                {
                        new JSONObject("{ \"lastActivityDate\": { \"gte\": 0 } }"),
                },
                {
                        new JSONObject("{ \"presence\": {} }"),
                },
                {
                        new JSONObject("{ \"geo\": {} }"),
                },
        });
    }

    public final JSONObject input;

    public SegmentationDSLParserRefusesAlienSourceTest(JSONObject input) {
        this.input = input;
    }

    @Test
    public void testItShouldThrow() throws UnknownValueError, UnknownCriterionError {
        try {
            parser.parse(input, new AlienDataSource(null));
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

}
