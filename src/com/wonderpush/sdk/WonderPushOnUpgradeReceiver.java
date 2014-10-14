package com.wonderpush.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver that refreshes the push token after each upgrade.
 *
 * <pre><code>&lt;application&gt;
 *    &lt;receiver android:name="com.wonderpush.sdk.WonderPushOnUpgradeReceiver"&gt;
 *        &lt;intent-filter&gt;
 *            &lt;action android:name="android.intent.action.PACKAGE_REPLACED" /&gt;
 *            &lt;data android:scheme="package" android:path="com.package" /&gt;
 *        &lt;/intent-filter&gt;
 *    &lt;/receiver&gt;
 *&lt;/application&gt;</code></pre>
 */
public class WonderPushOnUpgradeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Forget the old registration id, it will expire soon
        if (!WonderPushConfiguration.isInitialized()) {
            WonderPushConfiguration.initialize(context);
            WonderPushConfiguration.setGCMRegistrationId(null);
        }

        // Will register for push notifications itself
        WonderPush.ensureInitialized(context);
    }

}
