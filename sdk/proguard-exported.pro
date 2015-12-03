# [BEGIN] of WonderPush SDK proguard rules
# These proguard rules comes from the WonderPush SDK, and are imported to your application.

# Keep the initializer implementation, as it's referenced by classname from the manifest
#-keep public interface com.wonderpush.sdk.WonderPushInitializer # keeping the interface itself is not necessary
-keep public class * implements com.wonderpush.sdk.WonderPushInitializer {
    public void initialize(android.content.Context);
}

# [END] of WonderPush SDK proguard rules
