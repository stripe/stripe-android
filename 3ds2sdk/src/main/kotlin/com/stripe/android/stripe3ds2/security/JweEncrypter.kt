package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.JOSEException
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.ParseException

/**
 * A JWE encrypter used to encrypt device data.
 */
internal fun interface JweEncrypter {
    fun encrypt(
        payload: String,
        acsPublicKey: PublicKey,
        directoryServerId: String,
        keyId: String?
    ): String
}

internal class DefaultJweEncrypter private constructor(
    private val jweRsaEncrypter: JweRsaEncrypter,
    private val jweEcEncrypter: JweEcEncrypter,
    private val errorReporter: ErrorReporter
) : JweEncrypter {
    constructor(
        ephemeralKeyPairGenerator: EphemeralKeyPairGenerator,
        errorReporter: ErrorReporter
    ) : this(
        JweRsaEncrypter(),
        JweEcEncrypter(
            ephemeralKeyPairGenerator,
            errorReporter
        ),
        errorReporter
    )

    @Throws(JOSEException::class, ParseException::class)
    override fun encrypt(
        payload: String,
        acsPublicKey: PublicKey,
        directoryServerId: String,
        keyId: String?
    ): String {
        return when (acsPublicKey) {
            is RSAPublicKey -> Result.success(
                jweRsaEncrypter.encrypt(payload, acsPublicKey, keyId)
            )
            is ECPublicKey -> Result.success(
                jweEcEncrypter.encrypt(payload, acsPublicKey, directoryServerId)
            )
            else ->
                Result.failure(
                    SDKRuntimeException(
                        "Unsupported public key algorithm: ${acsPublicKey.algorithm}"
                    )
                )
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrThrow()
    }
}
