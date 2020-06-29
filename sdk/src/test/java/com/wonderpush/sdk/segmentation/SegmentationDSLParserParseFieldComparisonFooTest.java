package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.value.StringValueNode;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class SegmentationDSLParserParseFieldComparisonFooTest {

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    @Parameterized.Parameters(name = "{index}: Test that {0} parses with comparator {1}")
    public static Iterable<Object[]> data() throws JSONException {
        return Arrays.asList(new Object[][]{
                {
                        new JSONObject("{ \".field\": { \"gt\": \"foo\" } }"),
                        ComparisonCriterionNode.Comparator.gt,
                },
                {
                        new JSONObject("{ \".field\": { \"gte\": \"foo\" } }"),
                        ComparisonCriterionNode.Comparator.gte,
                },
                {
                        new JSONObject("{ \".field\": { \"lt\": \"foo\" } }"),
                        ComparisonCriterionNode.Comparator.lt,
                },
                {
                        new JSONObject("{ \".field\": { \"lte\": \"foo\" } }"),
                        ComparisonCriterionNode.Comparator.lte,
                },
        });
    }

    public final JSONObject input;
    public final ComparisonCriterionNode.Comparator comparator;

    public SegmentationDSLParserParseFieldComparisonFooTest(JSONObject input, ComparisonCriterionNode.Comparator comparator) {
        this.input = input;
        this.comparator = comparator;
    }

    @Test
    public void testItShouldParseProperly() throws BadInputError, UnknownValueError, UnknownCriterionError {
        ComparisonCriterionNode checkedAst = (ComparisonCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.comparator, is(comparator));
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource = (FieldSource) checkedAst.context.dataSource;
        assertThat(checkedDataSource.path.parts.length, is(1));
        assertThat(checkedDataSource.path.parts[0], is("field"));
        StringValueNode checkedValue = StringValueNode.class.cast(checkedAst.value);
        assertThat(checkedValue.getValue(), is("foo"));
    }

}
