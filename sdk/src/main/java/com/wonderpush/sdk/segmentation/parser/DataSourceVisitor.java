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

public interface DataSourceVisitor<T> {

    public T visitUserSource(UserSource dataSource);

    public T visitInstallationSource(InstallationSource dataSource);

    public T visitEventSource(EventSource dataSource);

    public T visitFieldSource(FieldSource dataSource);

    public T visitLastActivityDateSource(LastActivityDateSource dataSource);

    public T visitPresenceSinceDateSource(PresenceSinceDateSource dataSource);

    public T visitPresenceElapsedTimeSource(PresenceElapsedTimeSource dataSource);

    public T visitGeoLocationSource(GeoLocationSource dataSource);

    public T visitGeoDateSource(GeoDateSource dataSource);

}
