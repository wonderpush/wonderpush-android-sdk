package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.GeoCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.InsideCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.datasource.GeoDateSource;
import com.wonderpush.sdk.segmentation.datasource.GeoLocationSource;
import com.wonderpush.sdk.segmentation.datasource.InstallationSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class SegmentationDSLParserParseGeoTest {

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    @Parameterized.Parameters(name = "{index}: Test that {0} parses with geo, geobox {1}, date {2}")
    public static Iterable<Object[]> data() throws JSONException {
        return Arrays.asList(new Object[][]{
                {
                        new JSONObject("{ \"geo\": {} }"),
                        false,
                        false,
                },
                {
                        new JSONObject("{ \"geo\": { \"location\": { \"inside\": { \"geobox\": \"u\" } } } }"),
                        true,
                        false,
                },
                {
                        new JSONObject("{ \"geo\": { \"date\": { \"gt\": { \"date\": \"-PT1H\" } } } }"),
                        false,
                        true,
                },
                {
                        new JSONObject("{ \"geo\": { \"location\": { \"inside\": { \"geobox\": \"u\" } }, \"date\": { \"gt\": { \"date\": \"-PT1H\" } } } }"),
                        true,
                        true,
                },
        });
    }

    public final JSONObject input;
    public final boolean location;
    public final boolean date;

    public SegmentationDSLParserParseGeoTest(JSONObject input, boolean location, boolean date) {
        this.input = input;
        this.location = location;
        this.date = date;
    }

    @Test
    public void testItShouldParseProperly() throws BadInputError, UnknownValueError, UnknownCriterionError {
        GeoCriterionNode checkedAst = (GeoCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        if (location) {
            InsideCriterionNode checkedComparison = (InsideCriterionNode) checkedAst.locationComparison;
            assertThat(checkedComparison.context.dataSource, instanceOf(GeoLocationSource.class));
        } else {
            assertThat(checkedAst.locationComparison, nullValue());
        }
        if (date) {
            ComparisonCriterionNode checkedComparison = (ComparisonCriterionNode) checkedAst.dateComparison;
            assertThat(checkedComparison.context.dataSource, instanceOf(GeoDateSource.class));
        } else {
            assertThat(checkedAst.dateComparison, nullValue());
        }
    }

}
