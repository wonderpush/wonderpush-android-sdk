# [BEGIN] of WonderPush SDK proguard rules
# These proguard rules comes from the WonderPush SDK, and are imported to your application.

# Keep the initializer implementation (from the app itself), as it's referenced by classname from the manifest
#-keep public interface com.wonderpush.sdk.WonderPushInitializer # keeping the interface itself is not necessary
-keep public class * implements com.wonderpush.sdk.WonderPushInitializer {
    public void initialize(android.content.Context);
}

# Keep configuration from BuildConfig
-keep class **.BuildConfig {
    *** WONDERPUSH_*;
}

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

## For easier debugging, we strongly recommend keeping WonderPush class names unobfuscated
#-keepattributes SourceFile,LineNumberTable
#-keepnames class com.wonderpush.sdk.** {
#    *;
#}
#-keepnames interface com.wonderpush.sdk.** {
#    *;
#}

# [END] of WonderPush SDK proguard rules
