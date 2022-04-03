package com.wonderpush.sdk.inappmessaging.display.internal.web;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.wonderpush.sdk.JSONUtil;
import com.wonderpush.sdk.WonderPush;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class InAppWebViewBridge
{
    private final WeakReference<WebView> concernedWebViewReference;
    private final InAppWebViewBridgeInterface inAppWebViewBridgeInterface;
    private final Gson gsonInstance;

    public InAppWebViewBridge(WebView concernedWebViewExternalInstance, InAppWebViewBridgeInterface externalInAppWebViewBridgeInterface) {
        concernedWebViewReference = new WeakReference(concernedWebViewExternalInstance);
        inAppWebViewBridgeInterface = externalInAppWebViewBridgeInterface;
        gsonInstance = new Gson();
    }


    /********************************************************************/
    /*                       DISMISS                                    */
    @JavascriptInterface
    public void dismiss() {
        onUserCallDismissMethod();
    }

    @JavascriptInterface
    public void dismiss(String buttonId) {
        onUserCallDismissMethod();
    }

    private void onUserCallDismissMethod(){
        try{
            inAppWebViewBridgeInterface.onDimissCalled();
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                       OPEN TARGET URL                            */
    @JavascriptInterface
    public void openTargetUrl(String url) {
        onUserWantToOpenTargetUrl(url, InAppWebViewBridgeOpenTargetUrlMode.CURRENT);
    }

    @JavascriptInterface
    public void openTargetUrl(String url, String params) {
        try
        {
            if (JSONUtil.isJSONValid(params)) {
                JSONObject paramsDecoded = new JSONObject(params);

                if (paramsDecoded.has("mode")) {
                    onUserWantToOpenTargetUrl(url, InAppWebViewBridgeOpenTargetUrlMode.getInAppWebViewBridgeOpenTargetUrlModeFor(paramsDecoded.getString("mode")));
                }
                else
                {
                    onUserWantToOpenTargetUrl(url, InAppWebViewBridgeOpenTargetUrlMode.CURRENT);
                }
            }
            else {
                onUserWantToOpenTargetUrl(url, InAppWebViewBridgeOpenTargetUrlMode.CURRENT);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    private void onUserWantToOpenTargetUrl(String url, InAppWebViewBridgeOpenTargetUrlMode mode)
    {
        try
        {
            WebView functionLocalReferenceToWebview = concernedWebViewReference.get();

            if (null == functionLocalReferenceToWebview){
                return;
            }

            switch(mode){
                case EXTERNAL:
                    functionLocalReferenceToWebview.post(() -> {
                        try {
                            //require browser does not work
                            Intent externalWebViewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            functionLocalReferenceToWebview.getContext().startActivity(externalWebViewIntent);
                        }
                        catch(Exception exception){
                            //TODO : manage exception
                        }
                    });
                    break;
                case PARENT:
                    //works, you can test with "googlechrome://navigate?url=" + url
                    Intent externalWebViewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        externalWebViewIntent.setFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);
                    }
                    functionLocalReferenceToWebview.post(() -> {
                            try {
                                functionLocalReferenceToWebview.getContext().startActivity(externalWebViewIntent);
                            }
                            catch(Exception exception){
                                //TODO : manage exception
                            }
                    });
                    break;
                default:
                    functionLocalReferenceToWebview.post(() -> {
                        try {
                            functionLocalReferenceToWebview.loadUrl(url);
                        }
                        catch(Exception exception){
                            //TODO : manage exception
                        }
                    });
                    break;
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                       SUBSCRIBE TO NOTIFICATIONS                 */
    @JavascriptInterface
    public void subscribeToNotifications(){
        try{
            WonderPush.subscribeToNotifications();
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    @JavascriptInterface
    public void subscribeToNotifications(String encodedEvent){
        try{
            WonderPush.subscribeToNotifications();
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                       UNSUBSCRIBE TO NOTIFICATIONS               */
    @JavascriptInterface
    public void unsubscribeFromNotifications(){
        try{
            WonderPush.unsubscribeFromNotifications();
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                     IS SUBSCRIBED TO NOTIFICATIONS               */
    @JavascriptInterface
    public boolean isSubscribedToNotifications(){
        try{
            return WonderPush.isSubscribedToNotifications();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return false;
    }

    /********************************************************************/
    /*                          GET USER ID                             */
    @JavascriptInterface
    public String getUserId(){
        try{
            return WonderPush.getUserId();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                          GET INSTALLATION ID                     */
    @JavascriptInterface
    public String getInstallationId(){
        try{
            return WonderPush.getInstallationId();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                          GET COUNTRY                             */
    @JavascriptInterface
    public String getCountry(){
        try{
            return WonderPush.getCountry();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                          GET CURRENCY                             */
    @JavascriptInterface
    public String getCurrency(){
        try
        {
           return WonderPush.getCurrency();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                          GET LOCALE                              */
    @JavascriptInterface
    public String getLocale(){
        try{
            return WonderPush.getLocale();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                          GET TIMEZONE                            */
    @JavascriptInterface
    public String getTimeZone(){
        try{
            return WonderPush.getTimeZone();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                         TRACK EVENT                              */
    @JavascriptInterface
    public void trackEvent(String type){
        try{
            WonderPush.trackEvent(type);
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    @JavascriptInterface
    public void trackEvent(String type, String attributes){
        try{
            if (JSONUtil.isJSONValid(attributes)) {
                JSONObject attributesDecoded = new JSONObject(attributes);
                WonderPush.trackEvent(type, (JSONObject) attributesDecoded);
            }
            else {
                WonderPush.trackEvent(type);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                         ADD TAG                                  */
    @JavascriptInterface
    public void addTag(String encodedTagToAdd){
        try{
            if (JSONUtil.isJSONValid(encodedTagToAdd)) {
                String[] decodedTagToAdd = gsonInstance.fromJson(encodedTagToAdd, String[].class);
                WonderPush.addTag(decodedTagToAdd);
            }
            else {
                WonderPush.addTag(encodedTagToAdd);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                         REMOVE TAG                               */
    @JavascriptInterface
    public void removeTag(String encodedTagToRemove){
        try{
            if (JSONUtil.isJSONValid(encodedTagToRemove)) {
                String[] decodedTagToRemove = gsonInstance.fromJson(encodedTagToRemove, String[].class);
                WonderPush.removeTag(decodedTagToRemove);
            }
            else {
                WonderPush.removeTag(encodedTagToRemove);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                         REMOVE ALL TAGS                          */
    @JavascriptInterface
    public void removeAllTags(){
        try{
            WonderPush.removeAllTags();
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                           HAS TAG                                */
    @JavascriptInterface
    public boolean hasTag(String tag){
        try{
            return WonderPush.hasTag(tag);
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return false;
    }

    /********************************************************************/
    /*                           GET TAGS                               */
    @JavascriptInterface
    public String getTags(){
        try{
            return gsonInstance.toJson(WonderPush.getTags());
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                           GET PROPERTY VALUE                     */
    @JavascriptInterface
    public String getPropertyValue(String field){
        try{

            Object propertyValueResult = WonderPush.getPropertyValue(field);

            if (propertyValueResult instanceof String){
                //we have a string
                return (String) propertyValueResult;
            }
            else {
                return "";
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }

    /********************************************************************/
    /*                           GET PROPERTY VALUES                    */
    @JavascriptInterface
    public String getPropertyValues(String field){
        try{
            return gsonInstance.toJson(WonderPush.getPropertyValues(field));
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        String[] emptyArray = {};
        return gsonInstance.toJson(emptyArray);
    }

    /********************************************************************/
    /*                           ADD PROPERTY VALUE                     */
    @JavascriptInterface
    public void addProperty(String field, String encodedValue){
        try
        {
            if (JSONUtil.isJSONValid(encodedValue)) {
                String[] decodedPropertyToAdd = gsonInstance.fromJson(encodedValue, String[].class);
                WonderPush.addProperty(field, decodedPropertyToAdd);
            }
            else {
                WonderPush.addProperty(field, encodedValue);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                           REMOVE PROPERTY VALUE                  */
    @JavascriptInterface
    public void removeProperty(String field, String encodedValue){
        try{
            if (JSONUtil.isJSONValid(encodedValue)) {
                String[] decodedPropertyToRemove = gsonInstance.fromJson(encodedValue, String[].class);
                WonderPush.removeProperty(field, decodedPropertyToRemove);
            }
            else {
                WonderPush.removeProperty(field, encodedValue);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                           SET PROPERTY                           */
    @JavascriptInterface
    public void setProperty(String field, String value){
        try{
            if (value == null || value.equals("undefined")){
                WonderPush.unsetProperty(field);
            }
            else if (JSONUtil.isJSONValid(value)){
                Object[] decodedObjects = gsonInstance.fromJson(value, Object[].class);
                WonderPush.setProperty(field, decodedObjects);
            }
            else if ("true".equals(value) || "false".equals(value)){
                WonderPush.setProperty(field, "true".equals(value));
            }
            else {
                WonderPush.setProperty(field, value);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                           UNSET PROPERTY                         */
    @JavascriptInterface
    public void unsetProperty(String field){
        try{
            WonderPush.unsetProperty(field);
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                          PUT PROPERTIES                          */
    @JavascriptInterface
    public void putProperties(String properties){
        try
        {
            if (JSONUtil.isJSONValid(properties))
            {
                JSONObject decodedProperties = new JSONObject(properties);
                WonderPush.putProperties(decodedProperties);
            }
        }
        catch(Exception exception){
            //TODO : manage exception
        }
    }

    /********************************************************************/
    /*                          GET PROPERTIES                          */
    @JavascriptInterface
    public String getProperties(){
        try{
            return WonderPush.getProperties().toString();
        }
        catch(Exception exception){
            //TODO : manage exception
        }

        return "";
    }
}
