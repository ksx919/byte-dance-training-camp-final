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
# ==========================================================
# 1. 核心修复：数据模型 & API 接口 (必须保持原样！)
# ==========================================================
# 保护实体类 (PostInfo 等)
-keep class com.rednote.data.model.** { *; }
# 【新增】保护 API 接口 (PostApiService)，Retrofit 需要反射它
-keep class com.rednote.data.api.** { *; }

# ==========================================================
# 2. 核心修复：Kotlin 协程与元数据 (解决 suspend 崩溃)
# ==========================================================
# Retrofit 解析 suspend 函数需要读取 Kotlin Metadata
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
# 【新增】关键修复：Retrofit 需要看到 Continuation 的泛型类型
-keep interface kotlin.coroutines.Continuation
-keep class kotlin.coroutines.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# ==========================================================
# 3. 核心修复：保留泛型签名
# ==========================================================
# 这一行至关重要！没有它，List<PostInfo> 会变成 List，导致崩溃
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# ==========================================================
# 4. Gson 规则
# ==========================================================
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==========================================================
# 5. Retrofit & OkHttp 规则
# ==========================================================
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Exceptions

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ==========================================================
# 6. Glide 规则
# ==========================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# ==========================================================
# 7. 其他通用防崩溃规则
# ==========================================================
# 如果使用了 AndroidX 的 ViewModel/Lifecycle
-keep class androidx.lifecycle.** { *; }

# 忽略一些无关紧要的警告
-ignorewarnings