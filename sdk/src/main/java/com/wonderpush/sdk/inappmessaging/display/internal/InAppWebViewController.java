package com.wonderpush.sdk.inappmessaging.display.internal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;
import com.wonderpush.sdk.NotificationMetadata;
import com.wonderpush.sdk.R;
import com.wonderpush.sdk.SafeDeferProvider;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushCompatibilityHelper;
import com.wonderpush.sdk.inappmessaging.display.InAppMessagingDisplay;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.inappmessaging.model.WebViewMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * This class takes care of loading the webView and orchestrating its interactions
 * with the bridge and the rest of the SDK.
 */
public class InAppWebViewController implements InAppWebViewBridge.Controller {

    static final String HTML_INAPP_SDK_URL = "https://cdn.by.wonderpush.com/inapp-sdk/1/wonderpush-loader.min.js";
    static final long WEB_VIEW_LOAD_TIMEOUT_MILLIS = 10 * 1000; // timeout error after 10 seconds
    static final String JAVASCRIPT_INTERFACE_NAME = "_wpiam";

    final private WebViewMessage webViewMessage;
    final private WeakReference<WebView> webViewRef;
    final private WeakReference<Activity> activityRef;
    private Runnable onDismiss;
    private Consumer<String> onClick;
    final private SafeDeferProvider safeDeferProvider;
    final private InAppMessagingDisplay.TrackEventProvider trackEventProvider;
    final private InAppWebViewBridge bridge = new InAppWebViewBridge(this);

    public InAppWebViewController(
            InAppMessage inAppMessage,
            WebView webView,
            Activity activity,
            SafeDeferProvider safeDeferProvider,
            InAppMessagingDisplay.TrackEventProvider trackEventProvider) {
        webViewMessage = inAppMessage instanceof WebViewMessage ? (WebViewMessage) inAppMessage : null;
        webViewRef = new WeakReference<>(webView);
        activityRef = new WeakReference<>(activity);
        this.safeDeferProvider = safeDeferProvider;
        this.trackEventProvider = trackEventProvider;
    }

    public void setOnDismiss(Runnable r) {
        this.onDismiss = r;
    }

    public void setOnClick(Consumer<String> c) {
        this.onClick = c;
    }

