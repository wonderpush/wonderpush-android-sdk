package com.wonderpush.sdk;

/**
 * Default no-op implementation of the {@link WonderPushDelegate} for easy implementation.
 *
 * Subclassing this will ensure your code won't break if new methods gets added to the underlying interface.

 * @see WonderPush#setDelegate(WonderPushDelegate)
 * @see WonderPushDelegate
 */
public abstract class WonderPushAbstractDelegate implements WonderPushDelegate {

    @Override
    public String urlForDeepLink(DeepLinkEvent event) {
        return event.getUrl();
    }

}
