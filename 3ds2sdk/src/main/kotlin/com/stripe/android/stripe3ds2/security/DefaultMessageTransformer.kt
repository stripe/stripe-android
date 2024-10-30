package com.stripe.android.stripe3ds2.security

import androidx.annotation.VisibleForTesting
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.DirectDecrypter
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseParseException
import com.stripe.android.stripe3ds2.transactions.ProtocolError
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.util.Arrays
import java.util.Locale
import javax.crypto.SecretKey

/**
 * A class that manages encrypting Challenges requests (CReq) and decrypting Challenge
 * responses (CRes).
 *
 * The class manages two counters, [counterSdkToAcs] and [counterAcsToSdk],
 * which are used to validate the CReq/CRes messages.
 *
 * See "SDK Technical Guide - 3.4 Encryption of CReq / Decryption of CRes" for reference
 * implementation.
 */
internal data class DefaultMessageTransformer @VisibleForTesting internal constructor(
    private val isLiveMode: Boolean,
    private var counterSdkToAcs: Byte,
    private var counterAcsToSdk: Byte
) : MessageTransformer {

    constructor(
        isLiveMode: Boolean
    ) : this(
        isLiveMode,
        0.toByte(),
        0.toByte()
    )

    /**
     * See "Protocol and Core Functions Specification - 6.2.4.1 3DS SDK - CReq"
     *
     * @param challengeRequest CReq to encrypt
     * @param secretKey Diffie-Hellman secret key; see [StripeDiffieHellmanKeyGenerator]
     * @return a byte array representing the encrypted CReq
     */
    @Throws(JOSEException::class, JSONException::class)
    override fun encrypt(challengeRequest: JSONObject, secretKey: SecretKey): String {
        val header = createEncryptionHeader(
            challengeRequest.getString(ChallengeRequestData.FIELD_ACS_TRANS_ID)
        )
        challengeRequest.put(
            FIELD_SDK_COUNTER_SDK_TO_ACS,
            String.format(Locale.ROOT, "%03d", counterSdkToAcs)
        )
        val jweObject = JWEObject(
            header,
            Payload(challengeRequest.toString())
        )

        jweObject.encrypt(
            TransactionEncrypter(
                getEncryptionKey(secretKey, header.encryptionMethod),
                counterSdkToAcs
            )
        )

        counterSdkToAcs++

        require(counterSdkToAcs.toInt() != 0) {
            "SDK to ACS counter is zero"
        }

        return jweObject.serialize()
    }

    /**
     * See "Protocol and Core Functions Specification - 6.2.4.2 3DS SDK - CRes"
     *
     * @param message CRes to decrypt
     * @param secretKey Diffie-Hellman secret key; see [StripeDiffieHellmanKeyGenerator]
     * @return a [JSONObject] representing the decrypted CRes
     */
    @Throws(ParseException::class, JOSEException::class, JSONException::class, ChallengeResponseParseException::class)
    override fun decrypt(message: String, secretKey: SecretKey): JSONObject {
        val challengeResponse = decryptMessage(message, secretKey)
        validateAcsToSdkCounter(challengeResponse)

        counterAcsToSdk++

        require(counterAcsToSdk.toInt() != 0) {
            "ACS to SDK counter is zero"
        }

        return challengeResponse
    }

    /**
     * ACS counter validation is bypassed in test mode.
     */
    @VisibleForTesting
    @Throws(ChallengeResponseParseException::class, JSONException::class)
    internal fun validateAcsToSdkCounter(cres: JSONObject) {
//        if (!isLiveMode) {
//            return
//        }

        if (!cres.has(FIELD_ACS_COUNTER_ACS_TO_SDK)) {
            throw ChallengeResponseParseException
                .createRequiredDataElementMissing(FIELD_ACS_COUNTER_ACS_TO_SDK)
        }

        val acsCounterAcsToSdk = runCatching {
            cres.getString(FIELD_ACS_COUNTER_ACS_TO_SDK).toByte()
        }.getOrElse {
            throw ChallengeResponseParseException
                .createInvalidDataElementFormat(FIELD_ACS_COUNTER_ACS_TO_SDK)
        }

        if (counterAcsToSdk != acsCounterAcsToSdk) {
            throw ChallengeResponseParseException(
                ProtocolError.DataDecryptionFailure,
                "Counters are not equal. SDK counter: $counterAcsToSdk, ACS counter: $acsCounterAcsToSdk"
            )
        }
    }

    @VisibleForTesting
    @Throws(ParseException::class, JOSEException::class, JSONException::class)
    internal fun decryptMessage(message: String, secretKey: SecretKey): JSONObject {
        val jweObject = JWEObject.parse(message)

        val key = getDecryptionKey(secretKey, jweObject.header.encryptionMethod)
        jweObject.decrypt(DirectDecrypter(key))

        val invalidPayload = !isValidPayloadPart(jweObject.header.toString()) ||
            !isValidPayloadPart(jweObject.iv.toString()) ||
            !isValidPayloadPart(jweObject.cipherText.toString()) ||
            !isValidPayloadPart(jweObject.authTag.toString())

        if (invalidPayload) {
            throw ChallengeResponseParseException(ProtocolError.DataDecryptionFailure, "Invalid encryption.")
        }

        return JSONObject(jweObject.payload.toString())
    }

    @VisibleForTesting
    internal fun createEncryptionHeader(keyId: String): JWEHeader {
        return JWEHeader.Builder(JWEAlgorithm.DIR, ENCRYPTION_METHOD)
            .keyID(keyId)
            .build()
    }

    private fun isValidPayloadPart(part: String): Boolean {
        return !(
            part.endsWith("=") ||
                part.contains(" ") ||
                part.contains("+") ||
                part.contains("\n") ||
                part.contains("/")
            )
    }

    /**
     * AES 128 bit GCM using utilizes the last 16 bytes of the secret key to decrypt messages while
     * CBC utilizes the full key
     *
     * @param secretKey the secret key to get the decryption key from
     * @param encryptionMethod the encryption method, GCM or CBC
     * @return the encoded decryption key for the provided encryption method
     */
    @VisibleForTesting
    internal fun getDecryptionKey(
        secretKey: SecretKey,
        encryptionMethod: EncryptionMethod
    ): ByteArray {
        val encodedKey = secretKey.encoded
        return if (EncryptionMethod.A128GCM === encryptionMethod) {
            Arrays.copyOfRange(
                encodedKey,
                encodedKey.size - EncryptionMethod.A128GCM.cekBitLength() / BITS_IN_BYTE,
                encodedKey.size
            )
        } else {
            encodedKey
        }
    }

    /**
     * AES 128 bit GCM utilizes the first 16 bytes of the secret key to encrypt messages while CBC
     * utilizes the full key
     *
     * @param secretKey the secret key to get the encryption key from
     * @param encryptionMethod the encryption method, GCM or CBC
     * @return the encoded encryption key for the provided encryption method
     */
    @VisibleForTesting
    internal fun getEncryptionKey(
        secretKey: SecretKey,
        encryptionMethod: EncryptionMethod
    ): ByteArray {
        val encodedKey = secretKey.encoded
        return if (EncryptionMethod.A128GCM === encryptionMethod) {
            Arrays.copyOfRange(
                encodedKey,
                0,
                EncryptionMethod.A128GCM.cekBitLength() / BITS_IN_BYTE
            )
        } else {
            encodedKey
        }
    }

    companion object {
        internal const val FIELD_ACS_COUNTER_ACS_TO_SDK = "acsCounterAtoS"
        internal const val FIELD_SDK_COUNTER_SDK_TO_ACS = "sdkCounterStoA"

        /**
         * [EncryptionMethod.A128CBC_HS256] is supported natively on Android without needing to use
         * BouncyCastle, so we prefer it to [EncryptionMethod.A128GCM].
         *
         * @see [Cryptography Changes in Android P](https://android-developers.googleblog.com/2018/03/cryptography-changes-in-android-p.html).
         */
        private val ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256

        internal const val BITS_IN_BYTE = 8
    }
}
