package com.wonderpush.sdk.inappmessaging.display.internal.bindingwrappers;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.wonderpush.sdk.R;
import com.wonderpush.sdk.inappmessaging.display.internal.InAppMessageLayoutConfig;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessageScope;
import com.wonderpush.sdk.inappmessaging.display.internal.layout.IamRelativeLayout;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.inappmessaging.model.MessageType;
import com.wonderpush.sdk.inappmessaging.model.WebViewMessage;

import java.util.List;

import javax.inject.Inject;

/**
 * Wrapper for bindings for WebView only modal. This class currently is not unit tested since it is
 * purely declarative.
 *
 * @hide
 */
@InAppMessageScope
public class WebViewBindingWrapper extends BindingWrapper {

    private IamRelativeLayout webViewRoot;

    private WebView webView;
    private Button collapseButton;

    @Inject
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WebViewBindingWrapper(Rect displayBounds, InAppMessageLayoutConfig config, LayoutInflater inflater, InAppMessage message) {
        super(displayBounds, config, inflater, message);
    }

    @Nullable
    @Override
    public View getDismissView() {
        return webViewRoot;
    }

    @NonNull
    @Override
    public ImageView getImageView() {
        return new ImageView(webViewRoot.getContext());
    }

    @NonNull
    @Override
    public ViewGroup getRootView() {
        return webViewRoot;
    }

    @NonNull
    @Override
    public View getDialogView() {
        return webViewRoot;
    }

    @NonNull
    public View getCollapseButton() {
        return collapseButton;
    }

    @NonNull
    public WebView getWebView() {
        return webView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public ViewTreeObserver.OnGlobalLayoutListener inflate(
            List<View.OnClickListener> actionListeners,
            View.OnClickListener dismissOnClickListener) {
        try {
            View v = inflater.inflate(R.layout.wonderpush_android_sdk_webview, null);
            webViewRoot = v.findViewById(R.id.webview_root);
            webView = (WebView) v.findViewById(R.id.webview);
            collapseButton = v.findViewById(R.id.collapse_button);

            // Setup WebView.
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);

            if (message.getMessageType().equals(MessageType.WEBVIEW)) {
                WebViewMessage msg = (WebViewMessage) message;
                webView.setVisibility(
                        (msg.getWebViewUrl() == null || TextUtils.isEmpty(msg.getWebViewUrl()))
                                ? View.GONE
                                : View.VISIBLE);
                if (actionListeners.size() > 0)
                    webView.setOnClickListener(actionListeners.get(0));
            }

            // Setup dismiss button.
            webViewRoot.setDismissListener(dismissOnClickListener);
            collapseButton.setOnClickListener(dismissOnClickListener);

            if (message instanceof WebViewMessage){
                webView.loadUrl(((WebViewMessage) message).getWebViewUrl());
            }

            if (message instanceof WebViewMessage
                    && collapseButton.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) collapseButton.getLayoutParams();
                float density = inflater.getContext().getResources().getDisplayMetrics().density;
                switch (((WebViewMessage) message).getCloseButtonPosition()) {
                    case NONE:
                        collapseButton.setVisibility(View.GONE);
                        break;
                    case INSIDE:
                        collapseButton.setVisibility(View.VISIBLE);
                        layoutParams.topMargin = (int) (density * 5);
                        layoutParams.rightMargin = (int) (density * 5);
                        break;
                    case OUTSIDE:
                        collapseButton.setVisibility(View.VISIBLE);
                        layoutParams.topMargin = (int) (density * -12);
                        layoutParams.rightMargin = (int) (density * -12);
                        break;
                }
                collapseButton.setLayoutParams(layoutParams);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return null;
    }
}
