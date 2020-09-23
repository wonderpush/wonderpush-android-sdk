package com.wonderpush.sdk.remoteconfig;

import android.content.Context;
import android.content.SharedPreferences;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class SharedPreferencesRemoteConfigStorage implements RemoteConfigStorage {
    private final String sharedPreferencesFilename;
    private final Context applicationContext;
    public SharedPreferencesRemoteConfigStorage(@Nonnull String clientId, @Nonnull Context context) {
        sharedPreferencesFilename = String.format("wonderpushremoteconfig_%s", clientId);
        applicationContext = context.getApplicationContext();
    }

    public String getSharedPreferencesFilename() {
        return sharedPreferencesFilename;
    }

    @Override
    public void storeRemoteConfig(@Nonnull RemoteConfig config, @Nullable ErrorHandler handler) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(sharedPreferencesFilename, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("config", config.toString());
        editor.commit();
        if (handler != null) handler.handle(null);
    }

    @Override
    public void loadRemoteConfigAndHighestDeclaredVersion(RemoteConfigVersionHandler handler) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(sharedPreferencesFilename, Context.MODE_PRIVATE);
        String configString = sharedPreferences.getString("config", null);
        RemoteConfig config = configString != null ? RemoteConfig.fromString(configString) : null;

        Set<String> versions = sharedPreferences.getStringSet("versions", new HashSet<>());
        String highestVersion = null;
        for (String version : versions) {
            if (highestVersion == null) {
                highestVersion = version;
                continue;
            }
            if (RemoteConfig.compareVersions(version, highestVersion) > 0) {
                highestVersion = version;
            }
        }
        if (handler != null) handler.handle(config, highestVersion, null);
    }

    @Override
    public void declareVersion(@Nonnull String version, @Nullable ErrorHandler handler) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(sharedPreferencesFilename, Context.MODE_PRIVATE);
        Set<String> declaredVersions = sharedPreferences.getStringSet("versions", new HashSet<>());
        declaredVersions.add(version);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("versions", declaredVersions);
        editor.commit();
        if (handler != null) handler.handle(null);
    }
}
