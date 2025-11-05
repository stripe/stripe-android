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

    @Test
    fun `when multiple events are received, they are processed in order`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val viewModel = createViewModel(fakeBridgeHandler)
        val clientSecret = "pi_test_secret_456"

        viewModel.showWebView.test {
            // Initial state
            assertThat(awaitItem()).isFalse()

            // Emit Ready event
            fakeBridgeHandler.emitEvent(ConfirmationChallengeBridgeEvent.Ready)

            // showWebView should become true
            assertThat(awaitItem()).isTrue()
        }

        viewModel.result.test {
            // Emit Success event
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Success(clientSecret = clientSecret)
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Success::class.java)
            val successResult = result as IntentConfirmationChallengeActivityResult.Success
            assertThat(successResult.clientSecret).isEqualTo(clientSecret)

            expectNoEvents()
        }
    }

    @Test
    fun `when Error event is received with different error types, they are all handled`() = runTest {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val viewModel = createViewModel(fakeBridgeHandler)

        viewModel.result.test {
            // Test with IllegalArgumentException
            val illegalArgException = IllegalArgumentException("Invalid argument")
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Error(cause = illegalArgException)
            )

            val result1 = awaitItem()
            assertThat(result1).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
            val failedResult1 = result1 as IntentConfirmationChallengeActivityResult.Failed
            assertThat(failedResult1.error).isEqualTo(illegalArgException)

            // Test with generic Exception
            val genericException = Exception("Generic error")
            fakeBridgeHandler.emitEvent(
                ConfirmationChallengeBridgeEvent.Error(cause = genericException)
            )

            val result2 = awaitItem()
            assertThat(result2).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
            val failedResult2 = result2 as IntentConfirmationChallengeActivityResult.Failed
            assertThat(failedResult2.error).isEqualTo(genericException)

            expectNoEvents()
        }
    }

    @Test
    fun `bridgeHandler is accessible via viewModel property`() {
        val fakeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val viewModel = createViewModel(fakeBridgeHandler)

        assertThat(viewModel.bridgeHandler).isEqualTo(fakeBridgeHandler)
    }

    private fun createViewModel(
        bridgeHandler: ConfirmationChallengeBridgeHandler
    ) = IntentConfirmationChallengeViewModel(
        bridgeHandler = bridgeHandler
    )
}
