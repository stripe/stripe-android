# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class org.bouncycastle.jcajce.provider.asymmetric.RSA$Mappings
-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSA
-keep class org.bouncycastle.jcajce.provider.asymmetric.EC$Mappings
-keep class org.bouncycastle.jcajce.provider.asymmetric.ec.SignatureSpi$ecDSA256
-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305
