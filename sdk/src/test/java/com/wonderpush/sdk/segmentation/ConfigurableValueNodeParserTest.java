package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.value.NumberValueNode;
import com.wonderpush.sdk.segmentation.value.StringValueNode;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ConfigurableValueNodeParserTest {

    @Test
    public void testItShouldForbidRegisteringTwiceAParserWithSameName() {
        ConfigurableValueNodeParser ccnp = new ConfigurableValueNodeParser();
        try {
            ccnp.registerExactNameParser("foo", (context, key, input) -> null);
            assertThat(true, is(true));
        } catch (ValueParserAlreadyExistsForKey ex) {
            fail(ex.getMessage());
        }
        try {
            ccnp.registerExactNameParser("foo", (context, key, input) -> null);
            fail("should have thrown");
        } catch (ValueParserAlreadyExistsForKey ex) {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testItShouldReturnParseKnownNodesUsingDynamicNameAfterKnownNamesAndNullOnUnknownNodes() throws BadInputError, ValueParserAlreadyExistsForKey {
        ConfigurableValueNodeParser cvnp = new ConfigurableValueNodeParser();
        cvnp.registerExactNameParser("known", (context, key, input) -> ASTValueNode.castToObject(new StringValueNode(context, "")));
        cvnp.registerDynamicNameParser((context, key, input) -> key.startsWith("k") ? ASTValueNode.castToObject(new NumberValueNode(context, 0)) : null);
        SegmentationDSLParser parser = new SegmentationDSLParser(new ParserConfig(cvnp, new ConfigurableCriterionNodeParser()));
        ParsingContext context = new ParsingContext(parser, null, new InstallationSource());
        assertThat(cvnp.parseValue(context, "known", "stuff"), instanceOf(StringValueNode.class));
        assertThat(cvnp.parseValue(context, "kewl", "stuff"), instanceOf(NumberValueNode.class));
        assertThat(cvnp.parseValue(context, "krown", "stuff"), instanceOf(NumberValueNode.class));
        assertThat(cvnp.parseValue(context, "unknown", "stuff"), nullValue());
    }

}
