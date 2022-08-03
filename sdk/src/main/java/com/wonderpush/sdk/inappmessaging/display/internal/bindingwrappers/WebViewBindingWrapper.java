package com.wonderpush.sdk.inappmessaging.display.internal.bindingwrappers;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
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
import com.wonderpush.sdk.inappmessaging.display.internal.Logging;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessageScope;
import com.wonderpush.sdk.inappmessaging.display.internal.layout.IamRelativeLayout;
import com.wonderpush.sdk.inappmessaging.display.internal.layout.util.MeasureUtils;
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

    @Nullable
    @Override
    public ImageView getImageView() {
        return null;
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

    @Nullable
    @Override
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
            webView.setBackgroundColor(Color.TRANSPARENT);
            collapseButton = v.findViewById(R.id.collapse_button);

            //Set status bar padding
            v.setPadding(
                    v.getPaddingLeft(),
                    MeasureUtils.getStatusBarHeightFor(v.getContext()),
                    v.getPaddingRight(),
                    v.getPaddingBottom());

            // Setup WebView.
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

            //following settings require api 23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                webView.getSettings().setOffscreenPreRaster(true);
            }

            if (message.getMessageType().equals(MessageType.WEBVIEW)) {
                WebViewMessage msg = (WebViewMessage) message;
                webView.setVisibility(
                       TextUtils.isEmpty(msg.getWebViewUrl())
                                ? View.GONE
                                : View.VISIBLE);
                if (actionListeners.size() > 0)
                    webView.setOnClickListener(actionListeners.get(0));
            }

            // Setup dismiss button.
            webViewRoot.setDismissListener(dismissOnClickListener);
            collapseButton.setOnClickListener(dismissOnClickListener);

            if (message instanceof WebViewMessage
                    && collapseButton.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) collapseButton.getLayoutParams();
                float density = inflater.getContext().getResources().getDisplayMetrics().density;

                if (((WebViewMessage) message).getCloseButtonPosition() == InAppMessage.CloseButtonPosition.NONE)
                {
                    collapseButton.setVisibility(View.GONE);
                }
                else {
                    //inside and outside are the same cause of fullscreen
                    collapseButton.setVisibility(View.VISIBLE);
                    layoutParams.topMargin = (int) (density * 5);
                    layoutParams.rightMargin = (int) (density * 5);
                }

                collapseButton.setLayoutParams(layoutParams);
            }
        }
        catch(Exception exception){
            Logging.loge(exception.getLocalizedMessage());
        }

        return null;
    }
}
