package com.stripe.android.model

import androidx.annotation.VisibleForTesting
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

internal class Stripe3ds2Fingerprint private constructor(
    val source: String,
    val directoryServer: DirectoryServer,
    val serverTransactionId: String,
    val directoryServerEncryption: DirectoryServerEncryption
) {
    class DirectoryServerEncryption @VisibleForTesting
    @Throws(CertificateException::class)
    internal constructor(
        val directoryServerId: String,
        dsCertificateData: String,
        rootCertsData: List<String>,
        val keyId: String?
    ) {
        val directoryServerPublicKey: PublicKey = generateCertificate(dsCertificateData).publicKey
        val rootCerts: List<X509Certificate> = generateCertificates(rootCertsData)

        @Throws(CertificateException::class)
        private fun generateCertificates(certificatesData: List<String>): List<X509Certificate> {
            return certificatesData.map { generateCertificate(it) }
        }

        @Throws(CertificateException::class)
        private fun generateCertificate(certificateData: String): X509Certificate {
            val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(certificateData.toByteArray()))
            return certificate as X509Certificate
        }

        internal companion object {
            private const val FIELD_DIRECTORY_SERVER_ID = "directory_server_id"
            private const val FIELD_CERTIFICATE = "certificate"
            private const val FIELD_KEY_ID = "key_id"
            private const val FIELD_ROOT_CAS = "root_certificate_authorities"

            @JvmSynthetic
            @Throws(CertificateException::class)
            internal fun create(data: Map<String, *>): DirectoryServerEncryption {
                val rootCertData: List<String> = if (data.containsKey(FIELD_ROOT_CAS)) {
                    data[FIELD_ROOT_CAS] as List<String>
                } else {
                    emptyList()
                }
                return DirectoryServerEncryption(
                    data[FIELD_DIRECTORY_SERVER_ID] as String,
                    data[FIELD_CERTIFICATE] as String,
                    rootCertData,
                    data[FIELD_KEY_ID] as String?
                )
            }
        }
    }

    internal enum class DirectoryServer constructor(val networkName: String, val id: String) {
        Visa("visa", "A000000003"),
        Mastercard("mastercard", "A000000004"),
        Amex("american_express", "A000000025");

        internal companion object {
            @JvmSynthetic
            internal fun lookup(networkName: String): DirectoryServer {
                return values().find { it.networkName == networkName }
                    ?: error("Invalid directory server networkName: '$networkName'")
            }
        }
    }

    internal companion object {

        @JvmSynthetic
        @Throws(CertificateException::class)
        internal fun create(sdkData: StripeIntent.NextActionData.SdkData.`3DS2`): Stripe3ds2Fingerprint {
            return Stripe3ds2Fingerprint(
                sdkData.source,
                DirectoryServer.lookup(sdkData.serverName),
                sdkData.transactionId,
                DirectoryServerEncryption(
                    sdkData.serverEncryption.directoryServerId,
                    sdkData.serverEncryption.dsCertificateData,
                    sdkData.serverEncryption.rootCertsData,
                    sdkData.serverEncryption.keyId
                )
            )
        }
    }
}
