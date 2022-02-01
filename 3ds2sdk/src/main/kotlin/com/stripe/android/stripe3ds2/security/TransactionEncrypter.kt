package com.stripe.android.stripe3ds2.security

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWECryptoParts
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.KeyLengthException
import com.nimbusds.jose.crypto.DirectEncrypter
import com.nimbusds.jose.crypto.impl.AAD
import com.nimbusds.jose.crypto.impl.AESCBC
import com.nimbusds.jose.crypto.impl.AESGCM
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage
import com.nimbusds.jose.crypto.impl.AuthenticatedCipherText
import com.nimbusds.jose.crypto.impl.DeflateHelper
import com.nimbusds.jose.crypto.impl.DirectCryptoProvider
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.ByteUtils
import com.nimbusds.jose.util.Container
import java.util.Arrays
import javax.crypto.spec.SecretKeySpec

/**
 * Used for Encryption of CReq / Decryption of CRes messages.
 *
 * See "SDK Technical Guide - Section 3.4.1 - Encryption of CReq / Decryption of CRes - Android"
 */
internal class TransactionEncrypter @Throws(KeyLengthException::class) constructor(
    key: ByteArray,
    private val counter: Byte
) : DirectEncrypter(SecretKeySpec(key, "AES")) {

    @Throws(JOSEException::class)
    override fun encrypt(header: JWEHeader, clearText: ByteArray): JWECryptoParts {
        val alg = header.algorithm
        if (alg != JWEAlgorithm.DIR) {
            throw JOSEException("Invalid algorithm $alg")
        }

        val enc = header.encryptionMethod

        // Check key length matches encryption method
        if (enc.cekBitLength() != ByteUtils.bitLength(key.encoded)) {
            throw KeyLengthException(enc.cekBitLength(), enc)
        }

        val encryptedKey: Base64URL? = null // The second JWE part
        if (enc.cekBitLength() != ByteUtils.bitLength(key.encoded)) {
            throw KeyLengthException(
                "The Content Encryption Key length for $enc must be ${enc.cekBitLength()} bits"
            )
        }

        // Apply compression if instructed
        val plainText = DeflateHelper.applyCompression(header, clearText)
        // Compose the AAD
        val aad = AAD.compute(header)
        // Encrypt the plain text according to the JWE enc
        val iv: ByteArray
        val authCipherText: AuthenticatedCipherText
        when {
            header.encryptionMethod == EncryptionMethod.A128CBC_HS256 -> {
                iv = Crypto.getGcmIvStoA(AESCBC.IV_BIT_LENGTH, counter)
                authCipherText = AESCBC.encryptAuthenticated(
                    key, iv, plainText, aad,
                    jcaContext.contentEncryptionProvider,
                    jcaContext.macProvider
                )
            }
            header.encryptionMethod == EncryptionMethod.A128GCM -> {
                iv = Crypto.getGcmIvStoA(AESGCM.IV_BIT_LENGTH, counter)
                authCipherText = AESGCM.encrypt(key, Container(iv), plainText, aad, null)
            }
            else -> throw JOSEException(
                AlgorithmSupportMessage.unsupportedEncryptionMethod(
                    header.encryptionMethod, DirectCryptoProvider.SUPPORTED_ENCRYPTION_METHODS
                )
            )
        }

        return JWECryptoParts(
            header, encryptedKey, Base64URL.encode(iv),
            Base64URL.encode(authCipherText.cipherText),
            Base64URL.encode(authCipherText.authenticationTag)
        )
    }

    internal object Crypto {
        private const val BITS_IN_BYTE = 8

        /**
         * AES GCM utilizes 96 bit IVs and AES CBC uses 128 bit IVs
         *
         * @param length length of IV in bits
         * @param sdkCounterStoA counter value
         * @return Initialisation Vector (IV) with given counter and given length
         */
        fun getGcmIvStoA(length: Int, sdkCounterStoA: Byte): ByteArray {
            return getGcmId(length, 0x00.toByte(), sdkCounterStoA)
        }

        private fun getGcmIvAtoS(length: Int, sdkCounterAtoS: Byte): ByteArray {
            return getGcmId(length, 0xFF.toByte(), sdkCounterAtoS)
        }

        private fun getGcmId(length: Int, pad: Byte, counter: Byte): ByteArray {
            val iv = ByteArray(length / BITS_IN_BYTE)
            Arrays.fill(iv, pad)
            iv[iv.size - 1] = counter
            return iv
        }
    }
}
