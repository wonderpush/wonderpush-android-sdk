package com.wonderpush.sdk;

/**
 * The sole purpose of this class is to give a distinct android:name attribute to the &lt;provider&gt;
 * entry in AndroidManifest.xml in order to avoid conflicts with client applications.
 *
 * @hide
 */
public class WonderPushFileProvider extends androidx.core.content.FileProvider {
}
