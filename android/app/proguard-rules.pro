-keep class androidx.appcompat.widget.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}
-keep class com.coremedia.iso.** {*;}
-keep class com.googlecode.mp4parser.** {*;}
-keep class com.mp4parser.** {*;}
-dontwarn com.coremedia.**
-dontwarn com.googlecode.mp4parser.**
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$OutcomingTextMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$IncomingTextMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$IncomingImageMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$OutcomingImageMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}