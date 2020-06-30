package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PresenceCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceElapsedTimeSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceSinceDateSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class SegmentationDSLParserParsePresenceTest {

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    @Parameterized.Parameters(name = "{index}: Test that {0} parses with presence {1}, {2} sinceDate comparisons and {3} elapsedTime comparisons")
    public static Iterable<Object[]> data() throws JSONException {
        return Arrays.asList(new Object[][]{
                {
                        new JSONObject("{ \"presence\": { \"present\": true } }"),
                        true,
                        0,
                        0,
                },
                {
                        new JSONObject("{ \"presence\": { \"present\": false } }"),
                        false,
                        0,
                        0,
                },
                {
                        new JSONObject("{ \"presence\": { \"present\": true, \"sinceDate\": { \"gt\": 0 } } }"),
                        true,
                        1,
                        0,
                },
                {
                        new JSONObject("{ \"presence\": { \"present\": true, \"elapsedTime\": { \"gt\": 0 } } }"),
                        true,
                        0,
                        1,
                },
                {
                        new JSONObject("{ \"presence\": { \"present\": true, \"sinceDate\": { \"gt\": 0, \"lt\": 0 } } }"),
                        true,
                        2,
                        0,
                },
                {
                        new JSONObject("{ \"presence\": { \"present\": true, \"elapsedTime\": { \"gt\": 0, \"lt\": 0 } } }"),
                        true,
                        0,
                        2,
                },
                {
                        new JSONObject("{ \"presence\": { \"present\": true, \"sinceDate\": { \"gt\": 0, \"lt\": 0 }, \"elapsedTime\": { \"gt\": 0, \"lt\": 0 } } }"),
                        true,
                        2,
                        2,
                },
        });
    }

    public final JSONObject input;
    public final boolean present;
    public final int sinceDateComparisons;
    public final int elapsedTimeComparisons;

    public SegmentationDSLParserParsePresenceTest(JSONObject input, boolean present, int sinceDateComparisons, int elapsedTimeComparisons) {
        this.input = input;
        this.present = present;
        this.sinceDateComparisons = sinceDateComparisons;
        this.elapsedTimeComparisons = elapsedTimeComparisons;
    }

    @Test
    public void testItShouldParseProperly() throws BadInputError, UnknownValueError, UnknownCriterionError {
        PresenceCriterionNode checkedAst = (PresenceCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        assertThat(checkedAst.present, is(present));
        if (sinceDateComparisons == 0) {
            assertThat(checkedAst.sinceDateComparison, nullValue());
        } else if (sinceDateComparisons == 1) {
            ComparisonCriterionNode checkedComparison = (ComparisonCriterionNode) checkedAst.sinceDateComparison;
            PresenceSinceDateSource checkedComparisonSource = (PresenceSinceDateSource) checkedComparison.context.dataSource;
            assertThat(checkedComparisonSource.present, is(present));
        } else {
            AndCriterionNode checkedAstSinceDate = (AndCriterionNode) checkedAst.sinceDateComparison;
            assertThat(checkedAstSinceDate.children.size(), is(sinceDateComparisons));
            for (ASTCriterionNode criterionNode : checkedAstSinceDate.children) {
                ComparisonCriterionNode checkedComparison = (ComparisonCriterionNode) criterionNode;
                PresenceSinceDateSource checkedComparisonSource = (PresenceSinceDateSource) checkedComparison.context.dataSource;
                assertThat(checkedComparisonSource.present, is(present));
            }
        }
        if (elapsedTimeComparisons == 0) {
            assertThat(checkedAst.elapsedTimeComparison, nullValue());
        } else if (elapsedTimeComparisons == 1) {
            ComparisonCriterionNode checkedComparison = (ComparisonCriterionNode) checkedAst.elapsedTimeComparison;
            PresenceElapsedTimeSource checkedComparisonSource = (PresenceElapsedTimeSource) checkedComparison.context.dataSource;
            assertThat(checkedComparisonSource.present, is(present));
        } else {
            AndCriterionNode checkedAstSinceDate = (AndCriterionNode) checkedAst.elapsedTimeComparison;
            assertThat(checkedAstSinceDate.children.size(), is(elapsedTimeComparisons));
            for (ASTCriterionNode criterionNode : checkedAstSinceDate.children) {
                ComparisonCriterionNode checkedComparison = (ComparisonCriterionNode) criterionNode;
                PresenceElapsedTimeSource checkedComparisonSource = (PresenceElapsedTimeSource) checkedComparison.context.dataSource;
                assertThat(checkedComparisonSource.present, is(present));
            }
        }
    }

}
