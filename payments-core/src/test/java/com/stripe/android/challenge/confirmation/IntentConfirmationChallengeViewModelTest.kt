package com.stripe.android.challenge.confirmation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.confirmation.analytics.IntentConfirmationChallengeAnalyticsEventReporter
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.VerifyIntentConfirmationChallengeParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.CoroutineTestRule
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
    fun `when Ready event is received, bridgeReady emits value`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val fakeAnalyticsReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter()
        val viewModel = createViewModel(fakeBridgeHandler, fakeAnalyticsReporter)

        viewModel.bridgeReady.test {
            // Initial state
            expectNoEvents()

            // Emit Ready event
            fakeBridgeHandler.emitEvent(ConfirmationChallengeBridgeEvent.Ready)

            // Should emit value
            awaitItem()

            ensureAllEventsConsumed()
        }

        // Verify analytics
        assertThat(fakeAnalyticsReporter.calls).hasSize(1)
        assertThat(fakeAnalyticsReporter.calls.last()).isEqualTo(
            FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.WebViewLoaded
        )
    }

    @Test
    fun `when Success event is received, result emits Success with clientSecret`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val fakeAnalyticsReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter()
        val viewModel = createViewModel(fakeBridgeHandler, fakeAnalyticsReporter)
        val expectedClientSecret = "pi_test_secret_123"

        viewModel.result.test {
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Success(clientSecret = expectedClientSecret)
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Success::class.java)
            val successResult = result as IntentConfirmationChallengeActivityResult.Success
            assertThat(successResult.clientSecret).isEqualTo(expectedClientSecret)

            ensureAllEventsConsumed()
        }

        // Verify analytics
        assertThat(fakeAnalyticsReporter.calls).hasSize(1)
        assertThat(fakeAnalyticsReporter.calls.last()).isEqualTo(
            FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Success
        )
    }

    @Test
    fun `when Error event is received, result emits Failed with error`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val fakeAnalyticsReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter()
        val viewModel = createViewModel(fakeBridgeHandler, fakeAnalyticsReporter)
        val expectedError = BridgeException(IOException("Network error"))

        viewModel.result.test {
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Error(error = expectedError)
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
            val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
            assertThat(failedResult.error).isEqualTo(expectedError)

            ensureAllEventsConsumed()
        }

        // Verify analytics
        assertThat(fakeAnalyticsReporter.calls).hasSize(1)
        val errorCall =
            fakeAnalyticsReporter.calls.last() as FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Error
        assertThat(errorCall.errorType).isNull()
        assertThat(errorCall.errorCode).isNull()
        assertThat(errorCall.fromBridge).isTrue()
    }

    @Test
    fun `when handleWebViewError is called, result emits Failed with WebViewError`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val fakeAnalyticsReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter()
        val viewModel = createViewModel(fakeBridgeHandler, fakeAnalyticsReporter)
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
        assertThat(fakeAnalyticsReporter.calls).hasSize(1)
        val errorCall =
            fakeAnalyticsReporter.calls.last() as FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Error
        assertThat(errorCall.errorType).isEqualTo("generic_resource_error")
        assertThat(errorCall.errorCode).isEqualTo("-2")
        assertThat(errorCall.fromBridge).isFalse()
    }

    @Test
    fun `when closeClicked is called and verify succeeds, result emits Canceled`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val fakeStripeRepository = FakeStripeRepository(
            verifyResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        )
        val viewModel = createViewModel(
            bridgeHandler = fakeBridgeHandler,
            stripeRepository = fakeStripeRepository,
        )

        viewModel.result.test {
            viewModel.closeClicked()

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Canceled::class.java)
            val canceledResult = result as IntentConfirmationChallengeActivityResult.Canceled
            assertThat(canceledResult.clientSecret).isEqualTo(TEST_ARGS.intent.clientSecret)

            ensureAllEventsConsumed()
        }

        // Verify the API was called with correct params
        assertThat(fakeStripeRepository.verifyCallCount).isEqualTo(1)
        assertThat(fakeStripeRepository.lastVerificationUrl).isEqualTo(TEST_STRIPE_JS.verificationUrl)
        assertThat(fakeStripeRepository.lastVerifyParams?.clientSecret).isEqualTo(TEST_ARGS.intent.clientSecret)
        assertThat(fakeStripeRepository.lastVerifyParams?.captchaVendorName)
            .isEqualTo(TEST_STRIPE_JS.captchaVendorName?.code)
    }

    @Test
    fun `when closeClicked is called and verify fails, result emits Failed`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val expectedError = IOException("Network error")
        val fakeStripeRepository = FakeStripeRepository(
            verifyResult = Result.failure(expectedError)
        )
        val viewModel = createViewModel(
            bridgeHandler = fakeBridgeHandler,
            stripeRepository = fakeStripeRepository,
        )

        viewModel.result.test {
            viewModel.closeClicked()

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
            val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
            assertThat(failedResult.clientSecret).isEqualTo(TEST_ARGS.intent.clientSecret)
            assertThat(failedResult.error).isEqualTo(expectedError)

            ensureAllEventsConsumed()
        }

        assertThat(fakeStripeRepository.verifyCallCount).isEqualTo(1)
    }

    @Test
    fun `when closeClicked is called with null stripeJs, result emits Canceled without API call`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val fakeStripeRepository = FakeStripeRepository(
            verifyResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        )
        val viewModel = createViewModel(
            bridgeHandler = fakeBridgeHandler,
            stripeRepository = fakeStripeRepository,
            args = TEST_ARGS_NO_STRIPE_JS,
        )

        viewModel.result.test {
            viewModel.closeClicked()

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Canceled::class.java)
            val canceledResult = result as IntentConfirmationChallengeActivityResult.Canceled
            assertThat(canceledResult.clientSecret).isEqualTo(TEST_ARGS_NO_STRIPE_JS.intent.clientSecret)

            ensureAllEventsConsumed()
        }

        // Verify the API was NOT called
        assertThat(fakeStripeRepository.verifyCallCount).isEqualTo(0)
    }

    @Test
    fun `when onStart is called, analytics start is reported`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val fakeAnalyticsReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter()
        val viewModel = createViewModel(fakeBridgeHandler, fakeAnalyticsReporter)
        val lifecycleOwner = object : LifecycleOwner {
            private val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle = registry

            fun start() {
                registry.addObserver(viewModel)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
        }

        lifecycleOwner.start()

        assertThat(fakeAnalyticsReporter.calls).hasSize(1)
        assertThat(fakeAnalyticsReporter.calls.first()).isEqualTo(
            FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Start
        )
    }

    private fun createViewModel(
        bridgeHandler: ConfirmationChallengeBridgeHandler,
        analyticsReporter: IntentConfirmationChallengeAnalyticsEventReporter =
            FakeIntentConfirmationChallengeAnalyticsEventReporter(),
        stripeRepository: StripeRepository = FakeStripeRepository(
            verifyResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        ),
        args: IntentConfirmationChallengeArgs = TEST_ARGS,
    ) = IntentConfirmationChallengeViewModel(
        args = args,
        bridgeHandler = bridgeHandler,
        workContext = testDispatcher,
        analyticsEventReporter = analyticsReporter,
        userAgent = "fake-user-agent",
        stripeRepository = stripeRepository,
    )

    private class FakeStripeRepository(
        private val verifyResult: Result<StripeIntent>,
    ) : AbsFakeStripeRepository() {
        var verifyCallCount = 0
            private set
        var lastVerificationUrl: String? = null
            private set
        var lastVerifyParams: VerifyIntentConfirmationChallengeParams? = null
            private set

        override suspend fun verifyIntentConfirmationChallenge(
            verificationUrl: String,
            params: VerifyIntentConfirmationChallengeParams,
            requestOptions: ApiRequest.Options
        ): Result<StripeIntent> {
            verifyCallCount++
            lastVerificationUrl = verificationUrl
            lastVerifyParams = params
            return verifyResult
        }
    }

    private companion object {
        val TEST_STRIPE_JS = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.StripeJs(
            siteKey = "test_site_key",
            verificationUrl = "https://api.stripe.com/v1/payment_intents/verify",
            captchaVendorName = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.CaptchaVendorName.HCaptcha,
        )

        val TEST_ARGS = IntentConfirmationChallengeArgs(
            publishableKey = "pk_test_123",
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            productUsage = listOf("PaymentSheet"),
            stripeJs = TEST_STRIPE_JS,
        )

        val TEST_ARGS_NO_STRIPE_JS = IntentConfirmationChallengeArgs(
            publishableKey = "pk_test_123",
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            productUsage = listOf("PaymentSheet"),
            stripeJs = null,
        )
    }
}
