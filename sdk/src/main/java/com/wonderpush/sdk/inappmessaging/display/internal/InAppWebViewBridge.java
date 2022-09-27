package com.wonderpush.sdk.inappmessaging.display.internal;

import android.graphics.Rect;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import androidx.annotation.Nullable;

import com.wonderpush.sdk.WonderPush;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.*;

public class InAppWebViewBridge {

    public interface Controller {
        void sendError(String msg);
        void openExternalUrl(String url);
        void openDeepLink(String url);
        void triggerLocationPrompt() throws Exception;
        void dismiss();
        void trackClick(String buttonLabel);
        void openAppRating();
        void trackEvent(String type);
        void trackEvent(String type, JSONObject attributes);
        void callMethod(String methodName, String methodArg);
        void setClipPath(Rect clipPath);
        JSONObject getPayload();
    }

    private final WeakReference<Controller> controllerRef;

    public InAppWebViewBridge(Controller controller) {
        this.controllerRef = new WeakReference<>(controller);
    }

    private @Nullable String toJavascriptResult(Object o) {
        try {
            JSONObject out = new JSONObject();
            out.put("result", o == null ? JSONObject.NULL : o);
            String result = out.toString();
            return result;
        } catch (JSONException e) {
            Logging.loge("Could not encode result", e);
            return null;
        }
    }

    private @Nullable String toJavascriptError(String msg) {
        try {
            JSONObject out = new JSONObject();
            out.put("error", msg == null ? "Unknown error" : msg);
            return out.toString();
        } catch (JSONException e) {
            Logging.loge("Could not encode error", e);
            return null;
        }
    }

    private @Nullable Object argToObject(String arg) {
        return argToObject(arg, null);
    }

    private @Nullable Object argToObject(String arg, Object defaultValue) {
        try {
            JSONObject result = new JSONObject(arg);
            Object value = result.opt("value");
            return value == null ? defaultValue : value;
        } catch (JSONException e) {
            Logging.loge("Could not decode arg", e);
            return defaultValue;
        }
    }

    private @Nullable String argToString(String arg) {
        return argToString(arg, null);
    }

