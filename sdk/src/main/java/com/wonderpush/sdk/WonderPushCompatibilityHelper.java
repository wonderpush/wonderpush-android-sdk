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

}
