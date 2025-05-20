package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannelGroup;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class WonderPushCompatibilityHelper {

    @TargetApi(Build.VERSION_CODES.M)
    public static int getPendingIntentFlagImmutable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    public static int getColorResource(Resources resources, int identifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return resources.getColor(identifier, null);
        } else {
            return resources.getColor(identifier);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    @SuppressWarnings("deprecation")
    public static CharSequence fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, 0); // FROM_HTML_MODE_LEGACY
        } else {
            return Html.fromHtml(source);
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    public static boolean isNotificationChannelGroupBlocked(NotificationChannelGroup group) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return group != null && group.isBlocked();
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> void sort(List<E> list, Comparator<? super E> c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list.sort(c);
        } else {
            // Code taken from java.util.List.sort default implementation
            Object[] a = list.toArray();
            Arrays.sort(a, (Comparator) c);
            ListIterator<E> i = list.listIterator();
            for (Object e : a) {
                i.next();
                i.set((E) e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static <T extends Parcelable> T intentGetParcelableExtra(Intent intent, String name, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(name, clazz);
        } else {
            return intent.getParcelableExtra(name);
        }
    }

    @SuppressWarnings("deprecation")
    public static <T extends Parcelable> T parcelReadParcelable(Parcel parcel, ClassLoader loader, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return parcel.readParcelable(loader, clazz);
        } else {
            return parcel.readParcelable(loader);
        }
    }

    @SuppressWarnings("deprecation")
    public static <T extends Parcelable> T[] intentGetParcelableArrayExtra(Intent intent, String name, Class<T> clazz, T[] into) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableArrayExtra(name, clazz);
        } else {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(name);
            if (parcelables == null) return null;
            ArrayList<T> typedList = new ArrayList<>(parcelables.length);
            for (Parcelable parcelable : parcelables) {
                if (clazz.isInstance(parcelable)) {
                    typedList.add(clazz.cast(parcelable));
                }
            }
            return typedList.toArray(into);
        }
    }

    @SuppressWarnings("deprecation")
    public static Object bundleGetTypeUnsafe(Bundle bundle, String key) {
        return bundle.get(key);
    }

    @SuppressWarnings("deprecation")
    public static Bundle getApplicationInfoMetaData(Context context) throws PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)).metaData;
        } else {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
        }
    }

    @SuppressWarnings("deprecation")
    public static Bundle getActivityInfoMetaData(Activity activity) throws PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return activity.getPackageManager().getActivityInfo(activity.getComponentName(), PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA)).metaData;
        } else {
            return activity.getPackageManager().getActivityInfo(activity.getComponentName(), PackageManager.GET_META_DATA).metaData;
        }
    }

    @SuppressWarnings("deprecation")
    public static ApplicationInfo getApplicationInfo(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.ApplicationInfoFlags.of(0));
            } else {
                return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(WonderPush.TAG, "Failed to get PackageManager.getApplicationInfo("+context.getPackageName()+", 0)", e);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static PackageInfo getPackageInfo(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(WonderPush.TAG, "Failed to get PackageManager.getPackageInfo("+context.getPackageName()+", 0)", e);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static PackageInfo getPackageInfoPermissions(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
            } else {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(WonderPush.TAG, "Failed to get PackageManager.getPackageInfo("+context.getPackageName()+", PackageManager.GET_PERMISSIONS)", e);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static long getPackageInfoVersionCode(PackageInfo packageInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        } else {
            return packageInfo.versionCode;
        }
    }

}
