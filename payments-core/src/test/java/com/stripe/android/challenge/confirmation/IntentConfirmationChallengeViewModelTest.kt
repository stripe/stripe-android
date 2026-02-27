package com.stripe.android.challenge.confirmation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.confirmation.analytics.IntentConfirmationChallengeAnalyticsEventReporter
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CancelCaptchaChallengeParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `when Ready event is received, bridgeReady emits value`() = testScenario {
        viewModel.bridgeReady.test {
            // Initial state
            expectNoEvents()

            // Emit Ready event
            bridgeHandler.emitEvent(ConfirmationChallengeBridgeEvent.Ready)

            // Should emit value
            awaitItem()

            ensureAllEventsConsumed()
        }

        // Verify analytics
        assertThat(analyticsReporter.calls).hasSize(1)
        assertThat(analyticsReporter.calls.last()).isEqualTo(
            FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.WebViewLoaded
        )
    }

    @Test
    fun `when Success event is received, result emits Success with clientSecret`() = testScenario {
        val expectedClientSecret = "pi_test_secret_123"

        viewModel.result.test {
            bridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Success(clientSecret = expectedClientSecret)
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Success::class.java)
            val successResult = result as IntentConfirmationChallengeActivityResult.Success
            assertThat(successResult.clientSecret).isEqualTo(expectedClientSecret)

            ensureAllEventsConsumed()
        }

        // Verify analytics
        assertThat(analyticsReporter.calls).hasSize(1)
        assertThat(analyticsReporter.calls.last()).isEqualTo(
            FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Success
        )
    }

    @Test
    fun `when Error event is received, result emits Failed with error`() = testScenario {
        val expectedError = BridgeException(IOException("Network error"))

        viewModel.result.test {
            bridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Error(error = expectedError)
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
            val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
            assertThat(failedResult.error).isEqualTo(expectedError)

            ensureAllEventsConsumed()
        }

        // Verify analytics
        assertThat(analyticsReporter.calls).hasSize(1)
        val errorCall =
            analyticsReporter.calls.last() as FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Error
        assertThat(errorCall.errorType).isNull()
        assertThat(errorCall.errorCode).isNull()
        assertThat(errorCall.fromBridge).isTrue()
    }

    @Test
    fun `when handleWebViewError is called, result emits Failed with WebViewError`() = testScenario {
        val webViewError = WebViewError(
            message = "net::ERR_FAILED",
            url = "https://example.com/payment",
            errorCode = -2,
            webViewErrorType = "generic_resource_error"
        )

        viewModel.result.test {
            viewModel.handleWebViewError(webViewError)

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
            val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
            assertThat(failedResult.error).isEqualTo(webViewError)
            assertThat(failedResult.error).isInstanceOf(WebViewError::class.java)

            ensureAllEventsConsumed()
        }

        // Verify analytics
        assertThat(analyticsReporter.calls).hasSize(1)
        val errorCall =
            analyticsReporter.calls.last() as FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Error
        assertThat(errorCall.errorType).isEqualTo("generic_resource_error")
        assertThat(errorCall.errorCode).isEqualTo("-2")
        assertThat(errorCall.fromBridge).isFalse()
    }

    @Test
    fun `when closeClicked is called and verify succeeds, result emits Canceled`() {
        val fakeStripeRepository = FakeStripeRepository(
            cancelResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        )
        testScenario(stripeRepository = fakeStripeRepository) {
            viewModel.result.test {
                viewModel.closeClicked()

                val result = awaitItem()
                assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Canceled::class.java)
                val canceledResult = result as IntentConfirmationChallengeActivityResult.Canceled
                assertThat(canceledResult.clientSecret).isEqualTo(TEST_ARGS.intent.clientSecret)

                ensureAllEventsConsumed()
            }

            val repoCall = fakeStripeRepository.awaitCall()
            assertThat(repoCall.intentId).isEqualTo(TEST_ARGS.intent.id)
            assertThat(repoCall.params.clientSecret).isEqualTo(TEST_ARGS.intent.clientSecret)
            assertThat(repoCall.requestOptions).isEqualTo(REQUEST_OPTIONS)
            fakeStripeRepository.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `when closeClicked is called and intent id is null, errorReporter reports and result emits Failed`() {
        val fakeErrorReporter = FakeErrorReporter()
        val argsWithNullId = IntentConfirmationChallengeArgs(
            publishableKey = "pk_test_123",
            intent = PaymentIntentFixtures.PI_WITH_NULL_ID,
            productUsage = listOf("PaymentSheet"),
        )
        testScenario(args = argsWithNullId, errorReporter = fakeErrorReporter) {
            viewModel.result.test {
                viewModel.closeClicked()

                val result = awaitItem()
                assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
                val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
                assertThat(failedResult.error).isInstanceOf(IllegalArgumentException::class.java)
                assertThat(failedResult.error).hasMessageThat().isEqualTo("Intent parameters are unavailable")

                ensureAllEventsConsumed()
            }

            val errorCall = fakeErrorReporter.awaitCall()
            assertThat(errorCall.errorEvent).isEqualTo(
                ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_CHALLENGE_INTENT_PARAMETERS_UNAVAILABLE
            )
            fakeErrorReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `when closeClicked is called and verify fails, result emits Failed`() {
        val expectedError = IOException("Network error")
        val fakeStripeRepository = FakeStripeRepository(
            cancelResult = Result.failure(expectedError)
        )
        testScenario(stripeRepository = fakeStripeRepository) {
            viewModel.result.test {
                viewModel.closeClicked()

                val result = awaitItem()
                assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
                val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
                assertThat(failedResult.clientSecret).isEqualTo(TEST_ARGS.intent.clientSecret)
                assertThat(failedResult.error).isEqualTo(expectedError)

                ensureAllEventsConsumed()
            }

            fakeStripeRepository.awaitCall()
            fakeStripeRepository.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `when onStart is called, analytics start is reported`() = testScenario {
        val lifecycleOwner = object : LifecycleOwner {
            private val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle = registry

            fun start() {
                registry.addObserver(viewModel)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
        }

        lifecycleOwner.start()

        assertThat(analyticsReporter.calls).hasSize(1)
        assertThat(analyticsReporter.calls.first()).isEqualTo(
            FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Start
        )
    }

    private class Scenario(
        val viewModel: IntentConfirmationChallengeViewModel,
        val bridgeHandler: FakeConfirmationChallengeBridgeHandler,
        val analyticsReporter: FakeIntentConfirmationChallengeAnalyticsEventReporter,
    )

    private fun testScenario(
        stripeRepository: StripeRepository = FakeStripeRepository(
            cancelResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        ),
        args: IntentConfirmationChallengeArgs = TEST_ARGS,
        errorReporter: FakeErrorReporter = FakeErrorReporter(),
        test: suspend Scenario.() -> Unit
    ) = runTest {
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val analyticsReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter()
        val viewModel = createViewModel(
            bridgeHandler = bridgeHandler,
            analyticsReporter = analyticsReporter,
            stripeRepository = stripeRepository,
            args = args,
            errorReporter = errorReporter,
        )
        Scenario(viewModel, bridgeHandler, analyticsReporter).test()
    }

    private fun createViewModel(
        bridgeHandler: ConfirmationChallengeBridgeHandler,
        analyticsReporter: IntentConfirmationChallengeAnalyticsEventReporter =
            FakeIntentConfirmationChallengeAnalyticsEventReporter(),
        stripeRepository: StripeRepository = FakeStripeRepository(
            cancelResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        ),
        args: IntentConfirmationChallengeArgs = TEST_ARGS,
        errorReporter: FakeErrorReporter = FakeErrorReporter()
    ) = IntentConfirmationChallengeViewModel(
        args = args,
        bridgeHandler = bridgeHandler,
        workContext = testDispatcher,
        analyticsEventReporter = analyticsReporter,
        userAgent = "fake-user-agent",
        stripeRepository = stripeRepository,
        errorReporter = errorReporter,
        requestOptions = REQUEST_OPTIONS,
    )

    private class FakeStripeRepository(
        private val cancelResult: Result<StripeIntent>,
    ) : AbsFakeStripeRepository() {
        private val calls = Turbine<Call>()

        override suspend fun cancelPaymentIntentCaptchaChallenge(
            paymentIntentId: String,
            params: CancelCaptchaChallengeParams,
            requestOptions: ApiRequest.Options
        ): Result<PaymentIntent> {
            calls.add(
                item = Call(
                    intentId = paymentIntentId,
                    params = params,
                    requestOptions = requestOptions
                )
            )
            @Suppress("UNCHECKED_CAST")
            return cancelResult as Result<PaymentIntent>
        }

        override suspend fun cancelSetupIntentCaptchaChallenge(
            setupIntentId: String,
            params: CancelCaptchaChallengeParams,
            requestOptions: ApiRequest.Options
        ): Result<SetupIntent> {
            calls.add(
                item = Call(
                    intentId = setupIntentId,
                    params = params,
                    requestOptions = requestOptions
                )
            )
            @Suppress("UNCHECKED_CAST")
            return cancelResult as Result<SetupIntent>
        }

        suspend fun awaitCall(): Call {
            return calls.awaitItem()
        }

        fun ensureAllEventsConsumed() {
            calls.ensureAllEventsConsumed()
        }

        data class Call(
            val intentId: String,
            val params: CancelCaptchaChallengeParams,
            val requestOptions: ApiRequest.Options
        )
    }

    private companion object {
        private val REQUEST_OPTIONS = ApiRequest.Options("pk_test_vOo1umqsYxSrP5UXfOeL3ecm")

        val TEST_ARGS = IntentConfirmationChallengeArgs(
            publishableKey = "pk_test_123",
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            productUsage = listOf("PaymentSheet"),
        )
    }
}
