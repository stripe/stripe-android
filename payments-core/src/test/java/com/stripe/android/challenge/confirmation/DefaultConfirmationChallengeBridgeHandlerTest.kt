package com.stripe.android.challenge.confirmation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
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
        val handler = createHandler()
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
        val handler = createHandler()
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
        val handler = createHandler()
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
    fun `onSuccess with invalid JSON emits Error event`() = runTest {
        val handler = createHandler()
        val invalidJson = """not valid json"""

        handler.event.test {
            handler.onSuccess(invalidJson)

            val event = awaitItem()
            assertThat(event).isInstanceOf(ConfirmationChallengeBridgeEvent.Error::class.java)
            val errorEvent = event as ConfirmationChallengeBridgeEvent.Error
            assertThat(errorEvent.cause).isNotNull()
        }
    }

    @Test
    fun `onSuccess with missing client secret emits Error event`() = runTest {
        val argsWithNoClientSecret = testArgs.copy(
            intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(clientSecret = null)
        )
        val handler = createHandler(args = argsWithNoClientSecret)
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
    fun `logConsole with valid JSON logs message`() {
        val logger = FakeLogger()
        val handler = createHandler(logger = logger)
        val logData = """{"level": "error", "message": "Test error message"}"""

        handler.logConsole(logData)

        assertThat(logger.debugLogs).hasSize(1)
        assertThat(logger.debugLogs.first()).isEqualTo("❌ [ConfirmationChallenge] Console ERROR: Test error message")
    }

    @Test
    fun `logConsole with warning level logs message`() {
        val logger = FakeLogger()
        val handler = createHandler(logger = logger)
        val logData = """{"level": "warn", "message": "Test warning"}"""

        handler.logConsole(logData)

        assertThat(logger.debugLogs).hasSize(1)
        assertThat(logger.debugLogs.first()).isEqualTo("⚠️ [ConfirmationChallenge] Console WARN: Test warning")
    }

    @Test
    fun `logConsole with info level logs message`() {
        val logger = FakeLogger()
        val handler = createHandler(logger = logger)
        val logData = """{"level": "info", "message": "Test info"}"""

        handler.logConsole(logData)

        assertThat(logger.debugLogs).hasSize(1)
        assertThat(logger.debugLogs.first()).isEqualTo("📝 [ConfirmationChallenge] Console INFO: Test info")
    }

    @Test
    fun `logConsole with invalid JSON falls back to raw message`() {
        val logger = FakeLogger()
        val handler = createHandler(logger = logger)
        val invalidLogData = "not valid json"

        handler.logConsole(invalidLogData)

        assertThat(logger.debugLogs).hasSize(1)
        assertThat(logger.debugLogs.first()).isEqualTo("📝 [ConfirmationChallenge] Console: not valid json")
    }

    private fun createHandler(
        args: IntentConfirmationChallengeArgs = testArgs,
        logger: FakeLogger = FakeLogger()
    ): DefaultConfirmationChallengeBridgeHandler {
        return DefaultConfirmationChallengeBridgeHandler(
            args = args,
            logger = logger
        )
    }
}
