package com.wonderpush.sdk;

import android.content.Context;

/**
 * Information-storing class for the hook called when the SDK is trying to open the appropriate deep-link.
 *
 * @see WonderPushDelegate#urlForDeepLink(DeepLinkEvent)
 */
public class DeepLinkEvent {

    private Context context;
    private String url;

    DeepLinkEvent(Context context, String url) {
        this.context = context;
        this.url = url;
    }

    /**
     * The context we are running in.
     */
    public Context getContext() {
        return context;
    }

    /**
     * The deep-link we are to open.
     */
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "DeepLinkEvent{" +
                "url='" + url + '\'' +
                '}';
    }

}
