package com.stripe.android.stripe3ds2.security

import com.google.common.truth.Truth.assertThat
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEException
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseParseException
import org.json.JSONException
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.text.ParseException
import java.util.Arrays
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class DefaultMessageTransformerTest {
    private val errorReporter = FakeErrorReporter()
    private val ephemeralKeyPairGenerator = StripeEphemeralKeyPairGenerator(errorReporter)
    private val diffieHellmanKeyGenerator = StripeDiffieHellmanKeyGenerator(errorReporter)

    @Test
    fun encrypt_shouldReturnCorrectValue() {
        val encryptedMessage = createMessageTransformer()
            .encrypt(createChallengeRequest(), createSecretKey())

        assertThat(encryptedMessage)
            .hasLength(262)
    }

    @Test
    fun encrypt_whenCounterSdkToAcsWillBeZero_shouldThrowException() {
        val message = createChallengeRequest()

        // this counter will be incremented to 0 on the next encryption attempt
        val counterSdktoAcs: Byte = -1

        assertFailsWith<RuntimeException> {
            DefaultMessageTransformer(
                true,
                counterSdktoAcs,
                0.toByte()
            ).encrypt(
                message,
                createSecretKey()
            )
        }
    }

    @Test
    fun roundTripEncryptionDecryption_shouldReturnExpectedJsonObject() {
        val acsTransactionId = UUID.randomUUID().toString()
        val messageTransformer = createMessageTransformer()

        // artificially add ACS to SDK counter to the request message - in practice this would only
        // be in the response, but the field needs to be populated in order to validate the response
        val requestMessage = createChallengeRequest(acsTransactionId)
            .put(
                DefaultMessageTransformer.FIELD_ACS_COUNTER_ACS_TO_SDK,
                String.format(Locale.ROOT, "%03d", 0)
            )

        val encryptionSecretKey = createSecretKey()
        val decryptionSecretKey =
            DecryptionSecretKey(EncryptionMethod.A128CBC_HS256, encryptionSecretKey)

        val encryptedMessage = messageTransformer
            .encrypt(requestMessage, encryptionSecretKey)

        val responseMessage = messageTransformer
            .decrypt(encryptedMessage, decryptionSecretKey)

        assertThat(
            responseMessage.getString(ChallengeRequestData.FIELD_ACS_TRANS_ID)
        ).isEqualTo(acsTransactionId)

        assertThat(
            responseMessage.getString(DefaultMessageTransformer.FIELD_SDK_COUNTER_SDK_TO_ACS)
        ).isEqualTo("000")

        assertThat(
            responseMessage.getString(DefaultMessageTransformer.FIELD_ACS_COUNTER_ACS_TO_SDK)
        ).isEqualTo("000")
    }

    @Test
    fun createEncryptionHeader() {
        val header = createMessageTransformer()
            .createEncryptionHeader(UUID.randomUUID().toString())
        assertThat(header)
            .isNotNull()
    }

    @Test
    fun getDecryptionKey_correctKeyIsReturnedForMethod() {
        val secretKey = createSecretKey()
        val messageTransformer = createMessageTransformer()

        val cbcKey = messageTransformer.getDecryptionKey(
            secretKey,
            EncryptionMethod.A128CBC_HS256
        )
        assertThat(cbcKey)
            .isEqualTo(secretKey.encoded)

        val gcmKey = messageTransformer.getDecryptionKey(
            secretKey,
            EncryptionMethod.A128GCM
        )
        val expected = Arrays.copyOfRange(secretKey.encoded, 16, 32)

        assertThat(gcmKey)
            .isEqualTo(expected)
    }

    @Test
    fun getEncryptionKey_correctKeyIsReturnedForMethod() {
        val secretKey = createSecretKey()
        val messageTransformer = createMessageTransformer()

        val cbcKey = messageTransformer.getEncryptionKey(
            secretKey,
            EncryptionMethod.A128CBC_HS256
        )
        assertThat(cbcKey)
            .isEqualTo(secretKey.encoded)

        val gcmKey = messageTransformer.getEncryptionKey(
            secretKey,
            EncryptionMethod.A128GCM
        )
        val gcmKeyExpected = Arrays.copyOfRange(secretKey.encoded, 0, 16)
        assertThat(gcmKey)
            .isEqualTo(gcmKeyExpected)
    }

    @Test
    @Throws(ParseException::class, JOSEException::class, JSONException::class)
    fun testAcsToSdkRoundtrip() {
        val sdkKeyPair = ephemeralKeyPairGenerator.generate()

        val acsKeyPair = ephemeralKeyPairGenerator.generate()

        val acsSecretKey = diffieHellmanKeyGenerator
            .generate(
                sdkKeyPair.public as ECPublicKey,
                acsKeyPair.private as ECPrivateKey,
                SDK_REFERENCE_NUMBER
            )

        val sdkSecretKey = DecryptionSecretKey(EncryptionMethod.A128CBC_HS256, acsSecretKey)
        val creq = ChallengeMessageFixtures.CREQ.toJson()

        val messageTransformer = createMessageTransformer()
        val encryptedCres = messageTransformer.encrypt(creq, acsSecretKey)
        assertThat(encryptedCres)
            .isNotNull()

        val roundtripPayload = messageTransformer
            .decryptMessage(encryptedCres, sdkSecretKey)
        assertThat(roundtripPayload)
            .isNotNull()
    }

    @Test
    fun validateAcsToSdkCounter_withEmptyJson_shouldThrowException() {
        val exception = assertFailsWith<ChallengeResponseParseException> {
            createMessageTransformer().validateAcsToSdkCounter(JSONObject())
        }
        assertThat(exception.message)
            .isEqualTo(
                "201 - A message element required as defined in Table A.1 is missing from the message. (acsCounterAtoS)"
            )
    }

    @Test
    fun validateAcsToSdkCounter_withEmptyValue_shouldThrowException() {
        val exception = assertFailsWith<ChallengeResponseParseException> {
            createMessageTransformer()
                .validateAcsToSdkCounter(
                    JSONObject()
                        .put(DefaultMessageTransformer.FIELD_ACS_COUNTER_ACS_TO_SDK, "")
                )
        }
        assertThat(exception.message)
            .isEqualTo(
                "203 - Data element not in the required format or value is invalid as defined in Table A.1 (acsCounterAtoS)",
            )
    }

    @Test
    fun validateAcsToSdkCounter_withNonMatchingCounter_shouldThrowException() {
        val exception = assertFailsWith<ChallengeResponseParseException> {
            createMessageTransformer().validateAcsToSdkCounter(
                JSONObject()
                    .put(DefaultMessageTransformer.FIELD_ACS_COUNTER_ACS_TO_SDK, "100")
            )
        }
        assertThat(exception.message)
            .isEqualTo(
                "302 - Data could not be decrypted by the receiving " +
                    "system due to technical or other reason. (Counters are not equal. " +
                    "SDK counter: 0, ACS counter: 100)",

            )
    }

    @Test
    fun validateAcsToSdkCounter_withMatchingValue_shouldPass() {
        createMessageTransformer().validateAcsToSdkCounter(
            JSONObject()
                .put(DefaultMessageTransformer.FIELD_ACS_COUNTER_ACS_TO_SDK, "000")
        )
    }

    @Test
    fun validateAcsToSdkCounter_withNonMatchingCounter_inTestMode_shouldNotThrow() {
        createMessageTransformer(isLiveMode = false)
            .validateAcsToSdkCounter(
                JSONObject()
                    .put(DefaultMessageTransformer.FIELD_ACS_COUNTER_ACS_TO_SDK, "100")
            )
    }

    private fun createSecretKey(): SecretKey {
        val keyPair = ephemeralKeyPairGenerator.generate()
        return diffieHellmanKeyGenerator
            .generate(
                keyPair.public as ECPublicKey,
                keyPair.private as ECPrivateKey,
                SDK_REFERENCE_NUMBER
            )
    }

    private fun createChallengeRequest(
        acsTransactionId: String = UUID.randomUUID().toString()
    ): JSONObject {
        return JSONObject()
            .put(ChallengeRequestData.FIELD_ACS_TRANS_ID, acsTransactionId)
    }

    private fun createMessageTransformer(isLiveMode: Boolean = true): DefaultMessageTransformer {
        return DefaultMessageTransformer(isLiveMode)
    }

    /**
     * A wrapper around [SecretKey] to provide the same key used to encrypt the test
     * messages for testing decryption.
     */
    private class DecryptionSecretKey(
        val encryptionMethod: EncryptionMethod,
        private val encryptionSecretKey: SecretKey
    ) : SecretKey {

        override fun getAlgorithm(): String {
            return encryptionSecretKey.algorithm
        }

        override fun getFormat(): String {
            return encryptionSecretKey.format
        }

        /**
         * In [DefaultMessageTransformer.encrypt], the TransactionEncrypter
         * uses the first 16 bytes of the secret key to encrypt the message. The server also uses
         * the first 16 bytes to decrypt the message sent. However, the server encrypts it's
         * messages with the last 16 bytes of the secret key thus the
         * [DefaultMessageTransformer.getDecryptionKey]
         * method uses the last 16 bytes as the key to decrypt the message. However, in the above
         * test cases, the decryption key needs to be the same as the encryption key as we are
         * decrypting messages the SDK encrypted.
         *
         * @return This wrapper returns only the first 16 bytes of the secret key to ensure the
         * same key used to encrypt the test messages is the same used to decrypt them.
         */
        override fun getEncoded(): ByteArray {
            return Arrays.copyOfRange(
                encryptionSecretKey.encoded,
                0,
                encryptionMethod.cekBitLength() / DefaultMessageTransformer.BITS_IN_BYTE
            )
        }
    }

    private companion object {
        private const val SDK_REFERENCE_NUMBER = "ABC123"
    }
}
