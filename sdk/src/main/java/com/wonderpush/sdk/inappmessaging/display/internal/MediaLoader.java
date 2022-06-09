package com.wonderpush.sdk.inappmessaging.display.internal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.*;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewFeature;
import com.squareup.picasso.Callback;
import com.wonderpush.sdk.LogErrorProvider;
import com.wonderpush.sdk.SafeDeferProvider;
import com.wonderpush.sdk.inappmessaging.display.internal.bindingwrappers.BindingWrapper;
import com.wonderpush.sdk.inappmessaging.display.internal.web.InAppWebViewBridge;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Random;

public class MediaLoader {

    static final String HTML_INAPP_SDK_URL = "https://cdn.by.wonderpush.com/inapp-sdk/1.0/wonderpush-loader.min.js";

    static final long WEB_VIEW_LOAD_TIMEOUT_MILLIS = 10 * 1000; // timeout error after 10 seconds

    public static class WebViewLoadingException extends Exception {
        public WebViewLoadingException(Exception wrapped) {
            super(wrapped != null ? wrapped.getMessage() : null);
        }
    }

    public interface DismissProvider {
        void dismissIam(Activity activity);
    }

    final private IamImageLoader imageLoader;
    final private SafeDeferProvider safeDeferProvider;
    final private LogErrorProvider logErrorProvider;
    final private DismissProvider dismissProvider;

    public MediaLoader(
            IamImageLoader imageLoader,
            SafeDeferProvider safeDeferProvider,
            LogErrorProvider logErrorProvider,
            DismissProvider dismissProvider
            ) {
        this.imageLoader = imageLoader;
        this.safeDeferProvider = safeDeferProvider;
        this.logErrorProvider = logErrorProvider;
        this.dismissProvider = dismissProvider;
    }

    private class BaseClient extends WebViewClient {

        private Callback callback;

        BaseClient(Callback callback) {
            this.callback = callback;
        }

        private boolean callbackDone = false;

        protected void callOnSuccess(WebView webView) {
            if (callbackDone) {
                return;
            }
            callbackDone = true;
            if (webView != null) webView.setWebViewClient(null);
            callback.onSuccess();
        }

        protected void callOnError(WebView webView, Exception err) {
            if (callbackDone) {
                return;
            }
            callbackDone = true;
            if (webView != null) webView.setWebViewClient(null);
            callback.onError(new WebViewLoadingException(err));
        }

        @SuppressLint({"RequiresFeature", "NewApi"})
        @Override
        public void onPageStarted(final WebView webView, String url, Bitmap favicon) {
            try {
                super.onPageStarted(webView, url, favicon);

                safeDeferProvider.safeDefer(() -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            callOnError(webView, new Exception("WebView timeout reached"));
                        } catch (Exception e) {
                            logErrorProvider.logError("Unexpected error", e);
                        }
                    });
                }, WEB_VIEW_LOAD_TIMEOUT_MILLIS);
            } catch (Exception exception) {
                logErrorProvider.logError(exception.getLocalizedMessage());
            }
        }

    }

    private class ModernClient extends BaseClient {

        ModernClient(Callback callback) {
            super(callback);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request != null && request.getUrl() != null && request.getUrl().toString().startsWith(HTML_INAPP_SDK_URL)) {
                return new WebResourceResponse("text/javascript", "utf-8", new ByteArrayInputStream(new byte[0]));
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
                        callOnSuccess(webView);
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
                    callOnError(view, new Exception("HTTP error loading webView with status " + errorResponse.getStatusCode() + " for url: " + request.getUrl().toString()));
                }
            } catch (Exception exception) {
                logErrorProvider.logError(exception.getLocalizedMessage());
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            try {
                super.onReceivedError(view, request, error);
                // Only fail for the main document
                if (request.isForMainFrame()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        callOnError(view, new Exception(String.format(Locale.ENGLISH, "Error loading webView with code: %d description: %s", error.getErrorCode(), error.getDescription())));
                    } else {
                        callOnError(view, new Exception("Error loading webView (code and description unavailable)"));
                    }
                }
            } catch (Exception e) {
                logErrorProvider.logError("Unexpected webView error", e);
            }
        }

    }

    private class LegacyClient extends BaseClient {

        LegacyClient(Callback callback) {
            super(callback);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url != null && url.startsWith(HTML_INAPP_SDK_URL)) {
                return new WebResourceResponse("text/javascript", "utf-8", new ByteArrayInputStream(new byte[0]));
            }
            return super.shouldInterceptRequest(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            try {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Note: we don't have the luxury of testing request.isForMainFrame() here,
                // and we can't compare failingUrl with webViewUrl because they may be different as we follow redirects
                callOnError(view, new Exception("Error loading webView " + (description != null ? description : "(no description)") + " failing URL: " + (failingUrl != null ? failingUrl : "(no url)")));
            } catch (Exception e) {
                logErrorProvider.logError("Unexpected webView error", e);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            try {
                super.onPageFinished(view, url);
                callOnSuccess(view);
            } catch (Exception exception) {
                logErrorProvider.logError(exception.getLocalizedMessage());
            }
        }

    }

    public void loadWebView(
            Activity activity,
            BindingWrapper iam,
            String webViewUrl,
            final Callback callback) {
        final WebView webView = iam.getWebView();
        if (webViewUrl != null && webView != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try
                {

                    WebViewClient client = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? new ModernClient(callback) : new LegacyClient(callback);
                    webView.setWebViewClient(client);
                    webView.addJavascriptInterface(new InAppWebViewBridge(webView, () -> {
                        try {
                            this.dismissProvider.dismissIam(activity);
                        } catch (Exception exception) {
                            logErrorProvider.logError(exception.getLocalizedMessage());
                        }
                    }, this.logErrorProvider), "WonderPushInAppSDK");
                    webView.loadUrl(webViewUrl);
                }
                catch (Exception exception) {
                    logErrorProvider.logError("Unexpected error loading webView", exception);
                    if (callback != null) callback.onError(exception);
                }
            });
        } else if (callback != null) {
            callback.onSuccess();
        }

    }

    public void loadImage(
            Activity activity,
            BindingWrapper iam,
            String imageUrl,
            Callback callback) {
        if (imageUrl != null)  {
            imageLoader
                    .load(imageUrl)
                    .tag(activity.getClass())
                    .into(iam.getImageView(), callback);
        } else if (callback != null) {
            callback.onSuccess();
        }
    }

    public void loadNullableMedia(
            Activity activity,
            BindingWrapper iam,
            String imageUrl,
            String webViewUrl,
            Callback callback) {
        loadImage(activity, iam, imageUrl, new Callback() {
            @Override
            public void onSuccess() {
                loadWebView(activity, iam, webViewUrl, callback);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }


}
