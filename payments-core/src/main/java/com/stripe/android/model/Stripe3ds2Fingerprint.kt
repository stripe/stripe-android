package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

@Parcelize
internal data class Stripe3ds2Fingerprint internal constructor(
    val source: String,
    val directoryServerName: String,
    val serverTransactionId: String,
    val directoryServerEncryption: DirectoryServerEncryption
) : Parcelable {
    @Throws(CertificateException::class)
    internal constructor(
        sdkData: StripeIntent.NextActionData.SdkData.Use3DS2
    ) : this(
        sdkData.source,
        sdkData.serverName,
        sdkData.transactionId,
        DirectoryServerEncryption(
            sdkData.serverEncryption.directoryServerId,
            sdkData.serverEncryption.dsCertificateData,
            sdkData.serverEncryption.rootCertsData,
            sdkData.serverEncryption.keyId
        )
    )

    @Parcelize
    internal data class DirectoryServerEncryption internal constructor(
        val directoryServerId: String,
        val directoryServerPublicKey: PublicKey,
        val rootCerts: List<X509Certificate>,
        val keyId: String?
    ) : Parcelable {
        internal constructor(
            directoryServerId: String,
            dsCertificateData: String,
            rootCertsData: List<String>,
            keyId: String?
        ) : this(
            directoryServerId,
            generateCertificate(dsCertificateData).publicKey,
            generateCertificates(rootCertsData),
            keyId
        )

        private companion object {
            @Throws(CertificateException::class)
            private fun generateCertificates(
                certificatesData: List<String>
            ): List<X509Certificate> {
                return certificatesData.map { generateCertificate(it) }
            }

            @Throws(CertificateException::class)
            private fun generateCertificate(
                certificateData: String
            ): X509Certificate {
                val certificate = CertificateFactory.getInstance("X.509")
                    .generateCertificate(ByteArrayInputStream(certificateData.toByteArray()))
                return certificate as X509Certificate
            }
        }
    }
}
