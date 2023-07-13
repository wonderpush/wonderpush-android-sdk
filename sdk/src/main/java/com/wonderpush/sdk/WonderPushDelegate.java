package com.wonderpush.sdk;

/**
 * The interface of the WonderPush SDK delegate you can implement for a tighter integration.
 *
 * @see WonderPush#setDelegate(WonderPushDelegate)
 * @see WonderPushAbstractDelegate
 */
public interface WonderPushDelegate {

    /**
     * Hook called when the SDK is trying to open a deep-link with the appropriate activity or service.
     *
     * <p>Note: This does not fire for silent data notifications.</p>
     *
     * <p>
     *   Return the URL the SDK should proceed with, or {@code null} if you handled the deep-link yourself.
     *   Simply return {@code event.getUrl()} to continue with the default behavior.
     * </p>
     *
     * @param event The deep-link event information
     * @return The URL the SDK should open, or {@code null} if it should stop normal processing.
     */
    String urlForDeepLink(DeepLinkEvent event);

    void onNotificationOpened(NotificationModel notif, int buttonIndex);
    void onNotificationReceived(NotificationModel notif);
}
