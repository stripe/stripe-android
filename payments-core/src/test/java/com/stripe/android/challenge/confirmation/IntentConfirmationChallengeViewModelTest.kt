package com.stripe.android.challenge.confirmation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.confirmation.analytics.IntentConfirmationChallengeAnalyticsEventReporter
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
        val expectedError = IOException("Network error")

        viewModel.result.test {
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Error(cause = expectedError)
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
        assertThat(errorCall.error).isEqualTo(expectedError)
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
        assertThat(errorCall.error).isEqualTo(webViewError)
        assertThat(errorCall.errorType).isEqualTo("generic_resource_error")
        assertThat(errorCall.errorCode).isEqualTo("-2")
        assertThat(errorCall.fromBridge).isFalse()
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
            FakeIntentConfirmationChallengeAnalyticsEventReporter()
    ) = IntentConfirmationChallengeViewModel(
        bridgeHandler = bridgeHandler,
        workContext = testDispatcher,
        analyticsEventReporter = analyticsReporter
    )
}
