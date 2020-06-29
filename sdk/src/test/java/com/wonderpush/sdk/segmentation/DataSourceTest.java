package com.wonderpush.sdk.segmentation;

import com.wonderpush.sdk.segmentation.datasource.EventSource;
import com.wonderpush.sdk.segmentation.datasource.FieldSource;
import com.wonderpush.sdk.segmentation.datasource.GeoDateSource;
import com.wonderpush.sdk.segmentation.datasource.GeoLocationSource;
import com.wonderpush.sdk.segmentation.datasource.InstallationSource;
import com.wonderpush.sdk.segmentation.datasource.LastActivityDateSource;
import com.wonderpush.sdk.segmentation.datasource.PresenceElapsedTimeSource;
import com.wonderpush.sdk.segmentation.datasource.PresenceSinceDateSource;
import com.wonderpush.sdk.segmentation.datasource.UserSource;

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
