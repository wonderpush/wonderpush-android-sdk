package com.wonderpush.sdk.inappmessaging.display.internal.web;

public enum InAppWebViewBridgeOpenTargetUrlMode
{
    CURRENT("current"),EXTERNAL("external"), PARENT("parent");

    public String webViewModeName;

    private InAppWebViewBridgeOpenTargetUrlMode(String webViewModeName){
        this.webViewModeName = webViewModeName;
    }

    public static InAppWebViewBridgeOpenTargetUrlMode getInAppWebViewBridgeOpenTargetUrlModeFor(String webViewModeName){
        switch(webViewModeName){
            case "external":
                return EXTERNAL;
            case "parent":
                return PARENT;
            default:
                return CURRENT;
        }
    }
}
