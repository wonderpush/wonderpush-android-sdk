package com.wonderpush.sdk;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * WonderPush SDK automatic initialization provider.
 *
 * <p>
 *     Note that this feature does not work for multi-process applications.
 *     You must implement your own {@link android.app.Application} class and call
 *     {@link WonderPush#initialize(Context)} there in such apps.
 * </p>
 */
public class WonderPushInitProvider extends ContentProvider {

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        if (info == null) {
            throw new NullPointerException("WonderPushInitProvider ProviderInfo is null");
        }

        if ("com.wonderpush.sdk.wonderpush.initprovider".equals(info.authority)) {
            throw new IllegalStateException("WonderPushInitProvider authority is invalid. Check that you have an `android { defaultConfig { applicationId \"…\" } }` entry in your app/build.gradle");
        } else {
            super.attachInfo(context, info);
        }
    }

    @Override
    public boolean onCreate() {
        if (WonderPush.ensureInitialized(getContext(), true)) {
            WonderPush.logDebug("WonderPushInitProvider successful");
        } else {
            WonderPush.logDebug("WonderPushInitProvider not successful");
        }
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

}
