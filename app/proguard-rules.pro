# -------------------------------------------------------------
# General Attributes & Optimization
# -------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes Exceptions,Deprecated,Synthetic

# Keep native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# -------------------------------------------------------------
# Media3 & NextLib JNI
# -------------------------------------------------------------
-dontwarn androidx.media3.**
-dontwarn io.github.anilbeesetti.nextlib.**

-keep class io.github.anilbeesetti.nextlib.mediainfo.** { *; }

# -------------------------------------------------------------
# Kotlin Coroutines & Reflection
# -------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# -------------------------------------------------------------
# KotlinX Serialization
# -------------------------------------------------------------
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}
-dontwarn kotlinx.serialization.**

# -------------------------------------------------------------
# Koin Dependency Injection
# -------------------------------------------------------------
-keepclassmembers class * {
    @org.koin.core.annotation.* <init>(...);
    @org.koin.core.annotation.* <methods>;
}
-dontwarn org.koin.**

# -------------------------------------------------------------
# Coil 3 Image Loading
# -------------------------------------------------------------
-dontwarn coil3.**

# -------------------------------------------------------------
# Jetpack Room & SQLite
# -------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.**

# -------------------------------------------------------------
# AboutLibraries
# -------------------------------------------------------------
-keep class com.mikepenz.aboutlibraries.** { *; }
-dontwarn com.mikepenz.aboutlibraries.**

# -------------------------------------------------------------
# Reorderable
# -------------------------------------------------------------
-dontwarn sh.calvin.reorderable.**

# -------------------------------------------------------------
# Universal Chardet
# -------------------------------------------------------------
-dontwarn org.mozilla.universalchardet.**

# -------------------------------------------------------------
# SMBJ, SSHJ, Sardine, BouncyCastle, Commons Net
# -------------------------------------------------------------
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**

-dontwarn com.hierynomus.**
-dontwarn net.schmizz.sshj.**
-dontwarn com.thegrizzlylabs.sardineandroid.**
-dontwarn org.apache.commons.net.**
-dontwarn org.bouncycastle.**
-dontwarn net.engio.mbassy.**

-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

-dontwarn ch.boye.httpclientandroidlib.**
-dontwarn org.apache.http.**
-dontwarn com.jcraft.jzlib.**
-dontwarn net.i2p.crypto.eddsa.**
-dontwarn org.slf4j.**
-dontwarn javax.el.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.ietf.jgss.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.**
-dontwarn okio.**


