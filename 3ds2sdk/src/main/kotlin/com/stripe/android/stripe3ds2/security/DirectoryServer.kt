package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.jwk.KeyUse
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException

/**
 * Known directory servers. The Stripe API may add support for directory servers that are not
 * enumerated as [DirectoryServer] constants.
 */
internal enum class DirectoryServer(
    val ids: List<String>,
    val algorithm: Algorithm,

    /**
     * A file that represents a public key certificate. Should only be used for certifying
     * the 3DS2 SDK. In production, the Stripe API will provide the certificate.
     */
    val fileName: String,

    val keyUse: KeyUse? = KeyUse.SIGNATURE
) {
    // public key for each algorithm defined in
    // "UL Test Harness Specification - UL 3DS Self Test Platform"
    TestRsa(
        listOf("F000000000"),
        Algorithm.RSA,
        "ds-test-rsa.txt"
    ),
    TestEc(
        listOf("F000000001"),
        Algorithm.EC,
        "ds-test-ec.txt"
    ),

    Visa(
        listOf("A000000003"),
        Algorithm.RSA,
        "ds-visa.crt"
    ),

    Mastercard(
        listOf("A000000004"),
        Algorithm.RSA,
        "ds-mastercard.crt"
    ),

    Amex(
        listOf("A000000025"),
        Algorithm.RSA,
        "ds-amex.pem"
    ),

    Discover(
        listOf("A000000152", "A000000324"),
        Algorithm.RSA,
        "ds-discover.cer",
        keyUse = null
    ),

    CartesBancaires(
        listOf("A000000042"),
        Algorithm.RSA,
        "ds-cartesbancaires.pem"
    );

    val isCertificate: Boolean
        get() = CERTIFICATE_EXTENSIONS.any { fileName.endsWith(it) }

    companion object {
        private val CERTIFICATE_EXTENSIONS = setOf(".crt", ".cer", ".pem")

        /**
         * Look up a known [DirectoryServer] based on a directory server id.
         *
         * If not found, throw an exception.
         */
        fun lookup(directoryServerId: String): DirectoryServer {
            val directoryServer = values().firstOrNull {
                it.ids.contains(directoryServerId)
            }
            return directoryServer ?: throw SDKRuntimeException(
                IllegalArgumentException("Unknown directory server id: $directoryServerId")
            )
        }
    }
}
