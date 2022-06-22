package com.wonderpush.sdk.inappmessaging.display.internal;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.Patterns;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.inappmessaging.display.internal.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class InAppWebViewBridge {
    static final String JAVASCRIPT_INTERFACE_NAME = "_wpiam";

    private static final String ARRAY_RETURN_TYPE_PREFIX = "__array__";
    private static final String OBJECT_RETURN_TYPE_PREFIX = "__object__";
    private static final String OBJECT_ARG_TYPE_PREFIX = "__object__";
    private static final String NUMBER_ARG_TYPE_PREFIX = "__number__";
    private static final String BOOLEAN_ARG_TYPE_PREFIX = "__boolean__";
    public interface Callbacks {
        void onDismiss();
        void onClick(String buttonLabel);
    }
    private final WeakReference<WebView> webViewRef;
    private final WeakReference<Activity> activityRef;
    private final Callbacks callbacks;
    public final WebViewClient webViewClient;

    public InAppWebViewBridge(WebView webView, Activity activity, Callbacks callbacks) {
        webViewRef = new WeakReference<>(webView);
        activityRef = new WeakReference<>(activity);
        this.callbacks = callbacks;
        webViewClient = new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                view.removeJavascriptInterface(JAVASCRIPT_INTERFACE_NAME);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith("http:") && !url.startsWith("https:")) {
                    openDeepLink(url);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        };
        if (webView != null) {
            webView.addJavascriptInterface(this, JAVASCRIPT_INTERFACE_NAME);
            // Set up a new webView client that will disable the Javascript interface after first navigation
            webView.setWebViewClient(webViewClient);
            // Support target="_blank"
            webView.getSettings().setSupportMultipleWindows(true);
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                    if (!isDialog && isUserGesture) {
                        WebView.HitTestResult hitTestResult = view.getHitTestResult();
                        if (hitTestResult.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                            String url = hitTestResult.getExtra();
                            openExternalUrl(url);
                            return true;
                        }
                    }
                    return false;
                }
            });

        }
    }

    private void logException(Exception exception) {
        Logging.loge(exception.getLocalizedMessage(), exception);
    }

    private String toJavascriptResultArray(JSONArray a) {
        return ARRAY_RETURN_TYPE_PREFIX + a.toString();
    }

    private String toJavascriptResultObject(JSONObject a) {
        return OBJECT_RETURN_TYPE_PREFIX + a.toString();
    }

    private @Nullable Object argToObject(String arg) {
        return argToObject(arg, null);
    }

    private @Nullable Object argToObject(String arg, Object defaultValue) {
        if (arg == null) return null;
        if (arg.startsWith(BOOLEAN_ARG_TYPE_PREFIX)) {
            return arg.equals("__boolean__true");
        }
        if (arg.startsWith(NUMBER_ARG_TYPE_PREFIX)) {
            try {
                return NumberFormat.getInstance().parse(arg.substring(NUMBER_ARG_TYPE_PREFIX.length()));
            } catch (ParseException e) {
                return null;
            }
        }
        if (arg.startsWith(OBJECT_ARG_TYPE_PREFIX)) {
            String sub = arg.substring(OBJECT_ARG_TYPE_PREFIX.length());
            try {
                return new JSONObject(sub);
            } catch (JSONException e) {
                try {
                    return new JSONArray(sub);
                } catch (JSONException e1) {
                    return null;
                }
            }
        }
        return arg;
    }

    private @NonNull <T> List<T> argToList(String arg) {
        List<T> result = new ArrayList<>();
        JSONArray array = argToJSONArray(arg, new JSONArray());
        for (int i = 0; i < array.length(); i++) {
            Object elt = array.opt(i);
            if (elt != null) {
                try {
                    result.add((T)elt);
                } catch (ClassCastException e) {}
            }
        }
        return result;
    }

    private @Nullable Number argToNumber(String arg) {
        return argToNumber(arg, null);
    }

    private @Nullable Number argToNumber(String arg, Number defaultValue) {
        Object result = argToObject(arg);
        return result instanceof Number ? (Number)result : defaultValue;
    }

    private @Nullable JSONObject argToJSONObject(String arg) {
        return argToJSONObject(arg, null);
    }

    private @Nullable JSONObject argToJSONObject(String arg, JSONObject defaultValue) {
        Object result = argToObject(arg);
        return result instanceof JSONObject ? (JSONObject)result : defaultValue;
    }

    private @Nullable JSONArray argToJSONArray(String arg) {
        return argToJSONArray(arg, null);
    }

    private @Nullable JSONArray argToJSONArray(String arg, JSONArray defaultValue) {
        Object result = argToObject(arg);
        return result instanceof JSONArray ? (JSONArray)result : defaultValue;
    }

    private @Nullable JSONObject parseJSONObject(String s, JSONObject defaultValue) {
        if (s == null) return defaultValue;
        try {
            return new JSONObject(s);
        } catch (JSONException e) {
            return defaultValue;
        }
    }

    private @Nullable JSONArray parseJSONArray(String s, JSONArray defaultValue) {
        if (s == null) return defaultValue;
        try {
            return new JSONArray(s);
        } catch (JSONException e) {
            return defaultValue;
        }
    }

    private void throwJavascriptError(String msg) {
        final WebView webView = webViewRef.get();
        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    String js = String.format(Locale.ENGLISH, "console.error(%s)", JSONObject.quote(msg));
                    String console = "javascript:" + Uri.encode(js);
                    webView.loadUrl(console);
                }
            });
        }
        throw new RuntimeException(msg);
    }

    @JavascriptInterface
    public void dismiss() {
        if (callbacks != null) callbacks.onDismiss();
    }

    @JavascriptInterface
    public void trackClick(String buttonLabel) {
        if (callbacks != null) callbacks.onClick(buttonLabel);
    }

    @JavascriptInterface
    public void openExternalUrl(String urlString) {
        final WebView webView = webViewRef.get();
        Uri uri = Uri.parse(urlString);

        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    dismiss();
                    Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER);
                    intent.setData(uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        webView.getContext().startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Logging.loge("No activity for intent " + intent, e);
                    }
                }
            });
        }
    }

    @JavascriptInterface
    public void openDeepLink(String urlString) {
        Uri uri = Uri.parse(urlString);
        final WebView webView = webViewRef.get();
        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    dismiss();
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        webView.getContext().startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Logging.loge("No activity for intent " + intent, e);
                    }
                }
            });
        }
    }

    @JavascriptInterface
    public void subscribeToNotifications() {
        WonderPush.subscribeToNotifications();
    }

    @JavascriptInterface
    public void unsubscribeFromNotifications() {
        WonderPush.unsubscribeFromNotifications();
    }

    @JavascriptInterface
    public boolean isSubscribedToNotifications() {
        return WonderPush.isSubscribedToNotifications();
    }

    @JavascriptInterface
    public String getUserId() {
        return WonderPush.getUserId();
    }

    @JavascriptInterface
    public String getInstallationId() {
        return WonderPush.getInstallationId();
    }

    @JavascriptInterface
    public String getCountry() {
        return WonderPush.getCountry();
    }

    @JavascriptInterface
    public String getCurrency() {
        return WonderPush.getCurrency();
    }

    @JavascriptInterface
    public String getLocale() {
        return WonderPush.getLocale();
    }

    @JavascriptInterface
    public String getTimeZone() {
        return WonderPush.getTimeZone();
    }

    @JavascriptInterface
    public void trackEvent(String type) {
        WonderPush.trackEvent(type);
    }

    @JavascriptInterface
    public void trackEvent(String type, String attributeString) {
        JSONObject attributes = argToJSONObject(attributeString, new JSONObject());
        WonderPush.trackEvent(type, attributes);
    }

    @JavascriptInterface
    public void addTag(String t1) {
        List<String> tags = argToList(t1);
        String[] tagArray = tags.toArray(new String[tags.size()]);
        WonderPush.addTag(tagArray);
    }

    @JavascriptInterface
    public void removeTag(String t1) {
        List<String> tags = argToList(t1);
        String[] tagArray = tags.toArray(new String[tags.size()]);
        WonderPush.removeTag(tagArray);
    }

    @JavascriptInterface
    public void removeAllTags() {
        WonderPush.removeAllTags();
    }

    @JavascriptInterface
    public boolean hasTag(String tag) {
        return WonderPush.hasTag(tag);
    }

    @JavascriptInterface
    public String getTags() {
        Set<String> tags = WonderPush.getTags();
        return toJavascriptResultArray(new JSONArray(tags));
    }

    @JavascriptInterface
    public Object getPropertyValue(String field) {
        Object result = WonderPush.getPropertyValue(field);
        if (result == JSONObject.NULL) return null;
        return result;
    }

    @JavascriptInterface
    public String getPropertyValues(String field) {
        return toJavascriptResultArray(new JSONArray(WonderPush.getPropertyValues(field)));
    }

    @JavascriptInterface
    public void addProperty(String field, String valString) {
        Object val = argToObject(valString, null);
        WonderPush.addProperty(field, val);
    }

    @JavascriptInterface
    public void removeProperty(String field, String encodedValue) {
        Object val = argToObject(encodedValue, null);
        WonderPush.removeProperty(field, val);
    }

    @JavascriptInterface
    public void setProperty(String field, String encodedValue) {
        Object val = argToObject(encodedValue, null);
        WonderPush.setProperty(field, val);
    }

    @JavascriptInterface
    public void unsetProperty(String field) {
        WonderPush.unsetProperty(field);
    }

    @JavascriptInterface
    public void putProperties(String encodedProperties) {
        JSONObject properties = argToJSONObject(encodedProperties);
        if (properties != null) {
            WonderPush.putProperties(properties);
        }
    }

    @JavascriptInterface
    public String getProperties() {
        return toJavascriptResultObject(WonderPush.getProperties());
    }

    private static int checkSelfPermission(@NonNull Context context, @NonNull String permission) {
        // Catch for rare "Unknown exception code: 1 msg null" exception
        // See https://github.com/one-signal/OneSignal-Android-SDK/issues/48 for more details.
        try {
            return context.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid());
        } catch (Throwable t) {
            Logging.loge("checkSelfPermission failed, returning PERMISSION_DENIED");
            return PackageManager.PERMISSION_DENIED;
        }
    }
    @JavascriptInterface
    public void triggerLocationPrompt() {
        Activity activity = this.activityRef.get();
        if (activity == null) {
            throwJavascriptError("Activity not available");
            return;
        }
        Context context = activity;

        int coarsePermission = checkSelfPermission(context, "android.permission.ACCESS_COARSE_LOCATION");
        int finePermission = checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (finePermission != PackageManager.PERMISSION_GRANTED && coarsePermission != PackageManager.PERMISSION_GRANTED) {
                throwJavascriptError("Permissions missing in manifest");
            }
            // Granted
            return;
        }

        if (coarsePermission == PackageManager.PERMISSION_GRANTED || finePermission == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<String> permissionsToRequest = new ArrayList<>();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            List<String> permissionList = Arrays.asList(packageInfo.requestedPermissions);
            if (permissionList.contains("android.permission.ACCESS_FINE_LOCATION")) {
                permissionsToRequest.add("android.permission.ACCESS_FINE_LOCATION");
            } else if (permissionList.contains("android.permission.ACCESS_COARSE_LOCATION")) {
                permissionsToRequest.add("android.permission.ACCESS_COARSE_LOCATION");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throwJavascriptError("Error: " + e.getMessage());
        }
        if (permissionsToRequest.size() > 0) {
            activity.requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), 123);
        }
    }
}
