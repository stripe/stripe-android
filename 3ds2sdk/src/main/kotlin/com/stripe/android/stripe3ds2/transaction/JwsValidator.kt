package com.stripe.android.stripe3ds2.transaction

import androidx.annotation.VisibleForTesting
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory
import com.nimbusds.jose.util.Base64
import com.nimbusds.jose.util.X509CertChainUtils
import com.nimbusds.jose.util.X509CertUtils
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.cert.CertPathBuilder
import java.security.cert.CertStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.CollectionCertStoreParameters
import java.security.cert.PKIXBuilderParameters
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate
import java.text.ParseException
import java.util.Locale
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * JWS validator to validate ARes's `acsSignedContent` field.
 */
internal fun interface JwsValidator {
    fun getPayload(jws: String): JSONObject
}

/**
 * @param isLiveMode flag to indicate whether challenge is in live or test mode. If not in
 * live mode, don't validate the JWS object
 * @param rootCerts the [X509Certificate]s for the card network
 */
internal class DefaultJwsValidator(
    private val isLiveMode: Boolean,
    private val rootCerts: List<X509Certificate>,
    private val errorReporter: ErrorReporter
) : JwsValidator {
    /**
     * @param jws a JWS string
     * @return a [JSONObject] representing the payload if JWS was successfully verify;
     * otherwise, a [SDKRuntimeException] will be thrown}
     */
    @Throws(
        JSONException::class,
        ParseException::class,
        JOSEException::class,
        CertificateException::class
    )
    override fun getPayload(
        jws: String
    ): JSONObject {
        val jwsObject = JWSObject.parse(jws)

        if (!isLiveMode) {
            if (!jwsObject.header.x509CertChain.isNullOrEmpty()) {
                val certificates = jwsObject.header.x509CertChain.mapNotNull { certificateFromString(it.toString()) }

                if (certificates.isNotEmpty() && isValid(jwsObject, certificates)) {
                    return JSONObject(jwsObject.payload.toString())
                }

                throw IllegalStateException("Could not validate JWS")
            } else {
                return JSONObject(jwsObject.payload.toString())
            }
        } else if (isValid(jwsObject, rootCerts)) {
            return JSONObject(jwsObject.payload.toString())
        }

        throw IllegalStateException("Could not validate JWS")
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun certificateFromString(base64: String): X509Certificate? {
        val decoded = kotlin.io.encoding.Base64.decode(base64)
        val inputStream = ByteArrayInputStream(decoded)

        return CertificateFactory.getInstance("X.509").generateCertificate(inputStream) as? X509Certificate
    }

    @Throws(JOSEException::class, CertificateException::class)
    private fun isValid(
        jwsObject: JWSObject,
        rootCerts: List<X509Certificate>
    ): Boolean {
        if (jwsObject.header.jwk != null) {
            errorReporter.reportError(
                IllegalArgumentException("Encountered a JWK in ${jwsObject.header}")
            )
        }
        val jwsHeader = sanitizedJwsHeader(jwsObject.header)
        if (!isCertificateChainValid(jwsHeader.x509CertChain, rootCerts)) {
            return false
        }

        val verifier = getVerifier(jwsHeader)
        return jwsObject.verify(verifier)
    }

    @Throws(JOSEException::class, CertificateException::class)
    private fun getVerifier(jwsHeader: JWSHeader): JWSVerifier {
        val verifierFactory = DefaultJWSVerifierFactory()
        verifierFactory.jcaContext.provider = MessageDigest.getInstance("SHA-256").provider
        return verifierFactory.createJWSVerifier(jwsHeader, getPublicKeyFromHeader(jwsHeader))
    }

    @Throws(CertificateException::class)
    private fun getPublicKeyFromHeader(jwsHeader: JWSHeader): PublicKey {
        return X509CertUtils.parseWithException(
            jwsHeader.x509CertChain.first().decode()
        ).publicKey
    }

    @VisibleForTesting
    fun isCertificateChainValid(
        encodedChainCerts: List<Base64>?,
        rootCerts: List<X509Certificate>
    ): Boolean {
        return runCatching {
            require(!encodedChainCerts.isNullOrEmpty()) {
                "JWSHeader's X.509 certificate chain is null or empty"
            }
            require(rootCerts.isNotEmpty()) {
                "Root certificates are empty"
            }

            validateChain(encodedChainCerts, rootCerts)
        }.onFailure {
            errorReporter.reportError(it)
        }.isSuccess
    }

    companion object {
        /**
         * References
         * - https://docs.oracle.com/javase/7/docs/api/java/security/cert/CertPathBuilder.html
         * - https://stackoverflow.com/a/2458343/11103900
         */
        @Throws(GeneralSecurityException::class, IOException::class, ParseException::class)
        private fun validateChain(
            encodedChainCerts: List<Base64>,
            rootCerts: List<X509Certificate>
        ) {
            val chainCerts = X509CertChainUtils.parse(encodedChainCerts)

            val keyStore = createKeyStore(rootCerts)
            val target = X509CertSelector()
            target.certificate = chainCerts[0]

            val params = PKIXBuilderParameters(keyStore, target)
            params.isRevocationEnabled = false
            params.addCertStore(
                CertStore.getInstance(
                    "Collection",
                    CollectionCertStoreParameters(chainCerts)
                )
            )

            // throws an exception if the builder is unable to construct a certification path
            CertPathBuilder.getInstance("PKIX").build(params)
        }

        @VisibleForTesting
        @Throws(
            KeyStoreException::class,
            CertificateException::class,
            NoSuchAlgorithmException::class,
            IOException::class
        )
        fun createKeyStore(rootCerts: List<X509Certificate>): KeyStore {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            rootCerts.forEachIndexed { index, _ ->
                keyStore.setCertificateEntry(
                    String.format(Locale.ROOT, "ca_%d", index),
                    rootCerts[index]
                )
            }
            return keyStore
        }

        /**
         * @return the original `JWSHeader` without a `jwk`
         */
        internal fun sanitizedJwsHeader(jwsHeader: JWSHeader): JWSHeader {
            return JWSHeader.Builder(jwsHeader)
                .jwk(null)
                .build()
        }
    }
}
