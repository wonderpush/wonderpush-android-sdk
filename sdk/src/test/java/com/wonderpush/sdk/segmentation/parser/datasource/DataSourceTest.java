package com.wonderpush.sdk.segmentation.parser.datasource;

import com.wonderpush.sdk.segmentation.parser.FieldPath;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DataSourceTest {

    @Test
    public void testItShouldProduceGoodSourceNames() {
        assertThat(new FieldSource(new UserSource(), new FieldPath(new String[]{"foo", "bar"})).getName(), equalTo("user.foo.bar"));
        assertThat(new FieldSource(new InstallationSource(), new FieldPath(new String[]{"foo", "bar"})).getName(), equalTo("installation.foo.bar"));
        assertThat(new FieldSource(new FieldSource(new InstallationSource(), new FieldPath(new String[]{"foo"})), new FieldPath(new String[]{"bar"})).getName(), equalTo("installation.foo.bar"));
        assertThat(new FieldSource(new EventSource(), new FieldPath(new String[]{"foo", "bar"})).getName(), equalTo("event.foo.bar"));
        assertThat(new LastActivityDateSource(new InstallationSource()).getName(), equalTo("lastActivityDate"));
        assertThat(new PresenceSinceDateSource(new InstallationSource(), false).getName(), equalTo("presence.sinceDate"));
        assertThat(new PresenceElapsedTimeSource(new InstallationSource(), false).getName(), equalTo("presence.elapsedTime"));
        assertThat(new GeoLocationSource(new InstallationSource()).getName(), equalTo("geo.location"));
        assertThat(new GeoDateSource(new InstallationSource()).getName(), equalTo("geo.date"));
    }

}
