package com.wonderpush.sdk;

import android.content.Context;

/**
 * Interface for the self-initialization of the SDK.
 *
 * <p>
 *     You must implement this interface in a class named {@code WonderPushInitializerImpl}
 *     and placed directly in your Android application package.
 * </p>
 *
 * <p>
 *     You can actually customize the name and location of the implementation by adding the
 *     following in your {@code AndroidManifest.xml}:
 * </p>
 * <pre><code>&lt;manifest xmlns:android="http://schemas.android.com/apk/res/android"
 *    xmlns:tools="http://schemas.android.com/tools"
 *    package="COM.YOUR.PACKAGE"&gt;
 *
 *    &lt;application&gt;
 *
 *        &lt;!-- Permits the SDK to initialize itself whenever needed, without need for your application to launch --&gt;
 *        &lt;meta-data
 *            tools:node="replace"
 *            android:name="wonderpushInitializerClass"
 *            android:value="COM.SOME.OTHER.PACKAGE.MyWonderPushInitializerCustomClassName" /&gt;
 *
 *    &lt;/application&gt;
 *
 *&lt;/manifest&gt;</code></pre>
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
