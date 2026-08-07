# Root-only input capture is loaded by its stable binary name.
-keep class com.limelight.binding.input.evdev.* {*;}

# Native entry points and callbacks use statically named JNI symbols.
-keep class com.limelight.nvstream.jni.* {*;}

# These persisted documents use Gson field reflection. Keep only their schema
# members; the enclosing classes and ordinary methods remain optimizable and
# obfuscatable.
-keepclassmembers,allowoptimization class com.limelight.shortcuts.GameMenuShortcutDocumentCodec$Document {
    <fields>;
    <init>();
}
-keepclassmembers,allowoptimization class com.limelight.shortcuts.GameMenuShortcutDocumentCodec$Entry {
    <fields>;
    <init>();
}
-keepclassmembers,allowoptimization class com.limelight.shortcuts.LegacyGameMenuShortcutCodec$LegacyEntry {
    <fields>;
    <init>();
}
-keepclassmembers,allowoptimization class com.limelight.ui.gamemenu.bean.GameMenuQuickBean {
    <fields>;
    <init>();
}

# Okio
-keep class sun.misc.Unsafe {*;}
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**

# BouncyCastle
-keep class org.bouncycastle.jcajce.provider.asymmetric.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.util.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.* {*;}
-keep class org.bouncycastle.jcajce.provider.digest.** {*;}
-keep class org.bouncycastle.jcajce.provider.symmetric.** {*;}
-keep class org.bouncycastle.jcajce.spec.* {*;}
-keep class org.bouncycastle.jce.** {*;}
-dontwarn javax.naming.**

# jMDNS
-dontwarn javax.jmdns.impl.DNSCache
-dontwarn org.slf4j.**
