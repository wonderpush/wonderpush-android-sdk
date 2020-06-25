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
