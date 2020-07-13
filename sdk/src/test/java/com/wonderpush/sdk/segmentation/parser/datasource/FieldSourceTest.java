package com.wonderpush.sdk.segmentation.parser.datasource;

import com.wonderpush.sdk.segmentation.parser.FieldPath;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldSourceTest {

    @Test
    public void testItShouldGetFullPath() {
        assertThat(new FieldSource(new FieldSource(new InstallationSource(), new FieldPath(new String[]{"a", "b"})), new FieldPath(new String[]{"c", "d"})).fullPath().parts, equalTo(new String[]{"a", "b", "c", "d"}));
    }

}
