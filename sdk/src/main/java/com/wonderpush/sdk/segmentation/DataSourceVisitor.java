package com.wonderpush.sdk.segmentation;

interface DataSourceVisitor<T> {

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
