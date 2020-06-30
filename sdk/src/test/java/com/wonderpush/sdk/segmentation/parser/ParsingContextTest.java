package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.datasource.EventSource;
import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParsingContextTest {

    @Test
    public void testItShouldConstructAChildContextForWithDataSource() {
        ParsingContext contextRoot = new ParsingContext(SegmentationFactory.getDefaultParser(), null, new InstallationSource());
        assertThat(contextRoot.parentContext, nullValue());
        assertThat(contextRoot.dataSource, instanceOf(InstallationSource.class));
        ParsingContext contextChild = contextRoot.withDataSource(new EventSource());
        assertThat(contextChild.parentContext, sameInstance(contextRoot));
        assertThat(contextChild.dataSource, instanceOf(EventSource.class));
        ParsingContext contextGrandChild = contextChild.withDataSource(new FieldSource(contextChild.dataSource, new FieldPath(new String[]{"a", "b"})));
        assertThat(contextGrandChild.parentContext, sameInstance(contextChild));
        assertThat(contextGrandChild.dataSource, instanceOf(FieldSource.class));
    }

}
