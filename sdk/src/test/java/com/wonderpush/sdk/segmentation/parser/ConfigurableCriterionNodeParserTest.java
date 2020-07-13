package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.criteria.AndCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.OrCriterionNode;
import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ConfigurableCriterionNodeParserTest {

    @Test
    public void testItShouldForbidRegisteringTwiceAParserWithSameName() {
        ConfigurableCriterionNodeParser ccnp = new ConfigurableCriterionNodeParser();
        try {
            ccnp.registerExactNameParser("foo", (context, key, input) -> null);
            assertThat(true, is(true));
        } catch (CriterionParserAlreadyExistsForKey ex) {
            fail(ex.getMessage());
        }
        try {
            ccnp.registerExactNameParser("foo", (context, key, input) -> null);
            fail("should have thrown");
        } catch (CriterionParserAlreadyExistsForKey ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldReturnParseKnownNodesUsingDynamicNameAfterKnownNamesAndNullOnUnknownNodes() throws BadInputError, CriterionParserAlreadyExistsForKey, UnknownCriterionError, UnknownValueError {
        ConfigurableCriterionNodeParser ccnp = new ConfigurableCriterionNodeParser();
        ccnp.registerExactNameParser("known", (context, key, input) -> new AndCriterionNode(context, new ArrayList<>()));
        ccnp.registerDynamicNameParser((context, key, input) -> key.startsWith("k") ? new OrCriterionNode(context, new ArrayList<>()) : null);
        SegmentationDSLParser parser = new SegmentationDSLParser(new ParserConfig(new ConfigurableValueNodeParser(), ccnp));
        ParsingContext context = new ParsingContext(parser, null, new InstallationSource());
        assertThat(ccnp.parseCriterion(context, "known", "stuff"), instanceOf(AndCriterionNode.class));
        assertThat(ccnp.parseCriterion(context, "kewl", "stuff"), instanceOf(OrCriterionNode.class));
        assertThat(ccnp.parseCriterion(context, "krown", "stuff"), instanceOf(OrCriterionNode.class));
        assertThat(ccnp.parseCriterion(context, "unknown", "stuff"), nullValue());
    }

}
