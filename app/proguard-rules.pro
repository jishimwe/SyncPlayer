# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WorkManager: keep InputMerger subclasses and their no-arg constructors
# R8 strips constructors used only via reflection; this prevents that
-keep class * extends androidx.work.InputMerger { *; }
-keepclassmembers class * extends androidx.work.InputMerger {
    <init>();
}

# Glance AppWidget: keep ActionCallback subclasses used for widget buttons
-keep class * extends androidx.glance.appwidget.action.ActionCallback

# gRPC OkHttp transport references OkHttp 2.x classes that aren't on the classpath;
# these are optional and not needed at runtime
-dontwarn com.squareup.okhttp.**

# Credential Manager: keep Play Services provider and Google ID library used via reflection
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Firebase Firestore: keep model classes and gRPC internals used via reflection
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.** { *; }
-keep class io.grpc.** { *; }
-keepattributes Signature
-keepattributes *Annotation*