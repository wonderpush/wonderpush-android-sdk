package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.criteria.EqualityCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.JoinCriterionNode;
import com.wonderpush.sdk.segmentation.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.datasource.EventSource;
import com.wonderpush.sdk.segmentation.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.datasource.UserSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class SegmentationDSLParserJoinsTest {

    public static final SegmentationDSLParser parser = SegmentationFactory.getDefaultThrowingParser();

    @Parameterized.Parameters(name = "{index}: Test that {0} from {1} dataSource parses withJoin={2} parentSource={3} childSource={4}")
    public static Iterable<Object[]> data() throws JSONException {
        return Arrays.asList(new Object[][]{
                {
                        new JSONObject("{ \"user\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new UserSource(),
                        false,
                        UserSource.class,
                        UserSource.class,
                },
                {
                        new JSONObject("{ \"user\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new InstallationSource(),
                        true,
                        UserSource.class,
                        UserSource.class,
                },
                {
                        new JSONObject("{ \"user\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new EventSource(),
                        true,
                        InstallationSource.class,
                        UserSource.class,
                },
                {
                        new JSONObject("{ \"installation\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new UserSource(),
                        true,
                        InstallationSource.class,
                        InstallationSource.class,
                },
                {
                        new JSONObject("{ \"installation\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new InstallationSource(),
                        false,
                        InstallationSource.class,
                        InstallationSource.class,
                },
                {
                        new JSONObject("{ \"installation\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new EventSource(),
                        true,
                        InstallationSource.class,
                        InstallationSource.class,
                },
                {
                        new JSONObject("{ \"event\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new UserSource(),
                        true,
                        InstallationSource.class,
                        EventSource.class,
                },
                {
                        new JSONObject("{ \"event\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new InstallationSource(),
                        true,
                        EventSource.class,
                        EventSource.class,
                },
                {
                        new JSONObject("{ \"event\": { \".field\": { \"eq\": \"foo\" } } }"),
                        new EventSource(),
                        false,
                        EventSource.class,
                        EventSource.class,
                },
        });
    }

    public final JSONObject input;
    public final DataSource inputSource;
    public final boolean expectJoin;
    public final Class<? extends DataSource> parentSource;
    public final Class<? extends DataSource> childSource;

    public SegmentationDSLParserJoinsTest(JSONObject input, DataSource inputSource, boolean expectJoin, Class<? extends DataSource> parentSource, Class<? extends DataSource> childSource) {
        this.input = input;
        this.inputSource = inputSource;
        this.expectJoin = expectJoin;
        this.parentSource = parentSource;
        this.childSource = childSource;
    }

    @Test
    public void testItShouldParseProperly() throws BadInputError, UnknownValueError, UnknownCriterionError {
        ASTCriterionNode parsed = parser.parse(input, inputSource);
        if (expectJoin) {
            JoinCriterionNode checkedAst = (JoinCriterionNode) parsed;
            assertThat(checkedAst.context.parentContext, is(not(nullValue())));
            assertThat(checkedAst.context.parentContext.dataSource, is(inputSource));
            assertThat(checkedAst.context.dataSource, instanceOf(parentSource));
            assertThat(checkedAst.child.context.dataSource.getRootDataSource(), instanceOf(childSource));
        } else {
            assertThat(parsed, instanceOf(EqualityCriterionNode.class));
            assertThat(parsed.context.dataSource.getRootDataSource(), instanceOf(parentSource));
        }
    }

}
