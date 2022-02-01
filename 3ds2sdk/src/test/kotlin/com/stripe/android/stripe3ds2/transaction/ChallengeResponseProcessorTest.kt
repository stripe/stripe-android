package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.DefaultMessageTransformer
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.transactions.ProtocolError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import javax.crypto.SecretKey
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class ChallengeResponseProcessorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val secretKey: SecretKey = mock()
    private val messageTransformer = FakeMessageTransformer()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun processPayload_withValidCresPayload_callsOnSuccess() = testDispatcher.runBlockingTest {
        val cresData = ChallengeMessageFixtures.CRES.copy(
            isChallengeCompleted = true,
            transStatus = TransactionStatus.VerificationSuccessful.code,
            messageVersion = ChallengeMessageFixtures.MESSAGE_VERSION_210
        )

        val result = createProcessor()
            .processPayload(CREQ_DATA, cresData.toJson())

        assertThat(result)
            .isEqualTo(
                ChallengeRequestResult.Success(
                    CREQ_DATA,
                    cresData,
                    ChallengeRequestExecutorFixtures.CONFIG
                )
            )
    }

    @Test
    fun processPayload_withValidErrorPayload_shouldCallOnError() = testDispatcher.runBlockingTest {
        val errorData = ERROR_DATA.copy(
            errorCode = ProtocolError.InvalidMessageReceived.code.toString(),
            errorDescription = ProtocolError.InvalidMessageReceived.description,
            errorDetail = "Message was invalid",
            dsTransId = ""
        )

        val result = createProcessor()
            .processPayload(CREQ_DATA, errorData.toJson())

        assertThat(result)
            .isEqualTo(
                ChallengeRequestResult.ProtocolError(errorData)
            )
    }

    @Test
    fun processPayload_withEmptyPayload_shouldCallOnError() = testDispatcher.runBlockingTest {
        val result = createProcessor()
            .processPayload(CREQ_DATA, JSONObject("{}"))

        assertThat(result)
            .isEqualTo(
                ChallengeRequestResult.ProtocolError(
                    ERROR_DATA.copy(
                        errorCode = ProtocolError.InvalidMessageReceived.code.toString(),
                        errorDescription = "Message is not CRes",
                        errorDetail = "Invalid Message Type"
                    )
                )
            )
    }

    @Test
    fun processPayload_withCresSdkTransIdNotMatchingCreq_shouldCallOnError() = testDispatcher.runBlockingTest {
        val cresData = ChallengeMessageFixtures.CRES.copy(
            sdkTransId = SdkTransactionId.create(),
            isChallengeCompleted = true,
            transStatus = TransactionStatus.VerificationSuccessful.code
        )

        val result = createProcessor()
            .processPayload(CREQ_DATA, cresData.toJson())

        assertThat(result)
            .isEqualTo(
                ChallengeRequestResult.ProtocolError(
                    ERROR_DATA.copy(
                        errorCode = ProtocolError.InvalidTransactionId.code.toString(),
                        errorDescription = "Transaction ID received is not valid for the receiving component.",
                        errorDetail = "The Transaction ID received was invalid."
                    )
                )
            )
    }

    @Test
    fun processPayload_withCresMessageVersionNotMatchingCreq_shouldCallOnError() = testDispatcher.runBlockingTest {
        val cresData = ChallengeMessageFixtures.CRES.copy(
            isChallengeCompleted = true,
            messageVersion = "4.0.0",
            transStatus = TransactionStatus.VerificationSuccessful.code
        )

        val result = createProcessor()
            .processPayload(CREQ_DATA, cresData.toJson())

        assertThat(result)
            .isEqualTo(
                ChallengeRequestResult.ProtocolError(
                    ERROR_DATA.copy(
                        errorCode = ProtocolError.UnsupportedMessageVersion.code.toString(),
                        errorDescription = "Message Version Number received is not valid for the receiving component.",
                        errorDetail = "2.1.0"
                    )
                )
            )
    }

    @Test
    fun handleResponse_withInvalidResponseBody_shouldCallOnError() = testDispatcher.runBlockingTest {
        val result = createProcessor(
            DefaultMessageTransformer(isLiveMode = true)
        )
            .process(CREQ_DATA, HttpResponse("abc", ""))

        assertThat(result)
            .isEqualTo(
                ChallengeRequestResult.ProtocolError(
                    ERROR_DATA.copy(
                        errorCode = ProtocolError.DataDecryptionFailure.code.toString(),
                        errorDescription = "Data could not be decrypted by the receiving system due to technical or other reason.",
                        errorDetail = "Invalid serialized unsecured/JWS/JWE object: Missing part delimiters"
                    )
                )
            )
    }

    @Test
    fun handleResponse_withErrorMessage_shouldCallOnError() = testDispatcher.runBlockingTest {
        val errorMessage = ErrorData(
            sdkTransId = ChallengeMessageFixtures.SDK_TRANS_ID,
            acsTransId = ChallengeMessageFixtures.ACS_TRANS_ID,
            serverTransId = ChallengeMessageFixtures.SERVER_TRANS_ID,
            messageVersion = ChallengeMessageFixtures.MESSAGE_VERSION_210,
            errorCode = ProtocolError.InvalidMessageReceived.code.toString(),
            errorDescription = ProtocolError.InvalidMessageReceived.description,
            errorDetail = "Message was invalid"
        )

        val processor = createProcessor()
        val result = processor.process(
            CREQ_DATA,
            HttpResponse(
                errorMessage.toJson().toString(),
                "application/json"
            )
        )

        assertThat(messageTransformer.decryptions)
            .isEqualTo(0)

        assertThat(result)
            .isEqualTo(
                ChallengeRequestResult.ProtocolError(
                    ERROR_DATA.copy(
                        errorCode = ProtocolError.InvalidMessageReceived.code.toString(),
                        errorComponent = null,
                        errorDescription = "Message is not AReq, ARes, CReq, CRes, PReq, PRes, RReq, or RRes",
                        errorDetail = "Message was invalid",
                        errorMessageType = "",
                        dsTransId = ""
                    )
                )
            )
    }

    private fun createProcessor(
        messageTransformer: MessageTransformer = this.messageTransformer
    ): ChallengeResponseProcessor.Default {
        return ChallengeResponseProcessor.Default(
            messageTransformer,
            secretKey,
            FakeErrorReporter(),
            ChallengeRequestExecutorFixtures.CONFIG
        )
    }

    private class FakeMessageTransformer : MessageTransformer {
        var decryptions = 0
        var encryptions = 0

        override fun encrypt(challengeRequest: JSONObject, secretKey: SecretKey): String {
            encryptions++
            return ""
        }

        override fun decrypt(message: String, secretKey: SecretKey): JSONObject {
            decryptions++
            return JSONObject()
        }
    }

    private companion object {
        private val CREQ_DATA = ChallengeMessageFixtures.CREQ

        private val ERROR_DATA = ErrorData(
            errorCode = ProtocolError.InvalidMessageReceived.code.toString(),
            errorComponent = ErrorData.ErrorComponent.ThreeDsSdk,
            errorDescription = "Message is not CRes",
            errorDetail = "Invalid Message Type",
            errorMessageType = "CRes",
            messageVersion = CREQ_DATA.messageVersion,
            acsTransId = CREQ_DATA.acsTransId,
            sdkTransId = CREQ_DATA.sdkTransId,
            serverTransId = CREQ_DATA.threeDsServerTransId
        )
    }
}
