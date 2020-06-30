package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.parser.value.ASTUnknownValueNode;
import com.wonderpush.sdk.segmentation.parser.value.BooleanValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.DurationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoBoxValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoCircleValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoLocationValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoPolygonValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NullValueNode;
import com.wonderpush.sdk.segmentation.parser.value.NumberValueNode;
import com.wonderpush.sdk.segmentation.parser.value.RelativeDateValueNode;
import com.wonderpush.sdk.segmentation.parser.value.StringValueNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ASTValueVisitorTest {

    private enum Markers {
        ASTUnknownValueNode,
        DateValueNode,
        DurationValueNode,
        RelativeDateValueNode,
        GeoLocationValueNode,
        GeoBoxValueNode,
        GeoCircleValueNode,
        GeoPolygonValueNode,
        BooleanValueNode,
        NullValueNode,
        NumberValueNode,
        StringValueNode,
    }

    private static class TestRecordingValueVisitor implements ASTValueVisitor<Markers> {

        public final List<ASTValueNode<?>> seenObjects = new ArrayList<>();

        @Override
        public Markers visitASTUnknownValueNode(ASTUnknownValueNode node) {
            this.seenObjects.add(node);
            return Markers.ASTUnknownValueNode;
        }

        @Override
        public Markers visitDateValueNode(DateValueNode node) {
            this.seenObjects.add(node);
            return Markers.DateValueNode;
        }

        @Override
        public Markers visitDurationValueNode(DurationValueNode node) {
            this.seenObjects.add(node);
            return Markers.DurationValueNode;
        }

        @Override
        public Markers visitRelativeDateValueNode(RelativeDateValueNode node) {
            this.seenObjects.add(node);
            return Markers.RelativeDateValueNode;
        }

        @Override
        public Markers visitGeoLocationValueNode(GeoLocationValueNode node) {
            this.seenObjects.add(node);
            return Markers.GeoLocationValueNode;
        }

        @Override
        public Markers visitGeoBoxValueNode(GeoBoxValueNode node) {
            this.seenObjects.add(node);
            return Markers.GeoBoxValueNode;
        }

        @Override
        public Markers visitGeoCircleValueNode(GeoCircleValueNode node) {
            this.seenObjects.add(node);
            return Markers.GeoCircleValueNode;
        }

        @Override
        public Markers visitGeoPolygonValueNode(GeoPolygonValueNode node) {
            this.seenObjects.add(node);
            return Markers.GeoPolygonValueNode;
        }

        @Override
        public Markers visitBooleanValueNode(BooleanValueNode node) {
            this.seenObjects.add(node);
            return Markers.BooleanValueNode;
        }

        @Override
        public Markers visitNullValueNode(NullValueNode node) {
            this.seenObjects.add(node);
            return Markers.NullValueNode;
        }

        @Override
        public Markers visitNumberValueNode(NumberValueNode node) {
            this.seenObjects.add(node);
            return Markers.NumberValueNode;
        }

        @Override
        public Markers visitStringValueNode(StringValueNode node) {
            this.seenObjects.add(node);
            return Markers.StringValueNode;
        }

    }

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultParser();
    public static final ParsingContext context = new ParsingContext(parser, null, new InstallationSource());
    public static final GeoLocation geoLocation = new GeoLocation(0, 0);

    @Parameterized.Parameters(name = "{index}: Test that {0} is properly visited")
    public static Iterable<Object[]> data() throws BadInputError {
        return Arrays.asList(new Object[][]{
                {
                        new BooleanValueNode(context, true),
                        Markers.BooleanValueNode,
                },
                {
                        new DateValueNode(context, 0),
                        Markers.DateValueNode,
                },
                {
                        new DurationValueNode(context, 0),
                        Markers.DurationValueNode,
                },
                {
                        new GeoBoxValueNode(context, new GeoBox(0, 0, 0, 0)),
                        Markers.GeoBoxValueNode,
                },
                {
                        new GeoCircleValueNode(context, new GeoCircle(geoLocation, 1)),
                        Markers.GeoCircleValueNode,
                },
                {
                        new GeoLocationValueNode(context, geoLocation),
                        Markers.GeoLocationValueNode,
                },
                {
                        new GeoPolygonValueNode(context, new GeoPolygon(Collections.singletonList(geoLocation))),
                        Markers.GeoPolygonValueNode,
                },
                {
                        new NullValueNode(context),
                        Markers.NullValueNode,
                },
                {
                        new NumberValueNode(context, 0),
                        Markers.NumberValueNode,
                },
                {
                        new RelativeDateValueNode(context, ISO8601Duration.parse("P0D")),
                        Markers.RelativeDateValueNode,
                },
                {
                        new StringValueNode(context, "foo"),
                        Markers.StringValueNode,
                },
                {
                        new ASTUnknownValueNode(context, "foo", "bar"),
                        Markers.ASTUnknownValueNode,
                },
        });
    }

    public final ASTValueNode<?> valueNode;
    public final ASTValueVisitorTest.Markers expectedMarker;

    public ASTValueVisitorTest(ASTValueNode<?> valueNode, ASTValueVisitorTest.Markers expectedMarker) {
        this.valueNode = valueNode;
        this.expectedMarker = expectedMarker;
    }

    @Test
    public void testItShouldCallAppropriateMethods() {
        ASTValueVisitorTest.TestRecordingValueVisitor visitor = new ASTValueVisitorTest.TestRecordingValueVisitor();
        assertThat(valueNode.accept(visitor), sameInstance(expectedMarker));
        assertThat(visitor.seenObjects.size(), is(1));
        assertThat(visitor.seenObjects.get(0), sameInstance(valueNode));
    }

}
