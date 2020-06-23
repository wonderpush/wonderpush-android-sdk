# [BEGIN] of WonderPush SDK HCM proguard rules
# These proguard rules comes from the WonderPush SDK HCM, and are imported to your application.

# Huawei Mobile Services (HMS) support
-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.hianalytics.android.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}

# [END] of WonderPush SDK HCM proguard rules
