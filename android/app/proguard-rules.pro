# R8 keep rules for the release build.
#
# The app uses Moshi's REFLECTIVE Kotlin adapters (KotlinJsonAdapterFactory, no codegen), so the
# model classes' field/constructor names must survive shrinking/obfuscation or JSON breaks at
# runtime. Retrofit, OkHttp, Moshi and ML Kit ship their own consumer rules; these add the
# app-specific keeps on top.

# Reflection metadata used by Moshi-reflective and Retrofit generic signatures.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepattributes *Annotation*
-keepattributes Exceptions

# Kotlin metadata (Moshi reflective reads constructors/params via kotlin-reflect).
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# --- App JSON models (serialized reflectively by Moshi) ---
-keep class com.example.claims.android.data.** { *; }
-keepclassmembers class com.example.claims.android.data.** { *; }

# --- platform-login-ui wire DTOs + the Retrofit AuthApi it serializes ---
-keep class com.danovich.platform.login.ui.** { *; }
-keepclassmembers class com.danovich.platform.login.ui.** { *; }

# Retrofit service interfaces (annotations + suspend methods must be kept intact).
-keep interface com.example.claims.android.data.ClaimsApi { *; }
-keepclasseswithmembers interface * { @retrofit2.http.* <methods>; }

# Generic enum-by-name and data-class copy helpers Moshi may touch.
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Quiet known-safe missing references from transitive libs.
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
