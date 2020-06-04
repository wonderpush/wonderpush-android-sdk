package com.wonderpush.sdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WonderPushSettings {

    private static final String TAG = "WonderPush.Settings";
    private static Map<String, Object> sBuildConfigValues = new HashMap<String, Object>();
    private static Context sContext = null;
    private static Bundle sMetaData = null;
    private static boolean sFoundBuildConfig = false;
    private static Resources sResources = null;

    static void initialize(Context context) {
        if (sContext != null) return; // already initialized
        sContext = context;
        sResources = context.getResources();

        try {
            sMetaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to read application meta-data", e);
        }

        // Try to locate the BuildConfig class.
        // The difficulty being that it's in the Java package configured as `defaultConfig.applicationId` in gradle,
        // but does is not affected by `applicationIdSuffix` or flavors' `applicationId`, whereas Context.getPackageName() is.
        // - In the easiest case where applicationId is only given in `defaultConfig` in the app's build.gradle
        //   and there is no `applicationIdSuffix`, and we can use context.getPackageName();
        // - Otherwise, the application class is probably from the Java package, although that's not mandatory;
        // - Finally, we allow the developer to give us the value using a manifest metadata or a string resource.
        String buildConfigPackageGiven = null;
        {
            // Try using resources
            try {
                Resources resources = context.getResources();
                int res;
                String resString;
                res = resources.getIdentifier("wonderpush_buildConfigPackage", "string", context.getPackageName());
                resString = res == 0 ? null : resources.getString(res);
                if (!TextUtils.isEmpty(resString)) {
                    buildConfigPackageGiven = resString;
                    if (WonderPush.getLogging())
                        Log.d(TAG, "wonderpush_buildConfigPackage resource: " + buildConfigPackageGiven);
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not get a WonderPush configuration resource", e);
            }
            // Try using the manifest <application><meta-data>
            Object resValue = sMetaData.get("com.wonderpush.sdk.buildConfigPackage");
            if (resValue instanceof String && ((String) resValue).length() > 0) {
                buildConfigPackageGiven = (String) resValue;
                if (WonderPush.getLogging())
                    Log.d(TAG, "com.wonderpush.sdk.buildConfigPackage metadata: " + buildConfigPackageGiven);
            }
        }
        List<String> buildConfigPackagesToTry = new ArrayList<>();
        if (buildConfigPackageGiven != null) {
            buildConfigPackagesToTry.add(buildConfigPackageGiven);
        } else {
            buildConfigPackagesToTry.add(context.getPackageName());
            buildConfigPackagesToTry.add(context.getApplicationContext().getClass().getPackage().getName());
        }

        // Try loading configuration using the BuildConfig values for good security (using code-stored constants)
        // BuildConfig is first because it's hard to view them (needs a decompiler)
        for (String buildConfigPackage : buildConfigPackagesToTry) {
            String className = buildConfigPackage + ".BuildConfig";
            try {
                Class<?> classClass = context.getClassLoader().loadClass(className);
                if (WonderPush.getLogging()) Log.d(TAG, "Reading configuration from " + className);
                for (Field f : classClass.getFields()) {
                    if (!f.getName().startsWith("WONDERPUSH_")) continue;
                    try {
                        sBuildConfigValues.put(f.getName(), f.get(null));
                    } catch (Exception e) { // IllegalAccessException on non public field
                        Log.e(TAG, "Could not get BuildConfig field " + f.getName(), e);
                    }
                }
                sFoundBuildConfig = true;
                break;
            } catch (ClassNotFoundException e) {
                if (WonderPush.getLogging())
                    Log.d(TAG, "No " + className + " class found. ProGuard may have removed it after finding no WONDERPUSH_* constants.");
            }
        }
    }

    public static String getString(String buildConfigFieldName, String resourceName, String metaDataName) {
        String rtn = null;
        Object buildConfigValue = sBuildConfigValues.get(buildConfigFieldName);
        if (buildConfigValue instanceof String) {
            rtn = (String) buildConfigValue;
        }

        // Try loading configuration using resources
        // Resources are second because they are not easily viewable with tools like Dexplorer
        try {
            int res;
            String resString;
            res = sResources.getIdentifier(resourceName, "string", sContext.getPackageName());
            resString = res == 0 ? null : sResources.getString(res);
            if (!TextUtils.isEmpty(resString)) {
                rtn = resString;
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not get a WonderPush configuration resource", e);
        }

        if (sMetaData != null) {
            // Try loading configuration using the manifest <application><meta-data>
            // Metadata are last because they are easily viewable with tools like Dexplorer, hence it feels more natural that they override previous sources
            Object resValue;
            resValue = sMetaData.get(metaDataName);
            if (resValue instanceof String && ((String) resValue).length() > 0) {
                rtn = (String) resValue;
            }
        }

        return rtn;
    }

    public static Boolean getBoolean(String buildConfigFieldName, String resourceName, String metaDataName) {
        Boolean rtn = null;
        Object buildConfigValue = sBuildConfigValues.get(buildConfigFieldName);
        if (buildConfigValue instanceof Boolean) {
            rtn = (Boolean) buildConfigValue;
        }

        // Try loading configuration using resources
        // Resources are second because they are not easily viewable with tools like Dexplorer
        try {
            int res;
            String resString;
            res = sResources.getIdentifier(resourceName, "bool", sContext.getPackageName());
            if (res != 0) {
                rtn = sResources.getBoolean(res);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not get a WonderPush configuration resource", e);
        }

        if (sMetaData != null) {
            // Try loading configuration using the manifest <application><meta-data>
            // Metadata are last because they are easily viewable with tools like Dexplorer, hence it feels more natural that they override previous sources
            Object resValue;
            resValue = sMetaData.get(metaDataName);
            if (resValue instanceof Boolean) {
                rtn = (Boolean) resValue;
            } else if ("true".equals(resValue) || "false".equals(resValue)) {
                rtn = "true".equals(resValue);
            }
        }

        return rtn;
    }

    public static boolean getBuildConfigFound() {
        return sFoundBuildConfig;
    }

}
