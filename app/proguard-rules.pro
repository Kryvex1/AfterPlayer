# -------------------------------------------------------------
# Android & Core Optimization
# -------------------------------------------------------------
-dontobfuscate

# General Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepattributes Exceptions,Deprecated,Synthetic

# Keep all native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# -------------------------------------------------------------
# Media3 & ExoPlayer
# -------------------------------------------------------------
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# -------------------------------------------------------------
# NextLib FFmpeg & MediaInfo JNI Decoders
# -------------------------------------------------------------
-keep class io.github.anilbeesetti.nextlib.** { *; }
-keep interface io.github.anilbeesetti.nextlib.** { *; }
-dontwarn io.github.anilbeesetti.nextlib.**

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
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# -------------------------------------------------------------
# Koin Dependency Injection
# -------------------------------------------------------------
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-dontwarn org.koin.**

# -------------------------------------------------------------
# Coil 3 Image Loading & Custom Decoders
# -------------------------------------------------------------
-keep class coil3.** { *; }
-dontwarn coil3.**
-keep class dev.anilbeesetti.nextplayer.** { *; }

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
-keep class sh.calvin.reorderable.** { *; }
-dontwarn sh.calvin.reorderable.**

# -------------------------------------------------------------
# Universal Chardet
# -------------------------------------------------------------
-keep class org.mozilla.universalchardet.** { *; }
-dontwarn org.mozilla.universalchardet.**

# -------------------------------------------------------------
# SMBJ & Network Providers (SMB, SFTP, WebDAV, FTP)
# -------------------------------------------------------------
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**

-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

-keep class net.schmizz.sshj.** { *; }
-dontwarn net.schmizz.sshj.**

-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-dontwarn com.thegrizzlylabs.sardineandroid.**

-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keep class net.engio.mbassy.** { *; }
-dontwarn net.engio.mbassy.**

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

