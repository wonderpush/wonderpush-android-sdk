package com.wonderpush.sdk.inappmessaging.model;

import android.text.TextUtils;

import android.util.Patterns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.JSONUtil;
import com.wonderpush.sdk.NotificationMetadata;
import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;

import org.json.JSONObject;

import java.util.List;

/** Encapsulates an In App WebView Message. */
public class WebViewMessage extends InAppMessage implements InAppMessage.InAppMessageWithWebView
{
    /*
     * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
     * new private class members.
     */
    @NonNull
    private String webViewUrl;

    @NonNull private List<ActionModel> actions;

    @NonNull private CloseButtonPosition closeButtonPosition;

    public static WebViewMessage create(NotificationMetadata notificationMetadata, JSONObject payloadJson, JSONObject webViewJson) throws Campaign.InvalidJsonException {
        // WebView
        String webViewUrlString = JSONUtil.optString(webViewJson, "url");

        if (TextUtils.isEmpty(webViewUrlString)) {
            throw new Campaign.InvalidJsonException("Missing url in webViewJson payload: " + webViewJson.toString());
        } else if (!Patterns.WEB_URL.matcher(webViewUrlString).matches()) { // Validate url
            throw new Campaign.InvalidJsonException("Invalid url in webViewJson payload: " + webViewUrlString);
        }

        // Actions
        List <ActionModel> actions = ActionModel.from(webViewJson.optJSONArray("actions"));

        String closeButtonPositionString = webViewJson.optString("closeButtonPosition", "outside");
        InAppMessage.CloseButtonPosition closeButtonPosition = InAppMessage.CloseButtonPosition.OUTSIDE;
        if ("inside".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.INSIDE;
        if ("none".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.NONE;

        // Animations
        IamAnimator.EntryAnimation entryAnimation = IamAnimator.EntryAnimation.fromSlug(webViewJson.optString("entryAnimation", "fadeIn"));
        IamAnimator.ExitAnimation exitAnimation = IamAnimator.ExitAnimation.fromSlug(webViewJson.optString("exitAnimation", "fadeOut"));
        return new WebViewMessage(notificationMetadata, webViewUrlString, actions, closeButtonPosition, entryAnimation, exitAnimation, payloadJson);
    }

    /** @hide */
    @Override
    public int hashCode() {
        int actionHash = actions != null ? actions.hashCode() : 0;
        return webViewUrl.hashCode() + actionHash + closeButtonPosition.hashCode() + entryAnimation.hashCode() + exitAnimation.hashCode();
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true; // same instance
        }
        if (!(o instanceof WebViewMessage)) {
            return false; // not the correct instance type
        }
        WebViewMessage i = (WebViewMessage) o;
        if (entryAnimation != i.entryAnimation) return false;
        if (exitAnimation != i.exitAnimation) return false;
        if (hashCode() != i.hashCode()) {
            return false; // the hashcodes don't match
        }
        if ((actions == null && i.actions != null) || (actions != null && !actions.equals(i.actions))) {
            return false; // the actions don't match
        }

        if (closeButtonPosition != i.closeButtonPosition) return false;

        if (webViewUrl.equals(i.webViewUrl)) {
            return true; // everything matches
        }
        return false;
    }

    /*
     * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
     * new private class members.
     */
    private WebViewMessage(
            @NonNull NotificationMetadata notificationMetadata,
            @NonNull String webViewUrl,
            @NonNull List<ActionModel> actions,
            @NonNull CloseButtonPosition closeButtonPosition,
            @NonNull IamAnimator.EntryAnimation entryAnimation,
            @NonNull IamAnimator.ExitAnimation exitAnimation,
            @NonNull JSONObject data) {
        super(notificationMetadata, MessageType.WEBVIEW, data, entryAnimation, exitAnimation);
        this.webViewUrl = webViewUrl;
        this.actions = actions;
        this.closeButtonPosition = closeButtonPosition;
    }

    /** Gets the url associated with this message */
    @Nullable
    @Override
    public String getWebViewUrl() {
        return webViewUrl;
    }

    /** Gets the {@link ActionModel}s associated with this message */
    @NonNull
    public List<ActionModel> getActions() {
        return actions;
    }

    @NonNull
    public CloseButtonPosition getCloseButtonPosition() {
        return closeButtonPosition;
    }

    @Override
    public ButtonType getButtonType(List<ActionModel> actions) {
        return actionsEqual(actions, this.actions) ? ButtonType.PRIMARY : ButtonType.UNDEFINED;
    }
}
