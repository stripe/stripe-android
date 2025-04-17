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

#-keep class org.bouncycastle.jcajce.provider.** { *; }
#-keep class !org.bouncycastle.jce.provider.X509LDAPCertStoreSpi,org.bouncycastle.jce.provider.** { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.RSA$Mappings
-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSA
-keep class org.bouncycastle.jcajce.provider.asymmetric.EC$Mappings
-keep class org.bouncycastle.jcajce.provider.asymmetric.ec.SignatureSpi$ecDSA256
-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA256

#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding_PublicOnly
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding_PrivateOnly
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$ISO9796d1Padding
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$NoPadding
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$OAEPPadding
#
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$OAEP
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$PSS
#
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateKey
#
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.CustomPKCS1Encoding
#
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyPairGeneratorSpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyPairGeneratorSpi$PSS

#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD2
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD4
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD5
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$noneRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD160
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA1
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_224
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_384
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_512
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA224

#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA384
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512_224
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512_256

#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$MD5WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$RIPEMD160WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA1WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA224WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA256WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA384WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA512_224WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA512_256WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA512WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$WhirlpoolWithRSAEncryption
#
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.RSAUtil
#
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$nonePSS
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$NullPssDigest
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$PSSwithRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA1withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA1withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA1withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_224withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_224withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_224withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_256withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_256withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_256withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_384withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_384withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_384withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_512withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_512withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_512withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA224withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA224withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA224withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA384withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA384withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA384withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_224withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_224withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_224withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_256withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_256withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_256withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512withRSA
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512withRSAandSHAKE128
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512withRSAandSHAKE256
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHAKE128WithRSAPSS
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHAKE256WithRSAPSS
#
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$RIPEMD128WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$RIPEMD160WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA1WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA224WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA256WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA384WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA512_224WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA512_256WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA512WithRSAEncryption
#-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$WhirlpoolWithRSAEncryption


-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305
