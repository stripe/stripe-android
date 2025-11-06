package com.stripe.android.challenge.confirmation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `initial state has showWebView as false`() {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val viewModel = createViewModel(fakeBridgeHandler)

        assertThat(viewModel.showWebView.value).isFalse()
    }

    @Test
    fun `when Ready event is received, showWebView becomes true`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val viewModel = createViewModel(fakeBridgeHandler)

        viewModel.showWebView.test {
            // Initial state
            assertThat(awaitItem()).isFalse()

            // Emit Ready event
            fakeBridgeHandler.emitEvent(ConfirmationChallengeBridgeEvent.Ready)

            // Should emit true
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `when Success event is received, result emits Success with clientSecret`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val viewModel = createViewModel(fakeBridgeHandler)
        val expectedClientSecret = "pi_test_secret_123"

        viewModel.result.test {
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Success(clientSecret = expectedClientSecret)
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Success::class.java)
            val successResult = result as IntentConfirmationChallengeActivityResult.Success
            assertThat(successResult.clientSecret).isEqualTo(expectedClientSecret)

            expectNoEvents()
        }
    }

    @Test
    fun `when Error event is received, result emits Failed with error`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val viewModel = createViewModel(fakeBridgeHandler)
        val expectedError = IOException("Network error")

        viewModel.result.test {
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Error(cause = expectedError)
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
            val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
            assertThat(failedResult.error).isEqualTo(expectedError)

            expectNoEvents()
        }
    }

    private fun createViewModel(
        bridgeHandler: ConfirmationChallengeBridgeHandler
    ) = IntentConfirmationChallengeViewModel(
        bridgeHandler = bridgeHandler
    )
}
