package com.stripe.android.stripe3ds2.security

import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

internal class EcKeyFactory constructor(
    private val errorReporter: ErrorReporter
) {
    private val keyFactory: KeyFactory =
        runCatching {
            KeyFactory.getInstance("EC")
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse { error ->
            throw SDKRuntimeException(error)
        }

    /**
     * @param publicKeyEncoded byte array representation of a public key
     *
     * @return an [ECPublicKey] generated from the byte array
     */
    fun createPublic(publicKeyEncoded: ByteArray) =
        runCatching {
            keyFactory.generatePublic(X509EncodedKeySpec(publicKeyEncoded)) as ECPublicKey
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse {
            throw SDKRuntimeException(it)
        }

    /**
     * @param privateKeyEncoded byte array representation of a private key
     *
     * @return an [ECPrivateKey] generated from the byte array
     */
    fun createPrivate(privateKeyEncoded: ByteArray) =
        runCatching {
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyEncoded)) as ECPrivateKey
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
}
