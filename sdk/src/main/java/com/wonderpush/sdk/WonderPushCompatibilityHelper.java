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

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void ViewSetBackground(View view, Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(background);
        } else {
            view.setBackgroundDrawable(background);
        }
    }

    @SuppressWarnings("deprecation")
    public static void WebViewSettingsSetDatabasePath(WebView webView, String path) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webView.getSettings().setDatabasePath(path);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    public static int getIntentFlagActivityNewDocument() {
        if (Build.VERSION.SDK_INT >= 21) {
            return Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        } else {
            return Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static int getPendingIntentFlagImmutable() {
        if (Build.VERSION.SDK_INT >= 23) {
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

    @SuppressWarnings("deprecation")
    public static void removeOnGlobalLayoutListener(ViewTreeObserver vto, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            vto.removeGlobalOnLayoutListener(listener);
        } else {
            vto.removeOnGlobalLayoutListener(listener);
        }
    }

}
