package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent
import com.stripe.android.polling.IntentStatusPoller
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PollingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `Emits provided time limit as remaining duration`() = runBlocking {
        val timeLimit = 5.minutes

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
            dispatcher = testDispatcher,
        )

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit)
    }

    @Test
    fun `Remaining time is updated every second`() = runBlocking {
        val timeLimit = 5.minutes

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
            dispatcher = testDispatcher,
        )

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit)

        testDispatcher.scheduler.advanceTimeBy(1.seconds.inWholeMilliseconds + 1)

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit - 1.seconds)

        testDispatcher.scheduler.advanceTimeBy(1.seconds.inWholeMilliseconds)

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit - 2.seconds)
    }

    @Test
    fun `Reflects cancellation in UI state`() = runBlocking {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
            dispatcher = testDispatcher,
        )

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        viewModel.handleCancel()

        assertThat(fakePoller.isActive).isFalse()
        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Canceled)
    }

    @Test
    fun `Reflects failure in UI state`() = runBlocking {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
            dispatcher = testDispatcher,
        )

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        fakePoller.emitNextPollResult(StripeIntent.Status.Canceled)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
    }

    @Test
    fun `Performs one-off poll when time limit has been exceeded`() {
        val fakePoller = FakeIntentStatusPoller().apply {
            emitNextPollResult(StripeIntent.Status.RequiresAction)
        }

        val viewModel = createPollingViewModel(
            poller = fakePoller,
            timeLimit = 10.seconds,
            dispatcher = testDispatcher,
        )

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        fakePoller.enqueueForcePollResult(StripeIntent.Status.RequiresCapture)

        testDispatcher.scheduler.advanceTimeBy(10.seconds.inWholeMilliseconds + 1)

        assertThat(fakePoller.isActive).isFalse()

        testDispatcher.scheduler.advanceTimeBy(3.seconds.inWholeMilliseconds + 1)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
    }

    @Test
    fun `Pausing stops the poller and doesn't emit new state`() = runBlocking {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
            dispatcher = testDispatcher,
        )

        testDispatcher.scheduler.advanceTimeBy(5.seconds.inWholeMilliseconds + 1)

        assertThat(fakePoller.isActive).isTrue()
        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)

        viewModel.pausePolling()

        testDispatcher.scheduler.advanceTimeBy(1.minutes.inWholeMilliseconds + 1)

        assertThat(fakePoller.isActive).isFalse()
        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)
    }

    @Test
    fun `Continues polling after pausing and resuming`() = runBlocking {
        val fakePoller = FakeIntentStatusPoller()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
            dispatcher = testDispatcher,
        )

        testDispatcher.scheduler.advanceTimeBy(5.seconds.inWholeMilliseconds + 1)

        assertThat(fakePoller.isActive).isTrue()

        viewModel.pausePolling()

        assertThat(fakePoller.isActive).isFalse()

        viewModel.resumePolling()

        testDispatcher.scheduler.advanceTimeBy(5.seconds.inWholeMilliseconds + 1)

        assertThat(fakePoller.isActive).isTrue()
    }

    @Test
    fun `Emits correct time limit when restoring from process death`() = runBlocking {
        val currentTime = System.currentTimeMillis()
        val timeProvider = FakeTimeProvider(timeInMillis = currentTime)

        val savedStateHandle = SavedStateHandle().apply {
            val mockedStartTime = currentTime - 2.minutes.inWholeMilliseconds
            this["KEY_CURRENT_POLLING_START_TIME"] = mockedStartTime
        }

        val timeLimit = 10.minutes

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
            timeProvider = timeProvider,
            savedStateHandle = savedStateHandle,
            dispatcher = testDispatcher,
        )

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(8.minutes)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun createPollingViewModel(
    timeLimit: Duration = 5.minutes,
    initialDelay: Duration = 5.seconds,
    poller: IntentStatusPoller = FakeIntentStatusPoller(),
    timeProvider: TimeProvider = FakeTimeProvider(),
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    dispatcher: TestDispatcher,
): PollingViewModel {
    return PollingViewModel(
        args = PollingViewModel.Args(
            clientSecret = "secret",
            timeLimit = timeLimit,
            initialDelay = initialDelay,
            maxAttempts = 10,
            injectorKey = "injector",
        ),
        poller = poller,
        timeProvider = timeProvider,
        dispatcher = dispatcher,
        savedStateHandle = savedStateHandle,
    )
}
