# R8 is enabled for release for speed, not size.
# Compose is designed to run through R8 — it strips debug synthetic wrappers,
# inlines composables, and makes the UI run significantly faster.

# Nothing is renamed. Stack traces stay readable, and every lookup by name —
# enum names in preferences, Media3 custom commands, library reflection — keeps
# working without a rule of its own.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Exceptions,*Annotation*

# ---- Script engines and extractors ------------------------------------------
# Rhino compiles and runs YouTube's player JavaScript and binds Java classes into
# it reflectively; NewPipe and InnerTube drive it and parse with nanojson/jsoup.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class org.schabi.newpipe.** { *; }
-keep class com.grack.nanojson.** { *; }
-keep class org.jsoup.** { *; }
-keep class com.metrolist.** { *; }
-keep class com.dokar.quickjs.** { *; }

# ---- Native and crypto --------------------------------------------------------
# ONNX Runtime and QuickJS are called from JNI, which R8 cannot see. SMBJ picks
# its BouncyCastle providers and event bus handlers reflectively.
-keep class ai.onnxruntime.** { *; }
-keep class com.hierynomus.** { *; }
-keep class net.engio.mbassy.** { *; }
-keep class org.bouncycastle.** { *; }

# ---- Networking & Serialization ---------------------------------------------
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# ---- Data models & Room -----------------------------------------------------
-keep class com.music.dhvani.data.model.** { *; }
-keep class com.music.dhvani.data.innertube.** { *; }

# ---- Media3 & ExoPlayer ------------------------------------------------------
-keep class androidx.media3.** { *; }

# ---- Firebase ----------------------------------------------------------------
-keep class com.google.firebase.** { *; }

# ---- Suppress optional desktop JVM classes not present on Android -----------
-dontwarn java.beans.**
-dontwarn java.lang.management.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**