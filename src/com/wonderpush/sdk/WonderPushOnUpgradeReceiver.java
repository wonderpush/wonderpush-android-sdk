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
        if (!WonderPushConfiguration.isInitialized()) {
            WonderPushConfiguration.initialize(context);
        }

        WonderPush.ensureInitialized(context);

        // Detect same-version-code updates, which can invalidate the push token in a few weeks.
        // This scenario typically only happens during development.
        int versionCode = WonderPush.getApplicationVersionCode();
        if (WonderPushConfiguration.getGCMRegistrationAppVersionForUpdateReceiver() == versionCode) {
            // Forget the old registration id, it will expire soon
            WonderPushConfiguration.setGCMRegistrationId(null);
            // Force re-registration
            // Even if this call is concurrent with another update of push token (with an old value),
            // as this scenario is likely to reproduce soon in a classic development process,
            // and as Google registration ids are still valid for a few weeks, there is no service disruption.
            WonderPush.registerForPushNotification(context);
        } else {
            WonderPushConfiguration.setGCMRegistrationAppVersionForUpdateReceiver(versionCode);
        }
    }

}
