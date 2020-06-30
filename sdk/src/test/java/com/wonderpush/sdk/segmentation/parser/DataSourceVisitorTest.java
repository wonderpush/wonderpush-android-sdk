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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class DataSourceVisitorTest {

    private enum Markers {
        UserSource,
        InstallationSource,
        EventSource,
        FieldSource,
        LastActivityDateSource,
        PresenceSinceDateSource,
        PresenceElapsedTimeSource,
        GeoLocationSource,
        GeoDateSource,
    }

    private static class TestRecordingDataSourceVisitor implements DataSourceVisitor<Markers> {

        public final List<DataSource> seenObjects = new ArrayList<>();

        @Override
        public Markers visitUserSource(UserSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.UserSource;
        }

        @Override
        public Markers visitInstallationSource(InstallationSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.InstallationSource;
        }

        @Override
        public Markers visitEventSource(EventSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.EventSource;
        }

        @Override
        public Markers visitFieldSource(FieldSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.FieldSource;
        }

        @Override
        public Markers visitLastActivityDateSource(LastActivityDateSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.LastActivityDateSource;
        }

        @Override
        public Markers visitPresenceSinceDateSource(PresenceSinceDateSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.PresenceSinceDateSource;
        }

        @Override
        public Markers visitPresenceElapsedTimeSource(PresenceElapsedTimeSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.PresenceElapsedTimeSource;
        }

        @Override
        public Markers visitGeoLocationSource(GeoLocationSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.GeoLocationSource;
        }

        @Override
        public Markers visitGeoDateSource(GeoDateSource dataSource) {
            this.seenObjects.add(dataSource);
            return Markers.GeoDateSource;
        }

    }

    public static final InstallationSource installationSource = new InstallationSource();

    @Parameterized.Parameters(name = "{index}: Test that {0} is properly visited")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        new EventSource(),
                        Markers.EventSource,
                },
                {
                        new FieldSource(installationSource, new FieldPath(new String[]{"foo"})),
                        Markers.FieldSource,
                },
                {
                        new GeoDateSource(installationSource),
                        Markers.GeoDateSource,
                },
                {
                        new GeoLocationSource(installationSource),
                        Markers.GeoLocationSource,
                },
                {
                        new InstallationSource(),
                        Markers.InstallationSource,
                },
                {
                        new LastActivityDateSource(installationSource),
                        Markers.LastActivityDateSource,
                },
                {
                        new PresenceElapsedTimeSource(installationSource, true),
                        Markers.PresenceElapsedTimeSource,
                },
                {
                        new PresenceSinceDateSource(installationSource, true),
                        Markers.PresenceSinceDateSource,
                },
                {
                        new UserSource(),
                        Markers.UserSource,
                }
        });
    }

    public final DataSource dataSource;
    public final Markers expectedMarker;

    public DataSourceVisitorTest(DataSource dataSource, Markers expectedMarker) {
        this.dataSource = dataSource;
        this.expectedMarker = expectedMarker;
    }

    @Test
    public void testItShouldCallAppropriateMethods() {
        TestRecordingDataSourceVisitor visitor = new TestRecordingDataSourceVisitor();
        assertThat(dataSource.accept(visitor), sameInstance(expectedMarker));
        assertThat(visitor.seenObjects.size(), is(1));
        assertThat(visitor.seenObjects.get(0), sameInstance(dataSource));
    }

}