    private @Nullable String argToString(String arg, String defaultValue) {
        Object result = argToObject(arg);
        return result instanceof String ? (String)result : defaultValue;
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

    @JavascriptInterface
    public void dismiss() {
        Controller controller = controllerRef.get();
        if (controller != null) controller.dismiss();
    }

    @JavascriptInterface
    public String trackClick(String buttonLabelArg) {
        String buttonLabel = argToString(buttonLabelArg);
        if (buttonLabel == null) return toJavascriptError("buttonLabel cannot be null");
        Controller controller = controllerRef.get();
        if (controller != null) controller.trackClick(buttonLabel);
        return null;
    }

    @JavascriptInterface
    public String getPayload() {
        Controller controller = controllerRef.get();
        JSONObject payload = controller != null ? controller.getPayload() : null;
        if (payload != null) {
            return toJavascriptResult(payload);
        }
        return null;
    }

    @JavascriptInterface
    public @Nullable String openExternalUrl(String urlStringArg) {
        String urlString = argToString(urlStringArg);
        if (urlString == null) return toJavascriptError("Url cannot be null");
        if (Uri.parse(urlString) == null) return toJavascriptError("Invalid url");
        Controller controller = controllerRef.get();
        if (controller != null) controller.openExternalUrl(urlString);
        return null;
    }

    @JavascriptInterface
    public @Nullable String openDeepLink(String urlStringArg) {
        String urlString = argToString(urlStringArg);
        if (urlString == null) return toJavascriptError("Url cannot be null");
        if (Uri.parse(urlString) == null) return toJavascriptError("Invalid url");
        Controller controller = controllerRef.get();
        if (controller != null) controller.openDeepLink(urlString);
        return null;
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
        return toJavascriptResult(WonderPush.getUserId());
    }

    @JavascriptInterface
    public String getInstallationId() {
        return toJavascriptResult(WonderPush.getInstallationId());
    }

    @JavascriptInterface
    public String getCountry() {
        return toJavascriptResult(WonderPush.getCountry());
    }

    @JavascriptInterface
    public String getCurrency() {
        return toJavascriptResult(WonderPush.getCurrency());
    }

    @JavascriptInterface
    public String getLocale() {
        return toJavascriptResult(WonderPush.getLocale());
    }

    @JavascriptInterface
    public String getTimeZone() {
        return toJavascriptResult(WonderPush.getTimeZone());
    }

    @JavascriptInterface
    public @Nullable String trackEvent(String typeArg) {
        String type = argToString(typeArg);
        if (type == null) return toJavascriptError("Type cannot be null");
        Controller controller = controllerRef.get();
        if (controller == null) return toJavascriptError("Null controller");
        controller.trackEvent(type);
        return null;
    }

    @JavascriptInterface
    public String getDevicePlatform() {
        return toJavascriptResult("Android");
    }

    @JavascriptInterface
    public @Nullable String trackEvent(String typeArg, String attributeString) {
        String type = argToString(typeArg);
        if (type == null) return toJavascriptError("Type cannot be null");
        JSONObject attributes = argToJSONObject(attributeString, new JSONObject());
        Controller controller = controllerRef.get();
        if (controller == null) return toJavascriptError("Null controller");
        controller.trackEvent(type, attributes);
        return null;
    }

    @JavascriptInterface
    public void addTag(String t1) {
        JSONArray tags = argToJSONArray(t1);
        String[] tagArray = new String[tags.length()];
        for (int i = 0; i < tags.length(); i++) tagArray[i] = tags.optString(i);
        WonderPush.addTag(tagArray);
    }

    @JavascriptInterface
    public void removeTag(String t1) {
        JSONArray tags = argToJSONArray(t1);
        String[] tagArray = new String[tags.length()];
        for (int i = 0; i < tags.length(); i++) tagArray[i] = tags.optString(i);
        WonderPush.removeTag(tagArray);
    }

    @JavascriptInterface
    public void removeAllTags() {
        WonderPush.removeAllTags();
    }

    @JavascriptInterface
    public @Nullable String hasTag(String tagArg) {
        String tag = argToString(tagArg);
        if (tag == null) return toJavascriptError("Tag cannot be null");
        return toJavascriptResult(WonderPush.hasTag(tag));
    }

    @JavascriptInterface
    public String getTags() {
        Set<String> tags = WonderPush.getTags();
        return toJavascriptResult(new JSONArray(tags));
    }

    @JavascriptInterface
    public String getPropertyValue(String fieldArg) {
        String field = argToString(fieldArg);
        if (field == null) return toJavascriptError("Field cannot be null");
        return toJavascriptResult(WonderPush.getPropertyValue(field));
    }

    @JavascriptInterface
    public String getPropertyValues(String fieldArg) {
        String field = argToString(fieldArg);
        if (field == null) return toJavascriptError("Field cannot be null");
        return toJavascriptResult(WonderPush.getPropertyValues(field));
    }

    @JavascriptInterface
    public @Nullable String addProperty(String fieldArg, String valString) {
        String field = argToString(fieldArg);
        if (field == null) return toJavascriptError("Field cannot be null");
        Object val = argToObject(valString, null);
        WonderPush.addProperty(field, val);
        return null;
    }

    @JavascriptInterface
    public @Nullable String removeProperty(String fieldArg, String encodedValue) {
        String field = argToString(fieldArg);
        if (field == null) return toJavascriptError("Field cannot be null");
        Object val = argToObject(encodedValue, null);
        WonderPush.removeProperty(field, val);
        return null;
    }

    @JavascriptInterface
    public @Nullable String setProperty(String fieldArg, String encodedValue) {
        String field = argToString(fieldArg);
        if (field == null) return toJavascriptError("Field cannot be null");
        Object val = argToObject(encodedValue, null);
        WonderPush.setProperty(field, val);
        return null;
    }

    @JavascriptInterface
    public @Nullable String unsetProperty(String fieldArg) {
        String field = argToString(fieldArg);
        if (field == null) return toJavascriptError("Field cannot be null");
        WonderPush.unsetProperty(field);
        return null;
    }

    @JavascriptInterface
    public @Nullable String putProperties(String encodedProperties) {
        JSONObject properties = argToJSONObject(encodedProperties);
        if (properties == null) return toJavascriptError("Properties cannot be null");
        WonderPush.putProperties(properties);
        return null;
    }

    @JavascriptInterface
    public String getProperties() {
        return toJavascriptResult(WonderPush.getProperties());
    }

    @JavascriptInterface
    public @Nullable String triggerLocationPrompt() {
        Controller controller = controllerRef.get();
        if (controller != null) {
            try {
                controller.triggerLocationPrompt();
            } catch (Exception e) {
                return toJavascriptError(e.getMessage());
            }
        }
        return null;
    }

    @JavascriptInterface
    public @Nullable String openAppRating() {
        Controller controller = controllerRef.get();
        if (controller == null) {
            return toJavascriptError("Null controller");
        }
        controller.openAppRating();
        return null;
    }

    @JavascriptInterface
    public @Nullable String callMethod(String methodNameArg) {
        String methodName = argToString(methodNameArg);
        if (methodName == null) return toJavascriptError("Method name cannot be null");
        Controller controller = controllerRef.get();
        if (controller == null) return toJavascriptError("Null controller");
        controller.callMethod(methodName, null);
        return null;
    }

    @JavascriptInterface
    public @Nullable String callMethod(String methodNameArg, String methodArgArg) {
        String methodName = argToString(methodNameArg);
        if (methodName == null) return toJavascriptError("Method name cannot be null");
        String methodArg = argToString(methodArgArg);
        Controller controller = controllerRef.get();
        if (controller == null) return toJavascriptError("Null controller");
        controller.callMethod(methodName, methodArg);
        return null;
    }

    @JavascriptInterface
    public void setClipPath(String encodedClipPath) {
        JSONObject clipPathJson = argToJSONObject(encodedClipPath);
        if (clipPathJson == null) return;
        Controller controller = controllerRef.get();
        if (controller == null) return;
        if (clipPathJson.isNull("rect")) {
            controller.setClipPath(null);
            return;
        }
        JSONObject rectJson = clipPathJson.optJSONObject("rect");
        if (rectJson == null) return;

        int left=0, top=0, right=0, bottom=0;
        left = rectJson.optInt("left", left);
        top = rectJson.optInt("top", top);
        right = rectJson.optInt("right", right);
        bottom = rectJson.optInt("bottom", bottom);
        if (!(left == 0 && right == 0 && top == 0 && bottom == 0)) {
            Rect clipPath = new Rect(left, top, right, bottom);
            controller.setClipPath(clipPath);
        }
    }
}
