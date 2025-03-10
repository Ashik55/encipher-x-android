# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# JNA
-dontwarn java.awt.*
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# TagSoup, coming from the RTE library
-keep class org.ccil.cowan.tagsoup.** { *; }

# kotlinx.serialization

# Kotlin serialization looks up the generated serializer classes through a function on companion
# objects. The companions are looked up reflectively so we need to explicitly keep these functions.
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# If a companion has the serializer function, keep the companion field on the original type so that
# the reflective lookup succeeds.
-if class **.*$Companion {
  kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>.<2> {
  <1>.<2>$Companion Companion;
}

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
# Taken from https://raw.githubusercontent.com/square/okhttp/master/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Needed for Posthog
-keepclassmembers class android.view.JavaViewSpy {
    static int windowAttachCount(android.view.View);
}

-keep class io.element.android.x.di.** { *; }

# Facebook Fresco WebP Support
-dontwarn com.facebook.imagepipeline.nativecode.WebpTranscoder
-keep class com.facebook.imagepipeline.nativecode.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.chromium.build.BuildHooksAndroid

# Jitsi Meet SDK
-keep class org.jitsi.meet.** { *; }
-keep class org.jitsi.meet.sdk.** { *; }

# React Native related rules
-keep class com.facebook.react.bridge.CatalystInstanceImpl { *; }
-keep class com.facebook.react.bridge.ExecutorToken { *; }
-keep class com.facebook.react.bridge.JavaScriptExecutor { *; }
-keep class com.facebook.react.bridge.ModuleRegistryHolder { *; }
-keep class com.facebook.react.bridge.ReadableType { *; }
-keep class com.facebook.react.bridge.queue.NativeRunnable { *; }
-keep class com.facebook.react.devsupport.** { *; }

-dontwarn com.facebook.react.devsupport.**
-dontwarn com.google.appengine.**
-dontwarn com.squareup.okhttp.**
-dontwarn javax.servlet.**

# Rule to avoid build errors related to SVGs
-keep public class com.horcrux.svg.** {*;}

# Media3 and ExoPlayer2 conflict resolution
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**
# Prevent class renaming that could cause conflicts
-keepnames class androidx.media3.** { *; }
-keepnames class com.google.android.exoplayer2.** { *; }

# If JitsiMeetSDK tries to create an ExoPlayer2 PlayerView
-dontwarn com.google.android.exoplayer2.ui.PlayerView
-dontwarn com.google.android.exoplayer2.ui.DefaultTimeBar
-dontwarn com.google.android.exoplayer2.ui.TimeBar

# Keep our stub implementation of ReactVideoPackage
-keep class com.brentvatne.react.ReactVideoPackage { *; }
