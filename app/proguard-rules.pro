# linkkit API
-keep class com.aliyun.alink.**{*;}
-keep class com.aliyun.linksdk.**{*;}
-dontwarn com.aliyun.**
-dontwarn com.alibaba.**
-dontwarn com.alipay.**
-dontwarn com.ut.**

# keep native method
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep netty
-keepattributes Signature,InnerClasses
-keepclasseswithmembers class io.netty.** {
    *;
}
-keepnames class io.netty.** {
    *;
}
-dontwarn io.netty.**
-dontwarn sun.**

# keep mqtt
-keep public class org.eclipse.paho.**{*;}

# keep fastjson
-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.**{*;}

# keep gson
-keep class com.google.gson.** { *;}

# keep network core
-keep class com.http.**{*;}
-keep class org.mozilla.**{*;}

# keep okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.mozilla.**

-keep class okio.**{*;}
-keep class okhttp3.**{*;}
-keep class org.apache.commons.codec.**{*;}

-keep class com.aliyun.alink.devicesdk.demo.FileProvider{*;}
-keep class android.support.**{*;}
-keep class android.os.**{*;}

# keep gson
-keep class com.google.gson.** { *;}