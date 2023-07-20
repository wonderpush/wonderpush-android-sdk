package com.wonderpush.sdk;

import org.json.JSONObject;

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

    /**
     * Hook called when a notification is clicked.
     * @param notif The clicked notification
     * @param buttonIndex The index of the button clicked in the notification, or -1 when the notification itself was clicked
     */
    default void onNotificationOpened(JSONObject notif, int buttonIndex) {};

    /**
     * Hook called when a notification is received
     * @param notif The received notification
     */
    default void onNotificationReceived(JSONObject notif) {};

}
