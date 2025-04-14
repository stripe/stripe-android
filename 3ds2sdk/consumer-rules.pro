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

# current master
#-keep class org.bouncycastle.jcajce.provider.** { *; }
#-keep class !org.bouncycastle.jce.provider.X509LDAPCertStoreSpi,org.bouncycastle.jce.provider.** { *; }
#-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305

## original
#-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305
#-dontwarn org.bouncycastle.**

## can do better?
-keep class org.bouncycastle.jcajce.provider.asymmetric.**
-keep class org.bouncycastle.jcajce.provider.symmetric.**
-keep class !org.bouncycastle.jce.provider.X509LDAPCertStoreSpi,org.bouncycastle.jce.provider.** { *; }
-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305


#-keep class org.bouncycastle.jcajce.provider.** { *; }
#-keep class org.bouncycastle.jce.provider.** { *; }
#-keep class !org.bouncycastle.jce.provider.X509LDAPCertStoreSpi { *; }
#-keep class !org.bouncycastle.jce.provider.X509LDAPCertStoreSpi,org.bouncycastle.jce.provider.** { *; }
