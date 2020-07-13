package com.wonderpush.sdk.segmentation.parser;

import com.wonderpush.sdk.segmentation.parser.datasource.EventSource;
import com.wonderpush.sdk.segmentation.parser.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.parser.datasource.GeoDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.GeoLocationSource;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.parser.datasource.LastActivityDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceElapsedTimeSource;
import com.wonderpush.sdk.segmentation.parser.datasource.PresenceSinceDateSource;
import com.wonderpush.sdk.segmentation.parser.datasource.UserSource;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DataSourceTest {

    @Test
    public void testItShouldProduceGoodSourceNames() {
        assertThat(new FieldSource(new UserSource(), new FieldPath(new String[]{"foo", "bar"})).getName(), is("user.foo.bar"));
        assertThat(new FieldSource(new InstallationSource(), new FieldPath(new String[]{"foo", "bar"})).getName(), is("installation.foo.bar"));
        assertThat(new FieldSource(new FieldSource(new InstallationSource(), new FieldPath(new String[]{"foo"})), new FieldPath(new String[]{"bar"})).getName(), is("installation.foo.bar"));
        assertThat(new FieldSource(new EventSource(), new FieldPath(new String[]{"foo", "bar"})).getName(), is("event.foo.bar"));
        assertThat(new LastActivityDateSource(new InstallationSource()).getName(), is("lastActivityDate"));
        assertThat(new PresenceSinceDateSource(new InstallationSource(), false).getName(), is("presence.sinceDate"));
        assertThat(new PresenceElapsedTimeSource(new InstallationSource(), false).getName(), is("presence.elapsedTime"));
        assertThat(new GeoLocationSource(new InstallationSource()).getName(), is("geo.location"));
        assertThat(new GeoDateSource(new InstallationSource()).getName(), is("geo.date"));
    }

}
