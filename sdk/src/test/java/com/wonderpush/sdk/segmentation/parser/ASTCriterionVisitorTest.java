package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.criteria.ASTUnknownCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AllCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.AnyCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.ComparisonCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.GeoCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.InsideCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.JoinCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.LastActivityDateCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.MatchAllCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.NotCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.OrCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PrefixCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.PresenceCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.SubscriptionStatusCriterionNode;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.parser.value.ASTUnknownValueNode;
import com.wonderpush.sdk.segmentation.parser.value.GeoBoxValueNode;
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
public class ASTCriterionVisitorTest {

    private enum Markers {
        MatchAllCriterionNode,
        AndCriterionNode,
        OrCriterionNode,
        NotCriterionNode,
        GeoCriterionNode,
        SubscriptionStatusCriterionNode,
        LastActivityDateCriterionNode,
        PresenceCriterionNode,
        JoinCriterionNode,
        EqualityCriterionNode,
        AnyCriterionNode,
        AllCriterionNode,
        ComparisonCriterionNode,
        PrefixCriterionNode,
        InsideCriterionNode,
        ASTUnknownCriterionNode,
    }

    private static class TestRecordingCriterionVisitor implements ASTCriterionVisitor<Markers> {

        public final List<ASTCriterionNode> seenObjects = new ArrayList<>();

        @Override
        public Markers visitMatchAllCriterionNode(MatchAllCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.MatchAllCriterionNode;
        }

        @Override
        public Markers visitAndCriterionNode(AndCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.AndCriterionNode;
        }

        @Override
        public Markers visitOrCriterionNode(OrCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.OrCriterionNode;
        }

        @Override
        public Markers visitNotCriterionNode(NotCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.NotCriterionNode;
        }

        @Override
        public Markers visitGeoCriterionNode(GeoCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.GeoCriterionNode;
        }

        @Override
        public Markers visitSubscriptionStatusCriterionNode(SubscriptionStatusCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.SubscriptionStatusCriterionNode;
        }

        @Override
        public Markers visitLastActivityDateCriterionNode(LastActivityDateCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.LastActivityDateCriterionNode;
        }

        @Override
        public Markers visitPresenceCriterionNode(PresenceCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.PresenceCriterionNode;
        }

        @Override
        public Markers visitJoinCriterionNode(JoinCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.JoinCriterionNode;
        }

        @Override
        public Markers visitEqualityCriterionNode(EqualityCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.EqualityCriterionNode;
        }

        @Override
        public Markers visitAnyCriterionNode(AnyCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.AnyCriterionNode;
        }

        @Override
        public Markers visitAllCriterionNode(AllCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.AllCriterionNode;
        }

        @Override
        public Markers visitComparisonCriterionNode(ComparisonCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.ComparisonCriterionNode;
        }

        @Override
        public Markers visitPrefixCriterionNode(PrefixCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.PrefixCriterionNode;
        }

        @Override
        public Markers visitInsideCriterionNode(InsideCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.InsideCriterionNode;
        }

        @Override
        public Markers visitASTUnknownCriterionNode(ASTUnknownCriterionNode node) {
            this.seenObjects.add(node);
            return Markers.ASTUnknownCriterionNode;
        }

    }

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultParser();
    public static final ParsingContext context = new ParsingContext(parser, null, new InstallationSource());
    public static final ASTUnknownValueNode unknownValue = new ASTUnknownValueNode(context, "foo", "bar");
    public static final GeoBoxValueNode geoValue = new GeoBoxValueNode(context, new GeoBox(0, 0, 0, 0));
    public static final ASTUnknownCriterionNode unknownCriterion = new ASTUnknownCriterionNode(context, "foo", "bar");

    @Parameterized.Parameters(name = "{index}: Test that {0} is properly visited")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                    new ASTUnknownCriterionNode(context, "foo", null),
                    Markers.ASTUnknownCriterionNode,
                },
                {
                    new AllCriterionNode(context, Collections.singletonList(unknownValue)),
                    Markers.AllCriterionNode,
                },
                {
                    new AndCriterionNode(context, Collections.singletonList(unknownCriterion)),
                    Markers.AndCriterionNode,
                },
                {
                    new AnyCriterionNode(context, Collections.singletonList(unknownValue)),
                    Markers.AnyCriterionNode,
                },
                {
                    new ComparisonCriterionNode(context, ComparisonCriterionNode.Comparator.gt, unknownValue),
                    Markers.ComparisonCriterionNode,
                },
                {
                    new EqualityCriterionNode(context, unknownValue),
                    Markers.EqualityCriterionNode,
                },
                {
                    new GeoCriterionNode(context, null, null),
                    Markers.GeoCriterionNode,
                },
                {
                    new InsideCriterionNode(context, geoValue),
                    Markers.InsideCriterionNode,
                },
                {
                    new JoinCriterionNode(context, unknownCriterion),
                    Markers.JoinCriterionNode,
                },
                {
                    new LastActivityDateCriterionNode(context, unknownCriterion),
                    Markers.LastActivityDateCriterionNode,
                },
                {
                    new MatchAllCriterionNode(context),
                    Markers.MatchAllCriterionNode,
                },
                {
                    new NotCriterionNode(context, unknownCriterion),
                    Markers.NotCriterionNode,
                },
                {
                    new OrCriterionNode(context, Collections.singletonList(unknownCriterion)),
                    Markers.OrCriterionNode,
                },
                {
                    new PrefixCriterionNode(context, new StringValueNode(context, "foo")),
                    Markers.PrefixCriterionNode,
                },
                {
                    new PresenceCriterionNode(context, true, null, null),
                    Markers.PresenceCriterionNode,
                },
                {
                    new SubscriptionStatusCriterionNode(context, SubscriptionStatusCriterionNode.SubscriptionStatus.optIn),
                    Markers.SubscriptionStatusCriterionNode,
                },
        });
    }

    public final ASTCriterionNode criterionNode;
    public final Markers expectedMarker;

    public ASTCriterionVisitorTest(ASTCriterionNode criterionNode, Markers expectedMarker) {
        this.criterionNode = criterionNode;
        this.expectedMarker = expectedMarker;
    }

    @Test
    public void testItShouldCallAppropriateMethods() {
        TestRecordingCriterionVisitor visitor = new TestRecordingCriterionVisitor();
        assertThat(criterionNode.accept(visitor), sameInstance(expectedMarker));
        assertThat(visitor.seenObjects.size(), is(1));
        assertThat(visitor.seenObjects.get(0), sameInstance(criterionNode));
    }

}
