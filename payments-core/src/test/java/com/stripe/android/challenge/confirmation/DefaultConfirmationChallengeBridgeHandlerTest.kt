package com.stripe.android.challenge.confirmation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultConfirmationChallengeBridgeHandlerTest {

    private val testArgs = IntentConfirmationChallengeArgs(
        publishableKey = "pk_test_123",
        intent = PaymentIntentFixtures.PI_SUCCEEDED
    )

    @Test
    fun `getInitParams returns correct JSON with publishableKey and clientSecret`() {
        val handler = createHandler()

        val result = handler.getInitParams()

        val json = JSONObject(result)
        assertThat(json.getString("publishableKey")).isEqualTo("pk_test_123")
        assertThat(json.getString("clientSecret")).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED.clientSecret)
    }

    @Test
    fun `onReady emits Ready event`() = runTest {
        val handler = createHandler()

        handler.event.test {
            handler.onReady()

            val event = awaitItem()
            assertThat(event).isEqualTo(ConfirmationChallengeBridgeEvent.Ready)
        }
    }

    @Test
    fun `onSuccess with valid JSON emits Success event with client secret from JSON`() = runTest {
        val successParser = FakeBridgeSuccessParamsJsonParser().apply {
            willReturn(BridgeSuccessParams(clientSecret = "pi_custom_secret"))
        }
        val handler = createHandler(successParamsParser = successParser)
        val paymentIntentJson = """{"client_secret": "pi_custom_secret"}"""

        handler.event.test {
            handler.onSuccess(paymentIntentJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Success::class.java)
            val successEvent = event as ConfirmationChallengeBridgeEvent.Success
            assertThat(successEvent.clientSecret).isEqualTo("pi_custom_secret")
        }
    }

    @Test
    fun `onSuccess with empty client_secret falls back to intent client secret`() = runTest {
        val successParser = FakeBridgeSuccessParamsJsonParser().apply {
            willReturn(null)
        }
        val handler = createHandler(successParamsParser = successParser)
        val paymentIntentJson = """{"client_secret": ""}"""

        handler.event.test {
            handler.onSuccess(paymentIntentJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Success::class.java)
            val successEvent = event as ConfirmationChallengeBridgeEvent.Success
            assertThat(successEvent.clientSecret).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED.clientSecret)
        }
    }

    @Test
    fun `onSuccess with no client_secret falls back to intent client secret`() = runTest {
        val successParser = FakeBridgeSuccessParamsJsonParser().apply {
            willReturn(null)
        }
        val handler = createHandler(successParamsParser = successParser)
        val paymentIntentJson = """{"id": "pi_123"}"""

        handler.event.test {
            handler.onSuccess(paymentIntentJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Success::class.java)
            val successEvent = event as ConfirmationChallengeBridgeEvent.Success
            assertThat(successEvent.clientSecret).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED.clientSecret)
        }
    }

    @Test
    fun `onSuccess with invalid JSON emits Success event`() = runTest {
        val parser = FakeBridgeSuccessParamsJsonParser()
            .apply {
                willThrow(Throwable("oops"))
            }
        val handler = createHandler(
            successParamsParser = parser
        )

        handler.event.test {
            handler.onSuccess("")

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Success::class.java)
            val successEvent = event as ConfirmationChallengeBridgeEvent.Success
            assertThat(successEvent.clientSecret).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED.clientSecret)
        }
    }

    @Test
    fun `onSuccess with missing client secret emits Error event`() = runTest {
        val argsWithNoClientSecret = testArgs.copy(
            intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(clientSecret = null)
        )
        val successParser = FakeBridgeSuccessParamsJsonParser().apply {
            willReturn(null)
        }
        val handler = createHandler(args = argsWithNoClientSecret, successParamsParser = successParser)
        val paymentIntentJson = """{"id": "pi_123"}"""

        handler.event.test {
            handler.onSuccess(paymentIntentJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Error::class.java)
            val errorEvent = event as ConfirmationChallengeBridgeEvent.Error
            assertThat(errorEvent.cause).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(errorEvent.cause.message).isEqualTo("Missing client secret")
        }
    }

    @Test
    fun `onError emits Error event with message`() = runTest {
        val handler = createHandler()
        val errorMessage = "Something went wrong"

        handler.event.test {
            handler.onError(errorMessage)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Error::class.java)
            val errorEvent = event as ConfirmationChallengeBridgeEvent.Error
            assertThat(errorEvent.cause).isInstanceOf(Exception::class.java)
            assertThat(errorEvent.cause.message).isEqualTo(errorMessage)
        }
    }

    @Test
    fun `onError with JSON error object parses and emits error message`() = runTest {
        val errorParser = FakeBridgeErrorParamsJsonParser().apply {
            willReturn(
                BridgeErrorParams(
                    message = "Payment declined",
                    type = "card_error",
                    code = "card_declined"
                )
            )
        }
        val handler = createHandler(errorParamsParser = errorParser)
        val errorJson = """
            {
                "message": "Payment declined",
                "type": "card_error",
                "code": "card_declined"
            }
        """.trimIndent()

        handler.event.test {
            handler.onError(errorJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Error::class.java)
            val errorEvent = event as ConfirmationChallengeBridgeEvent.Error
            assertThat(errorEvent.cause).isInstanceOf(BridgeError::class.java)
            assertThat(errorEvent.cause.message).isEqualTo("Payment declined")
        }
    }

    @Test
    fun `onError with JSON error object but no message field uses raw error message`() = runTest {
        val errorParser = FakeBridgeErrorParamsJsonParser().apply {
            willReturn(
                BridgeErrorParams(
                    message = null,
                    type = "card_error",
                    code = "card_declined"
                )
            )
        }
        val handler = createHandler(errorParamsParser = errorParser)
        val errorJson = """
            {
                "type": "card_error",
                "code": "card_declined"
            }
        """.trimIndent()

        handler.event.test {
            handler.onError(errorJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Error::class.java)
            val errorEvent = event as ConfirmationChallengeBridgeEvent.Error
            assertThat(errorEvent.cause).isInstanceOf(BridgeError::class.java)
            assertThat(errorEvent.cause.message).isEqualTo(errorJson)
        }
    }

    @Test
    fun `onSuccess when parser throws exception falls back to intent client secret`() = runTest {
        val successParser = FakeBridgeSuccessParamsJsonParser().apply {
            willThrow(RuntimeException("Parser failed"))
        }
        val errorReporter = FakeErrorReporter()
        val handler = createHandler(successParamsParser = successParser, errorReporter = errorReporter)
        val paymentIntentJson = """{"client_secret": "pi_test_secret"}"""

        handler.event.test {
            handler.onSuccess(paymentIntentJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Success::class.java)
            val successEvent = event as ConfirmationChallengeBridgeEvent.Success
            assertThat(successEvent.clientSecret).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED.clientSecret)
        }

        assertThat(errorReporter.getLoggedErrors()).hasSize(1)
        assertThat(errorReporter.getLoggedErrors().first())
            .isEqualTo(
                ErrorReporter.UnexpectedErrorEvent
                    .INTENT_CONFIRMATION_CHALLENGE_FAILED_TO_PARSE_SUCCESS_CALLBACK_PARAMS.eventName
            )
    }

    @Test
    fun `onSuccess when parser throws exception and no fallback client secret emits Error event`() = runTest {
        val argsWithNoClientSecret = testArgs.copy(
            intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(clientSecret = null)
        )
        val successParser = FakeBridgeSuccessParamsJsonParser().apply {
            willThrow(RuntimeException("Parser failed"))
        }
        val errorReporter = FakeErrorReporter()
        val handler = createHandler(
            args = argsWithNoClientSecret,
            successParamsParser = successParser,
            errorReporter = errorReporter
        )
        val paymentIntentJson = """{"client_secret": "pi_test_secret"}"""

        handler.event.test {
            handler.onSuccess(paymentIntentJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Error::class.java)
            val errorEvent = event as ConfirmationChallengeBridgeEvent.Error
            assertThat(errorEvent.cause).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(errorEvent.cause.message).isEqualTo("Missing client secret")
        }

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.UnexpectedErrorEvent
                .INTENT_CONFIRMATION_CHALLENGE_FAILED_TO_PARSE_SUCCESS_CALLBACK_PARAMS.eventName
        )
    }

    @Test
    fun `onError when parser throws exception falls back to raw message`() = runTest {

        val errorReporter = FakeErrorReporter()
        val errorParser = FakeBridgeErrorParamsJsonParser().apply {
            willThrow(RuntimeException("Parser failed"))
        }
        val handler = createHandler(errorParamsParser = errorParser, errorReporter = errorReporter)
        val errorJson = """{"message": "Payment failed"}"""

        handler.event.test {
            handler.onError(errorJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Error::class.java)
            val errorEvent = event as ConfirmationChallengeBridgeEvent.Error
            assertThat(errorEvent.cause).isInstanceOf(Exception::class.java)
            assertThat(errorEvent.cause.message).isEqualTo(errorJson)
        }
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.UnexpectedErrorEvent
                .INTENT_CONFIRMATION_CHALLENGE_FAILED_TO_PARSE_ERROR_CALLBACK_PARAMS.eventName
        )
    }

    private fun createHandler(
        args: IntentConfirmationChallengeArgs = testArgs,
        successParamsParser: FakeBridgeSuccessParamsJsonParser = FakeBridgeSuccessParamsJsonParser(),
        errorParamsParser: FakeBridgeErrorParamsJsonParser = FakeBridgeErrorParamsJsonParser(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): DefaultConfirmationChallengeBridgeHandler {
        return DefaultConfirmationChallengeBridgeHandler(
            successParamsParser = successParamsParser,
            errorParamsParser = errorParamsParser,
            args = args,
            logger = FakeLogger(),
            errorReporter = errorReporter,
        )
    }
}
