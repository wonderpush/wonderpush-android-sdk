package com.wonderpush.sdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannelGroup;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;

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


}
