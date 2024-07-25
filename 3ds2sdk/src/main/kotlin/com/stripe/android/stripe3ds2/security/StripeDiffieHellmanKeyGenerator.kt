package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.crypto.impl.ConcatKDF
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.util.Base64URL
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import javax.crypto.SecretKey

internal class StripeDiffieHellmanKeyGenerator(
    private val errorReporter: ErrorReporter
) : DiffieHellmanKeyGenerator {

    /**
     * Implementation as defined in "EMV® 3-D Secure SDK Technical Guide - Section 3.3.4"
     */
    override fun generate(
        acsPublicKey: ECPublicKey,
        sdkPrivateKey: ECPrivateKey,
        agreementInfo: String
    ): SecretKey {
        return runCatching {
            val keyDerivationFunction = ConcatKDF(HASH_ALGO)
            keyDerivationFunction.deriveKey(
                ECDH.deriveSharedSecret(acsPublicKey, sdkPrivateKey, null),
                KEY_LENGTH,
                ConcatKDF.encodeStringData(null),
                ConcatKDF.encodeDataWithLength(null as Base64URL?),
                ConcatKDF.encodeDataWithLength(Base64URL.encode(agreementInfo)),
                ConcatKDF.encodeIntData(KEY_LENGTH),
                ConcatKDF.encodeNoData()
            )
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
    }

    private companion object {
        private const val HASH_ALGO = "SHA-256"
        private const val KEY_LENGTH = 256
    }
}