    public void load(String url, Runnable onSuccess, Consumer<Throwable> onError) {
        WebView webView = webViewRef.get();
        if (url == null || webView == null) {
            onSuccess.run();
            return;
        }
        final Activity activity = activityRef.get();
        if (activity == null) {
            onError.accept(new Exception("Null activity"));
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Set our client
                WebViewClient client = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? new ModernClient(onSuccess, onError, activity.getResources()) : new LegacyClient(onSuccess, onError, activity.getResources());
                webView.setWebViewClient(client);
                webView.addJavascriptInterface(bridge, JAVASCRIPT_INTERFACE_NAME);
                // Support target="_blank"
                webView.getSettings().setSupportMultipleWindows(true);
                webView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                        if (!isDialog && isUserGesture) {
                            WebView.HitTestResult hitTestResult = view.getHitTestResult();
                            if (hitTestResult.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE
                                || hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                                String url = hitTestResult.getExtra();
                                openExternalUrl(url);
                                return true;
                            }
                        }
                        return false;
                    }
                });
                webView.loadUrl(url);
            }
            catch (Exception exception) {
                Logging.loge("Unexpected error loading webView", exception);
                if (onError != null) onError.accept(exception);
            }
        });

    }

    @Override
    public void sendError(String msg) {
        final WebView webView = webViewRef.get();
        if (webView != null) {
            webView.post(() -> {
                try {
                    String js = String.format(Locale.ENGLISH, "console.error(%s)", JSONObject.quote(msg));
                    String console = "javascript:" + Uri.encode(js);
                    webView.loadUrl(console);
                } catch (Exception ex) {
                    Logging.loge("Failed to send error to web view", ex);
                }
            });
        }
    }

    @Override
    public void openExternalUrl(String url) {
        final WebView webView = webViewRef.get();

        if (webView != null && url != null) {
            Uri uri = Uri.parse(url);
            webView.post(() -> {
                try {
                    dismiss();
                    Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER);
                    intent.setData(uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        webView.getContext().startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        try {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            webView.getContext().startActivity(browserIntent);
                        } catch (ActivityNotFoundException e1) {
                            Logging.loge("No activity for intent " + intent, e1);
                        }
                    }
                } catch (Exception ex) {
                    Logging.loge("Failed to open external url " + uri, ex);
                }
            });
        }
    }

    @Override
    public void openDeepLink(String url) {
        final WebView webView = webViewRef.get();
        if (webView != null && url != null) {
            Uri uri = Uri.parse(url);
            if (uri == null) return;
            webView.post(() -> {
                try {
                    dismiss();
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        webView.getContext().startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Logging.loge("No activity for intent " + intent, e);
                    }
                } catch (Exception ex) {
                    Logging.loge("Failed to open deep link " + uri, ex);
                }
            });
        }
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

    @Override
    public void triggerLocationPrompt() throws Exception {
        Activity activity = this.activityRef.get();
        if (activity == null) {
            throw new Exception("Activity not available");
        }
        Context context = activity;

        int coarsePermission = checkSelfPermission(context, "android.permission.ACCESS_COARSE_LOCATION");
        int finePermission = checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (finePermission != PackageManager.PERMISSION_GRANTED && coarsePermission != PackageManager.PERMISSION_GRANTED) {
                throw new Exception("Permissions missing in manifest");
            }
            // Granted
            return;
        }

        if (coarsePermission == PackageManager.PERMISSION_GRANTED || finePermission == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<String> permissionsToRequest = new ArrayList<>();
        PackageInfo packageInfo = WonderPushCompatibilityHelper.getPackageInfoPermissions(context);
        if (packageInfo != null) {
            List<String> permissionList = Arrays.asList(packageInfo.requestedPermissions);
            if (permissionList.contains("android.permission.ACCESS_FINE_LOCATION")) {
                permissionsToRequest.add("android.permission.ACCESS_FINE_LOCATION");
            } else if (permissionList.contains("android.permission.ACCESS_COARSE_LOCATION")) {
                permissionsToRequest.add("android.permission.ACCESS_COARSE_LOCATION");
            }
        }
        if (permissionsToRequest.size() > 0) {
            activity.requestPermissions(permissionsToRequest.toArray(new String[0]), 123);
        }
    }

    @Override
    public void dismiss() {
        if (onDismiss != null) onDismiss.run();
    }

    @Override
    public void trackClick(String buttonLabel) {
        if (onClick != null) onClick.accept(buttonLabel);
    }

    @Override
    public JSONObject getPayload() {
        return this.webViewMessage.getData();
    }

    @Override
    public void openAppRating() {
        Activity activity = activityRef.get();
        if (activity == null) return;
        ReviewManager manager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Activity activity1 = activityRef.get();
                if (activity1 == null) return;
                Task<Void> flow = manager.launchReviewFlow(activity1, reviewInfo);
                flow.addOnCompleteListener(task1 -> {
                    if (!task1.isSuccessful()) {
                        Logging.loge("Could not open app rating", task1.getException());
                    }
                });
            } else {
                Logging.loge("Could not get review info", task.getException());
            }
        });
    }

    @Override
    public void callMethod(String methodName, String methodArg) {
        WebView webView = this.webViewRef.get();
        if (webView == null) return;
        Context context = webView.getContext().getApplicationContext();
        Intent intent = new Intent();
        intent.setPackage(context.getPackageName());
        intent.setAction(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_ACTION);
        intent.setData(new Uri.Builder()
                .scheme(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_SCHEME)
                .authority(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_AUTHORITY)
                .appendPath(methodName)
                .build());
        intent.putExtra(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_METHOD, methodName);
        intent.putExtra(WonderPush.INTENT_NOTIFICATION_BUTTON_ACTION_METHOD_EXTRA_ARG, methodArg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void trackEvent(String type) {
        this.trackEvent(type, null);
    }

    @Override
    public void trackEvent(String type, @Nullable JSONObject attributes) {
        JSONObject eventData = new JSONObject();
        NotificationMetadata metadata = webViewMessage.getNotificationMetadata();
        if (metadata != null) {
            try {
                metadata.fill(eventData, NotificationMetadata.AttributionReason.INAPP_VIEWED);
            } catch (JSONException e) {
                Logging.loge("Could not serialize notification metadata", e);
            }
        }
        trackEventProvider.trackInAppEvent(type, eventData, attributes);
    }

    @Override
    public void setClipPath(Rect clipPath) {
        WebView webView = this.webViewRef.get();
        if (webView == null) return;
        if (webView instanceof InAppWebView) {
            ((InAppWebView)webView).setClipPath(clipPath);
        }
    }

    public static class WebViewLoadingException extends Exception {
        public WebViewLoadingException(Exception wrapped) {
            super(wrapped != null ? wrapped.getMessage() : null);
        }
    }

    private class BaseClient extends WebViewClient {

        final private Runnable onSuccess;
        final private Consumer<Throwable> onError;
        final private Resources resources;

        BaseClient(Runnable onSuccess, Consumer<Throwable> onError, Resources resources) {
            this.onError = onError;
            this.onSuccess = onSuccess;
            this.resources = resources;
        }

        private boolean initialLoadDone = false;

        protected void initialLoadSuccess() {
            if (initialLoadDone) {
                return;
            }
            initialLoadDone = true;
            if (onSuccess != null) onSuccess.run();
        }

        protected void initialLoadError(Exception err) {
            if (initialLoadDone) {
                return;
            }
            initialLoadDone = true;
            if (onError != null) onError.accept(new WebViewLoadingException(err));
        }

        @SuppressLint({"RequiresFeature", "NewApi"})
        @Override
        public void onPageStarted(final WebView webView, String url, Bitmap favicon) {
            try {
                super.onPageStarted(webView, url, favicon);

                safeDeferProvider.safeDefer(() -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            initialLoadError(new Exception("WebView timeout reached"));
                        } catch (Exception e) {
                            Logging.loge("Unexpected error", e);
                        }
                    });
                }, WEB_VIEW_LOAD_TIMEOUT_MILLIS);

            } catch (Exception exception) {
                Logging.loge(exception.getLocalizedMessage());
            }
        }

        protected InputStream getJavascriptSDKInputStream() {
            return this.resources.openRawResource(R.raw.wonderpush_inapp_sdk);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // On subsequent page loads, set the Javascript interface if the origin is the same
            // as the original inApp, otherwise, remove the interface
            WebView webView = webViewRef.get();
            if (webView != null) {
                if (webViewMessage != null
                        && webViewMessage.getWebViewUrl() != null
                        && url != null) {
                    Uri inAppMessageUri = Uri.parse(webViewMessage.getWebViewUrl());
                    Uri uri = Uri.parse(url);
                    if (inAppMessageUri.getScheme().equals(uri.getScheme())
                            && inAppMessageUri.getPort() == uri.getPort()
                            && inAppMessageUri.getHost().equals(uri.getHost())) {
                        webView.addJavascriptInterface(bridge, JAVASCRIPT_INTERFACE_NAME);
                    } else {
                        webView.removeJavascriptInterface(JAVASCRIPT_INTERFACE_NAME);
                    }
                } else {
                    webView.removeJavascriptInterface(JAVASCRIPT_INTERFACE_NAME);
                }
            }

            if (!url.startsWith("http:") && !url.startsWith("https:")) {
                openDeepLink(url);
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    private class ModernClient extends BaseClient {

        ModernClient(Runnable onSuccess, Consumer<Throwable> onError, Resources resources) {
            super(onSuccess, onError, resources);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request != null && request.getUrl() != null && request.getUrl().toString().startsWith(HTML_INAPP_SDK_URL)) {
                return new WebResourceResponse("text/javascript", "utf-8", getJavascriptSDKInputStream());
            }
            return super.shouldInterceptRequest(view, request);
        }

        final long webViewVisualStateCallbackId = new Random().nextLong();

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onPageStarted(final WebView webView, String url, Bitmap favicon) {
            super.onPageStarted(webView, url, favicon);
            webView.postVisualStateCallback(webViewVisualStateCallbackId, new WebView.VisualStateCallback() {
                @Override
                public void onComplete(long requestId) {
                    if (requestId == webViewVisualStateCallbackId) {
                        initialLoadSuccess();
                    }
                }
            });
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            try {
                super.onReceivedHttpError(view, request, errorResponse);
                // Only fail for the main document
                if (request.isForMainFrame()) {
                    initialLoadError(new Exception("HTTP error loading webView with status " + errorResponse.getStatusCode() + " for url: " + request.getUrl().toString()));
                }
            } catch (Exception exception) {
                Logging.loge(exception.getLocalizedMessage());
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            try {
                super.onReceivedError(view, request, error);
                // Only fail for the main document
                if (request.isForMainFrame()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        initialLoadError(new Exception(String.format(Locale.ENGLISH, "Error loading webView with code: %d description: %s", error.getErrorCode(), error.getDescription())));
                    } else {
                        initialLoadError(new Exception("Error loading webView (code and description unavailable)"));
                    }
                }
            } catch (Exception e) {
                Logging.loge("Unexpected webView error", e);
            }
        }

    }

    private class LegacyClient extends BaseClient {

        LegacyClient(Runnable onSuccess, Consumer<Throwable> onError, Resources resources) {
            super(onSuccess, onError, resources);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url != null && url.startsWith(HTML_INAPP_SDK_URL)) {
                return new WebResourceResponse("text/javascript", "utf-8", getJavascriptSDKInputStream());
            }
            return super.shouldInterceptRequest(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            try {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Note: we don't have the luxury of testing request.isForMainFrame() here,
                // and we can't compare failingUrl with webViewUrl because they may be different as we follow redirects
                initialLoadError(new Exception("Error loading webView " + (description != null ? description : "(no description)") + " failing URL: " + (failingUrl != null ? failingUrl : "(no url)")));
            } catch (Exception e) {
                Logging.loge("Unexpected webView error", e);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            try {
                super.onPageFinished(view, url);
                initialLoadSuccess();
            } catch (Exception exception) {
                Logging.loge(exception.getLocalizedMessage());
            }
        }

    }

}
