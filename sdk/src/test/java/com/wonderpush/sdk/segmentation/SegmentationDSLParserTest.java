package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.criteria.ASTUnknownCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.AllCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.AnyCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.GeoCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.InsideCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.LastActivityDateCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.MatchAllCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.NotCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.OrCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.PrefixCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.PresenceCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.SubscriptionStatusCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.datasource.LastActivityDateSource;
import com.wonderpush.sdk.segmentation.value.ASTUnknownValueNode;
import com.wonderpush.sdk.segmentation.value.GeoBoxValueNode;
import com.wonderpush.sdk.segmentation.value.GeoCircleValueNode;
import com.wonderpush.sdk.segmentation.value.GeoPolygonValueNode;
import com.wonderpush.sdk.segmentation.value.NumberValueNode;
import com.wonderpush.sdk.segmentation.value.RelativeDateValueNode;
import com.wonderpush.sdk.segmentation.value.StringValueNode;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.fail;

public class SegmentationDSLParserTest {

    private static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    private static <T1, T2 extends T1> T1 checkType(T2 actual, Class<T1> expected) {
        assertThat(actual, instanceOf(expected));
        return expected.cast(actual);
    }

    @Test
    public void testItShouldNotThrowOnUnknownCriterionWhenConfiguredTo() throws ValueParserAlreadyExistsForKey, CriterionParserAlreadyExistsForKey, JSONException {
        SegmentationDSLParser parserLenientCriterion = new SegmentationDSLParser(new ParserConfig(
                new DefaultValueNodeParser(),
                new DefaultCriterionNodeParser(),
                true,
                false
        ));
        String unknownCriterionKey = "INEXISTENT CRITERION";
        String unknownCriterionValue = "foo";
        JSONObject unknownCriterionInput = new JSONObject();
        unknownCriterionInput.put(unknownCriterionKey, unknownCriterionValue);
        JSONObject unknownCriterionInput2 = new JSONObject();
        unknownCriterionInput2.put(".field", unknownCriterionInput);
        String unknownValueKey = "INEXISTENT VALUE";
        String unknownValueValue = "bar";
        JSONObject unknownValueInputFieldEq = new JSONObject();
        unknownValueInputFieldEq.put(unknownValueKey, unknownValueValue);
        JSONObject unknownValueInputField = new JSONObject();
        unknownValueInputField.put("eq", unknownValueInputFieldEq);
        JSONObject unknownValueInput = new JSONObject();
        unknownValueInput.put(".field", unknownValueInputField);

        try {
            parser.parse(unknownCriterionInput, new InstallationSource());
            fail("should throw");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(UnknownCriterionError.class));
        }
        try {
            parser.parse(unknownCriterionInput2, new InstallationSource());
            fail("should throw");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(UnknownCriterionError.class));
        }

        try {
            ASTUnknownCriterionNode checkedAst = (ASTUnknownCriterionNode) parserLenientCriterion.parse(unknownCriterionInput, new InstallationSource());
            assertThat(checkedAst.key, is(unknownCriterionKey));
            assertThat(checkedAst.value, is(unknownCriterionValue));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        try {
            ASTUnknownCriterionNode checkedAst = (ASTUnknownCriterionNode) parserLenientCriterion.parse(unknownCriterionInput2, new InstallationSource());
            assertThat(checkedAst.key, is(unknownCriterionKey));
            assertThat(checkedAst.value, is(unknownCriterionValue));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        // Ensure being lenient on criterion does not change being lenient on values
        for (SegmentationDSLParser anyParser : Arrays.asList(parser, parserLenientCriterion)) {
            try {
                anyParser.parse(unknownValueInput, new InstallationSource());
                fail("should throw");
            } catch (Exception ex) {
                assertThat(ex, instanceOf(UnknownValueError.class));
            }
        }
    }

    @Test
    public void testItShouldNotThrowOnUnknownValueWhenConfiguredTo() throws ValueParserAlreadyExistsForKey, CriterionParserAlreadyExistsForKey, JSONException {
        SegmentationDSLParser parserLenientValue = new SegmentationDSLParser(new ParserConfig(
                new DefaultValueNodeParser(),
                new DefaultCriterionNodeParser(),
                false,
                true
        ));
        String unknownCriterionKey = "INEXISTENT CRITERION";
        String unknownCriterionValue = "foo";
        JSONObject unknownCriterionInput = new JSONObject();
        unknownCriterionInput.put(unknownCriterionKey, unknownCriterionValue);
        JSONObject unknownCriterionInput2 = new JSONObject();
        unknownCriterionInput2.put(".field", unknownCriterionInput);
        String unknownValueKey = "INEXISTENT VALUE";
        String unknownValueValue = "bar";
        JSONObject unknownValueInputFieldEq = new JSONObject();
        unknownValueInputFieldEq.put(unknownValueKey, unknownValueValue);
        JSONObject unknownValueInputField = new JSONObject();
        unknownValueInputField.put("eq", unknownValueInputFieldEq);
        JSONObject unknownValueInput = new JSONObject();
        unknownValueInput.put(".field", unknownValueInputField);

        // Ensure being lenient on value does not change being lenient on criteria
        for (SegmentationDSLParser anyParser : Arrays.asList(parser, parserLenientValue)) {
            try {
                anyParser.parse(unknownCriterionInput, new InstallationSource());
                fail("should throw");
            } catch (Exception ex) {
                assertThat(ex, instanceOf(UnknownCriterionError.class));
            }
        }

        try {
            EqualityCriterionNode checkedAst = (EqualityCriterionNode) parserLenientValue.parse(unknownValueInput, new InstallationSource());
            ASTUnknownValueNode checkedAstValue = (ASTUnknownValueNode) checkedAst.value;
            assertThat(checkedAstValue.key, is(unknownValueKey));
            assertThat(checkedAstValue.getValue(), is(unknownValueValue));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        try {
            parser.parse(unknownValueInput, new InstallationSource());
            fail("should throw");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(UnknownValueError.class));
        }
    }

    @Test
    public void testItShouldParseEmptyObjectAsMatchAllCriterionNode() throws BadInputError, UnknownValueError, UnknownCriterionError {
        assertThat(parser.parse(new JSONObject(), new InstallationSource()), instanceOf(MatchAllCriterionNode.class));
    }

    @Test
    public void testItShouldParseFieldEqDateP1Y() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"eq\":{\"date\":\"P1Y\"}}}");
        EqualityCriterionNode checkedAst = (EqualityCriterionNode) parser.parse(input, new InstallationSource());
        RelativeDateValueNode checkedAstValue = RelativeDateValueNode.class.cast(checkedAst.value);
        assertThat(checkedAstValue.duration, is(new ISO8601Duration(true, 1, 0, 0, 0, 0, 0, 0)));

        Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);
        assertThat(checkedAstValue.getValue().doubleValue(), closeTo(nextYear.getTimeInMillis(), 500)); // match at ±500ms
    }

    @Test
    public void testItShouldParseFieldAll0Foo() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"all\":[0,\"foo\"]}}");
        AllCriterionNode checkedAst = (AllCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.values.size(), is(2));

        NumberValueNode item0 = NumberValueNode.class.cast(checkedAst.values.get(0));
        assertThat(item0.getValue(), is(0));

        StringValueNode item1 = StringValueNode.class.cast(checkedAst.values.get(1));
        assertThat(item1.getValue(), is("foo"));
    }

    @Test
    public void testItShouldParseFieldAny0Foo() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"any\":[0,\"foo\"]}}");
        AnyCriterionNode checkedAst = (AnyCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.values.size(), is(2));

        NumberValueNode item0 = NumberValueNode.class.cast(checkedAst.values.get(0));
        assertThat(item0.getValue(), is(0));

        StringValueNode item1 = StringValueNode.class.cast(checkedAst.values.get(1));
        assertThat(item1.getValue(), is("foo"));
    }

    @Test
    public void testItShouldParseFieldNotEqFoo() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"not\":{\"eq\":\"foo\"}}}");
        NotCriterionNode checkedAst = (NotCriterionNode) parser.parse(input, new InstallationSource());
        EqualityCriterionNode checkedAstChild = (EqualityCriterionNode) checkedAst.child;
        FieldSource checkedAstChildDataSource = (FieldSource) checkedAstChild.context.dataSource;
        assertThat(checkedAstChildDataSource.path.parts.length, is(1));
        assertThat(checkedAstChildDataSource.path.parts[0], is("field"));
        StringValueNode checkedAstChildValue = StringValueNode.class.cast(checkedAstChild.value);
        assertThat(checkedAstChildValue.getValue(), is("foo"));
    }

    @Test
    public void testItShouldParseFieldAndGt0Lt1() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"and\":[{\"gt\":0},{\"lt\":1}]}}");
        AndCriterionNode checkedAst = (AndCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.children.size(), is(2));

        ComparisonCriterionNode checkedAstChild0 = (ComparisonCriterionNode) checkedAst.children.get(0);
        assertThat(checkedAstChild0.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        assertThat(checkedAstChild0.comparator, is(ComparisonCriterionNode.Comparator.gt));
        FieldSource checkedDataSource0 = (FieldSource) checkedAstChild0.context.dataSource;
        assertThat(checkedDataSource0.path.parts.length, is(1));
        assertThat(checkedDataSource0.path.parts[0], is("field"));
        NumberValueNode checkedValue0 = NumberValueNode.class.cast(checkedAstChild0.value);
        assertThat(checkedValue0.getValue(), is(0));

        ComparisonCriterionNode checkedAstChild1 = (ComparisonCriterionNode) checkedAst.children.get(1);
        assertThat(checkedAstChild1.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        assertThat(checkedAstChild1.comparator, is(ComparisonCriterionNode.Comparator.lt));
        FieldSource checkedDataSource1 = (FieldSource) checkedAstChild1.context.dataSource;
        assertThat(checkedDataSource1.path.parts.length, is(1));
        assertThat(checkedDataSource1.path.parts[0], is("field"));
        NumberValueNode checkedValue1 = NumberValueNode.class.cast(checkedAstChild1.value);
        assertThat(checkedValue1.getValue(), is(1));
    }

    @Test
    public void testItShouldParseFieldOrGt0Lt1() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"or\":[{\"gt\":0},{\"lt\":1}]}}");
        OrCriterionNode checkedAst = (OrCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.children.size(), is(2));

        ComparisonCriterionNode checkedAstChild0 = (ComparisonCriterionNode) checkedAst.children.get(0);
        assertThat(checkedAstChild0.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        assertThat(checkedAstChild0.comparator, is(ComparisonCriterionNode.Comparator.gt));
        FieldSource checkedDataSource0 = (FieldSource) checkedAstChild0.context.dataSource;
        assertThat(checkedDataSource0.path.parts.length, is(1));
        assertThat(checkedDataSource0.path.parts[0], is("field"));
        NumberValueNode checkedValue0 = NumberValueNode.class.cast(checkedAstChild0.value);
        assertThat(checkedValue0.getValue(), is(0));

        ComparisonCriterionNode checkedAstChild1 = (ComparisonCriterionNode) checkedAst.children.get(1);
        assertThat(checkedAstChild1.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        assertThat(checkedAstChild1.comparator, is(ComparisonCriterionNode.Comparator.lt));
        FieldSource checkedDataSource1 = (FieldSource) checkedAstChild1.context.dataSource;
        assertThat(checkedDataSource1.path.parts.length, is(1));
        assertThat(checkedDataSource1.path.parts[0], is("field"));
        NumberValueNode checkedValue1 = NumberValueNode.class.cast(checkedAstChild1.value);
        assertThat(checkedValue1.getValue(), is(1));
    }

    @Test
    public void testItShouldParseFieldGt0Lt1() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"gt\":0,\"lt\":1}}");
        AndCriterionNode checkedAst = (AndCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.children.size(), is(2));

        // JSONObject implementation may not preserve order, sort children
        List<ASTCriterionNode> sortedChildren = new ArrayList<>(checkedAst.children);
        Collections.sort(sortedChildren, (o1, o2) -> ((ComparisonCriterionNode) o1).comparator.compareTo(((ComparisonCriterionNode) o2).comparator));

        ComparisonCriterionNode checkedAstChild0 = (ComparisonCriterionNode) sortedChildren.get(0);
        assertThat(checkedAstChild0.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        assertThat(checkedAstChild0.comparator, is(ComparisonCriterionNode.Comparator.gt));
        FieldSource checkedDataSource0 = (FieldSource) checkedAstChild0.context.dataSource;
        assertThat(checkedDataSource0.path.parts.length, is(1));
        assertThat(checkedDataSource0.path.parts[0], is("field"));
        NumberValueNode checkedValue0 = NumberValueNode.class.cast(checkedAstChild0.value);
        assertThat(checkedValue0.getValue(), is(0));

        ComparisonCriterionNode checkedAstChild1 = (ComparisonCriterionNode) sortedChildren.get(1);
        assertThat(checkedAstChild1.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        assertThat(checkedAstChild1.comparator, is(ComparisonCriterionNode.Comparator.lt));
        FieldSource checkedDataSource1 = (FieldSource) checkedAstChild1.context.dataSource;
        assertThat(checkedDataSource1.path.parts.length, is(1));
        assertThat(checkedDataSource1.path.parts[0], is("field"));
        NumberValueNode checkedValue1 = NumberValueNode.class.cast(checkedAstChild1.value);
        assertThat(checkedValue1.getValue(), is(1));
    }

    @Test
    public void testItShouldParseFieldPrefixFoo() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"prefix\":\"foo\"}}");
        PrefixCriterionNode checkedAst = (PrefixCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource = (FieldSource) checkedAst.context.dataSource;
        assertThat(checkedDataSource.path.parts.length, is(1));
        assertThat(checkedDataSource.path.parts[0], is("field"));
        StringValueNode checkedAstValue = (StringValueNode) checkedAst.value;
        assertThat(checkedAstValue.getValue(), is("foo"));
    }

    @Test
    public void testItShouldParseFieldInsideGeoboxEzs42() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        Geohash geohash = Geohash.parse("ezs42");
        JSONObject input = new JSONObject("{\".field\":{\"inside\":{\"geobox\":\"ezs42\"}}}");
        InsideCriterionNode checkedAst = (InsideCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource = (FieldSource) checkedAst.context.dataSource;
        assertThat(checkedDataSource.path.parts.length, is(1));
        assertThat(checkedDataSource.path.parts[0], is("field"));
        GeoBoxValueNode checkedAstValue = (GeoBoxValueNode) checkedAst.value;
        assertThat(checkedAstValue.getValue().top, is(geohash.top));
        assertThat(checkedAstValue.getValue().right, is(geohash.right));
        assertThat(checkedAstValue.getValue().bottom, is(geohash.bottom));
        assertThat(checkedAstValue.getValue().left, is(geohash.left));
    }

    @Test
    public void testItShouldParseFieldInsideGeocircleRadius1CenterLat1Lon2() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        Geohash geohash = Geohash.parse("ezs42");
        JSONObject input = new JSONObject("{\".field\":{\"inside\":{\"geocircle\":{\"radius\":1,\"center\":{\"lat\":1,\"lon\":2}}}}}");
        InsideCriterionNode checkedAst = (InsideCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource = (FieldSource) checkedAst.context.dataSource;
        assertThat(checkedDataSource.path.parts.length, is(1));
        assertThat(checkedDataSource.path.parts[0], is("field"));
        GeoCircleValueNode checkedAstValue = (GeoCircleValueNode) checkedAst.value;
        assertThat(checkedAstValue.getValue().radiusMeters, is(1.0));
        assertThat(checkedAstValue.getValue().center.lat, is(1.0));
        assertThat(checkedAstValue.getValue().center.lon, is(2.0));
    }

    @Test
    public void testItShouldParseFieldInsideGeopolygonTLat0Lon0V() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\".field\":{\"inside\":{\"geopolygon\":[\"t\",{\"lat\":0,\"lon\":0},\"v\"]}}}");
        InsideCriterionNode checkedAst = (InsideCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource = (FieldSource) checkedAst.context.dataSource;
        assertThat(checkedDataSource.path.parts.length, is(1));
        assertThat(checkedDataSource.path.parts[0], is("field"));
        GeoPolygonValueNode checkedAstValue = (GeoPolygonValueNode) checkedAst.value;
        assertThat(checkedAstValue.getValue().points.size(), is(3));
        assertThat(checkedAstValue.getValue().points.get(0).lat, is(Geohash.parse("t").toGeoLocation().lat));
        assertThat(checkedAstValue.getValue().points.get(0).lon, is(Geohash.parse("t").toGeoLocation().lon));
        assertThat(checkedAstValue.getValue().points.get(1).lat, is(0.0));
        assertThat(checkedAstValue.getValue().points.get(1).lon, is(0.0));
        assertThat(checkedAstValue.getValue().points.get(2).lat, is(Geohash.parse("v").toGeoLocation().lat));
        assertThat(checkedAstValue.getValue().points.get(2).lon, is(Geohash.parse("v").toGeoLocation().lon));
    }

    @Test
    public void testItShouldNotParseLastActivityDate() throws JSONException, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"lastActivityDate\":{}}");
        try {
            parser.parse(input, new InstallationSource());
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldParseLastActivityDateGteDateMinusP1D() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"lastActivityDate\":{\"gte\":{\"date\":\"-P1D\"}}}");
        LastActivityDateCriterionNode checkedAst = (LastActivityDateCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.context.dataSource, instanceOf(InstallationSource.class));
        assertThat(checkedAst.dateComparison.context.dataSource, instanceOf(LastActivityDateSource.class));
        assertThat(checkedAst.dateComparison, instanceOf(ComparisonCriterionNode.class));
    }

    @Test
    public void testItShouldParsePresencePresentTrueSinceDateOrGt0Lt1Gt10Lt11() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"presence\":{\"present\":true,\"sinceDate\":{\"or\":[{\"gt\":0,\"lt\":1},{\"gt\":10,\"lt\":11}]}}}");
        assertThat(parser.parse(input, new InstallationSource()), instanceOf(PresenceCriterionNode.class));
    }

    @Test
    public void testItShouldNotParseUserPresencePresentTrue() throws JSONException, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"user\":{\"presence\":{\"present\":true}}}");
        try {
            parser.parse(input, new InstallationSource());
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldNotParseEventPresencePresentTrue() throws JSONException, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"event\":{\"presence\":{\"present\":true}}}");
        try {
            parser.parse(input, new InstallationSource());
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldParseGeo() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"geo\":{}}");
        assertThat(parser.parse(input, new InstallationSource()), instanceOf(GeoCriterionNode.class));
    }

    @Test
    public void testItShouldParseSomeLongGeoTest() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject(" {\"geo\":{\"location\":{\"and\":[{\"inside\":{\"geobox\":\"u\"}},{\"inside\":{\"geocircle\":{\"center\":\"u\",\"radius\":1}}}]},\"date\":{\"or\":[{\"gt\":1},{\"lt\":0}]}}}");
        assertThat(parser.parse(input, new InstallationSource()), instanceOf(GeoCriterionNode.class));
    }

    @Test
    public void testItShouldNotParseUserGeo() throws JSONException, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"user\":{\"geo\":{}}}");
        try {
            parser.parse(input, new InstallationSource());
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldNotParseEventGeo() throws JSONException, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"event\":{\"geo\":{}}}");
        try {
            parser.parse(input, new InstallationSource());
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldParseSubscriptionStatusOptIn() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"subscriptionStatus\":\"optIn\"}");
        SubscriptionStatusCriterionNode checkedAst = (SubscriptionStatusCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.subscriptionStatus, is(SubscriptionStatusCriterionNode.SubscriptionStatus.optIn));
    }

    @Test
    public void testItShouldParseSubscriptionStatusOptOut() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"subscriptionStatus\":\"optOut\"}");
        SubscriptionStatusCriterionNode checkedAst = (SubscriptionStatusCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.subscriptionStatus, is(SubscriptionStatusCriterionNode.SubscriptionStatus.optOut));
    }

    @Test
    public void testItShouldParseSubscriptionStatusSoftOptOut() throws JSONException, BadInputError, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"subscriptionStatus\":\"softOptOut\"}");
        SubscriptionStatusCriterionNode checkedAst = (SubscriptionStatusCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.subscriptionStatus, is(SubscriptionStatusCriterionNode.SubscriptionStatus.softOptOut));
    }

    @Test
    public void testItShouldNotParseSubscriptionStatusFoo() throws JSONException, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"subscriptionStatus\":\"foo\"}");
        try {
            parser.parse(input, new InstallationSource());
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldNotParseEventSubscriptionStatusOptIn() throws JSONException, UnknownValueError, UnknownCriterionError {
        JSONObject input = new JSONObject("{\"event\":{\"subscriptionStatus\":\"optIn\"}}");
        try {
            parser.parse(input, new InstallationSource());
            fail("should have thrown");
        } catch (BadInputError ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldRewindFieldPathForFooEqFooInstallationBarEqBar() throws JSONException, UnknownValueError, UnknownCriterionError, BadInputError {
        JSONObject input = new JSONObject("{\".foo\":{\"eq\":\"foo\",\"installation\":{\".bar\":{\"eq\":\"bar\"}}}}");
        AndCriterionNode checkedAst = (AndCriterionNode) parser.parse(input, new InstallationSource());
        List<ASTCriterionNode> checkedAstChildren = new ArrayList<>(checkedAst.children);
        Collections.sort(checkedAstChildren, (o1, o2) -> -((FieldSource)o1.context.dataSource).path.parts[0].compareTo(((FieldSource)o2.context.dataSource).path.parts[0]));
        assertThat(checkedAstChildren.size(), is(2));
        EqualityCriterionNode checkedAstChild0 = (EqualityCriterionNode) checkedAstChildren.get(0);
        assertThat(checkedAstChild0.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource0 = (FieldSource) checkedAstChild0.context.dataSource;
        assertThat(checkedDataSource0.path.parts.length, is(1));
        assertThat(checkedDataSource0.path.parts[0], is("foo"));
        StringValueNode chechedValue0 = StringValueNode.class.cast(checkedAstChild0.value);
        assertThat(chechedValue0.getValue(), is("foo"));
        EqualityCriterionNode checkedAstChild1 = (EqualityCriterionNode) checkedAstChildren.get(1);
        assertThat(checkedAstChild1.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource1 = (FieldSource) checkedAstChild1.context.dataSource;
        assertThat(checkedDataSource1.path.parts.length, is(1));
        assertThat(checkedDataSource1.path.parts[0], is("bar"));
        StringValueNode chechedValue1 = StringValueNode.class.cast(checkedAstChild1.value);
        assertThat(chechedValue1.getValue(), is("bar"));
    }

    @Test
    public void testItShouldParseNotFieldEqFoo() throws BadInputError, UnknownValueError, UnknownCriterionError, JSONException {
        JSONObject input = new JSONObject("{\"not\":{\".field\":{\"eq\":\"foo\"}}}");
        NotCriterionNode checkedAst = (NotCriterionNode) parser.parse(input, new InstallationSource());
        EqualityCriterionNode checkedAstChild = (EqualityCriterionNode) checkedAst.child;
        assertThat(checkedAstChild.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource = (FieldSource) checkedAstChild.context.dataSource;
        assertThat(checkedDataSource.path.parts.length, is(1));
        assertThat(checkedDataSource.path.parts[0], is("field"));
        StringValueNode checkedValue = StringValueNode.class.cast(checkedAstChild.value);
        assertThat(checkedValue.getValue(), is("foo"));
    }

    @Test
    public void testItShouldParseAndFieldEqFooFieldEqBar() throws BadInputError, UnknownValueError, UnknownCriterionError, JSONException {
        JSONObject input = new JSONObject("{\"and\":[{\".field\":{\"eq\":\"foo\"}},{\".field\":{\"eq\":\"bar\"}}]}");
        AndCriterionNode checkedAst = (AndCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.children.size(), is(2));

        EqualityCriterionNode checkedAstChild0 = (EqualityCriterionNode) checkedAst.children.get(0);
        assertThat(checkedAstChild0.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource0 = (FieldSource) checkedAstChild0.context.dataSource;
        assertThat(checkedDataSource0.path.parts.length, is(1));
        assertThat(checkedDataSource0.path.parts[0], is("field"));
        StringValueNode checkedValue0 = StringValueNode.class.cast(checkedAstChild0.value);
        assertThat(checkedValue0.getValue(), is("foo"));

        EqualityCriterionNode checkedAstChild1 = (EqualityCriterionNode) checkedAst.children.get(1);
        assertThat(checkedAstChild1.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource1 = (FieldSource) checkedAstChild1.context.dataSource;
        assertThat(checkedDataSource1.path.parts.length, is(1));
        assertThat(checkedDataSource1.path.parts[0], is("field"));
        StringValueNode checkedValue1 = StringValueNode.class.cast(checkedAstChild1.value);
        assertThat(checkedValue1.getValue(), is("bar"));
    }

    @Test
    public void testItShouldParseFieldFooEqFooFieldBarEqBar() throws BadInputError, UnknownValueError, UnknownCriterionError, JSONException {
        JSONObject input = new JSONObject("{\".fieldFoo\":{\"eq\":\"foo\"},\".fieldBar\":{\"eq\":\"bar\"}}");
        AndCriterionNode checkedAst = (AndCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.children.size(), is(2));
        List<ASTCriterionNode> checkedAstChildren = new ArrayList<>(checkedAst.children);
        Collections.sort(checkedAstChildren, (o1, o2) -> -((FieldSource)o1.context.dataSource).path.parts[0].compareTo(((FieldSource)o2.context.dataSource).path.parts[0]));

        EqualityCriterionNode checkedAstChild0 = (EqualityCriterionNode) checkedAst.children.get(0);
        assertThat(checkedAstChild0.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource0 = (FieldSource) checkedAstChild0.context.dataSource;
        assertThat(checkedDataSource0.path.parts.length, is(1));
        assertThat(checkedDataSource0.path.parts[0], is("fieldFoo"));
        StringValueNode checkedValue0 = StringValueNode.class.cast(checkedAstChild0.value);
        assertThat(checkedValue0.getValue(), is("foo"));

        EqualityCriterionNode checkedAstChild1 = (EqualityCriterionNode) checkedAst.children.get(1);
        assertThat(checkedAstChild1.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource1 = (FieldSource) checkedAstChild1.context.dataSource;
        assertThat(checkedDataSource1.path.parts.length, is(1));
        assertThat(checkedDataSource1.path.parts[0], is("fieldBar"));
        StringValueNode checkedValue1 = StringValueNode.class.cast(checkedAstChild1.value);
        assertThat(checkedValue1.getValue(), is("bar"));
    }

    @Test
    public void testItShouldParseOrFieldEqFooFieldEqBar() throws BadInputError, UnknownValueError, UnknownCriterionError, JSONException {
        JSONObject input = new JSONObject("{\"or\":[{\".field\":{\"eq\":\"foo\"}},{\".field\":{\"eq\":\"bar\"}}]}");
        OrCriterionNode checkedAst = (OrCriterionNode) parser.parse(input, new InstallationSource());
        assertThat(checkedAst.children.size(), is(2));

        EqualityCriterionNode checkedAstChild0 = (EqualityCriterionNode) checkedAst.children.get(0);
        assertThat(checkedAstChild0.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource0 = (FieldSource) checkedAstChild0.context.dataSource;
        assertThat(checkedDataSource0.path.parts.length, is(1));
        assertThat(checkedDataSource0.path.parts[0], is("field"));
        StringValueNode checkedValue0 = StringValueNode.class.cast(checkedAstChild0.value);
        assertThat(checkedValue0.getValue(), is("foo"));

        EqualityCriterionNode checkedAstChild1 = (EqualityCriterionNode) checkedAst.children.get(1);
        assertThat(checkedAstChild1.context.dataSource.getRootDataSource(), instanceOf(InstallationSource.class));
        FieldSource checkedDataSource1 = (FieldSource) checkedAstChild1.context.dataSource;
        assertThat(checkedDataSource1.path.parts.length, is(1));
        assertThat(checkedDataSource1.path.parts[0], is("field"));
        StringValueNode checkedValue1 = StringValueNode.class.cast(checkedAstChild1.value);
        assertThat(checkedValue1.getValue(), is("bar"));
    }

}
