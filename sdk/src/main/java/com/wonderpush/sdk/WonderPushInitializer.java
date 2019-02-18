package com.wonderpush.sdk;

import android.content.Context;

/**
 * Interface for the self-initialization of the SDK, if automatic initialization is not used.
 *
 * <p>
 *     You can implement this interface to control the initialization of the SDK using code.
 *     This should hardly be necessary and using automatic initialization if easier and recommended.
 * </p>
 *
 * <p>
 *     Reference your implementation by adding the following in your {@code AndroidManifest.xml},
 *     inside your {@code <application>} tag with the actual class name:
 * </p>
 * <pre><code>&lt;meta-data
 *        android:name="wonderpushInitializerClass"
 *        android:value=".MyWonderPushInitializer" /&gt;</code></pre>
 */
public interface WonderPushInitializer {

    /**
     * Initialize the {@link WonderPush} SDK with your client id and client secret.
     *
     * <p>This method should retrieve the client id and client secret that you took care to hide securely in your application.</p>
     *
     * <p>
     *   It <i>must</i> then call {@link WonderPush#initialize(Context, String, String)} with the {@link Context} given in argument,
     *   and the retrieved credentials.
     * </p>
     *
     * <pre><code>@Override
     *public void initialize(Context context) {
     *    String clientId;     // retrieve these credentials
     *    String clientSecret; // from a protected storage
     *    WonderPush.initialize(context, clientId, clientSecret)
     *}</code></pre>
     *
     * @param context
     *            The {@link Context} you must pass on to {@link WonderPush#initialize(Context, String, String)}.
     */
    void initialize(Context context);

}
