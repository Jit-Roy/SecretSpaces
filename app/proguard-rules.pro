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

# Keep Cloudinary classes used via reflection
-keep class com.cloudinary.** { *; }
-keep class com.cloudinary.android.** { *; }
-dontwarn com.cloudinary.**

# Keep Firebase models and avoid stripping annotations
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep MapLibre (uses reflection for some plugin loading)
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Keep FileProvider
-keep class androidx.core.content.FileProvider { *; }

# Keep Kotlin metadata for data classes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Compose/Material usually handled by AGP, but keep views just in case
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
