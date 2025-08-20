package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.polling.IntentStatusPoller
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PollingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `Emits provided time limit as remaining duration`() = runTest(testDispatcher) {
        val timeLimit = 5.minutes

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
        )

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit)
    }

    @Test
    fun `Remaining time is updated every second`() = runTest(testDispatcher) {
        val timeLimit = 5.minutes

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
        )

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit)

        advanceTimeBy(1.seconds + 1.milliseconds)

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit - 1.seconds)

        advanceTimeBy(1.seconds)

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit - 2.seconds)
    }

    @Test
    fun `Reflects cancellation in UI state`() = runTest(testDispatcher) {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
        )

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        viewModel.handleCancel()

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()
        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Canceled)
    }

    @Test
    fun `Reflects failure in UI state`() = runTest(testDispatcher) {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
        )

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        fakePoller.emitNextPollResult(StripeIntent.Status.Canceled)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
    }

    @Test
    fun `Performs one-off poll when time limit has been exceeded`() = runTest(StandardTestDispatcher()) {
        val fakePoller = FakeIntentStatusPoller().apply {
            emitNextPollResult(StripeIntent.Status.RequiresAction)
        }

        val viewModel = createPollingViewModel(
            poller = fakePoller,
            timeLimit = 10.seconds,
        )
        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        fakePoller.enqueueForcePollResult(StripeIntent.Status.RequiresCapture)

        advanceTimeBy(10.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

        advanceTimeBy(3.seconds)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
    }

    @Test
    fun `Pausing stops the poller and doesn't emit new state`() = runTest(testDispatcher) {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
        )

        advanceTimeBy(5.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()
        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        viewModel.pausePolling()

        advanceTimeBy(1.minutes)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()
        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)
    }

    @Test
    fun `Continues polling after pausing and resuming`() = runTest(testDispatcher) {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
        )

        advanceTimeBy(5.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        viewModel.pausePolling()

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

        viewModel.resumePolling()

        advanceTimeBy(5.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()
    }

    @Test
    fun `Emits correct time limit when restoring from process death`() = runTest(testDispatcher) {
        val currentTime = System.currentTimeMillis()
        val timeProvider = FakeTimeProvider(timeInMillis = currentTime)

        val timeLimit = 10.minutes
        val alreadyPassed = 2.minutes

        val savedStateHandle = SavedStateHandle().apply {
            val mockedStartTime = currentTime - alreadyPassed.inWholeMilliseconds
            this["KEY_CURRENT_POLLING_START_TIME"] = mockedStartTime
        }

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
            timeProvider = timeProvider,
            savedStateHandle = savedStateHandle,
        )

        val remainingTime = timeLimit - alreadyPassed
        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(remainingTime)
    }

    @Test
    fun `Stops poller when encountering failed result`() = runTest(testDispatcher) {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            timeLimit = 5.minutes,
            poller = fakePoller,
            initialDelay = ZERO,
        )

        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        // Anything that's not succeeded or requires_action is considered failure
        fakePoller.emitNextPollResult(StripeIntent.Status.RequiresPaymentMethod)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()
    }
}

private fun createPollingViewModel(
    timeLimit: Duration = 5.minutes,
    initialDelay: Duration = 5.seconds,
    poller: IntentStatusPoller = FakeIntentStatusPoller(),
    timeProvider: TimeProvider = FakeTimeProvider(),
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
): PollingViewModel {
    return PollingViewModel(
        args = PollingViewModel.Args(
            clientSecret = "secret",
            timeLimit = timeLimit,
            initialDelay = initialDelay,
            pollingStrategy = IntentStatusPoller.PollingStrategy.ExponentialBackoff(maxAttempts = 10),
            ctaText = R.string.stripe_upi_polling_message,
            stripeAccountId = null
        ),
        poller = poller,
        timeProvider = timeProvider,
        savedStateHandle = savedStateHandle,
    )
}
