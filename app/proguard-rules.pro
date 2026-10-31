# Mülkora ProGuard / R8 kuralları
# CLAUDE.md §35: Güvenlik — üretim kodunda hassas bilgi gizlenmemeli.

# =============================================
# Firebase
# =============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore data class mapping (DTO'lar)
-keepclassmembers class com.mulkora.** {
    @com.google.firebase.firestore.PropertyName *;
}
-keep class com.mulkora.**.dto.** { *; }
-keep class com.mulkora.**.data.**.Dto { *; }

# =============================================
# Kotlin / Coroutines
# =============================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# =============================================
# Hilt / Dagger
# =============================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# =============================================
# Jetpack Compose
# =============================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# =============================================
# Google Play Billing
# =============================================
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# =============================================
# AdMob
# =============================================
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# =============================================
# Coil (image loading)
# =============================================
-dontwarn coil.**

# =============================================
# WorkManager
# =============================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# =============================================
# Retrofit / OkHttp (core-network için)
# =============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# =============================================
# Serialization / Gson
# =============================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# =============================================
# Genel
# =============================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile