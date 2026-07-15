package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.polling.IntentStatusPoller
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakePollingAnalyticsEventReporter
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

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `Emits provided time limit as remaining duration`() = runTest(testDispatcher) {
        val timeLimit = 5.minutes

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
        ).also { viewModelStoreRule.track(it) }

        assertThat(viewModel.uiState.value.durationRemaining).isEqualTo(timeLimit)
    }

    @Test
    fun `Remaining time is updated every second`() = runTest(testDispatcher) {
        val timeLimit = 5.minutes

        val viewModel = createPollingViewModel(
            timeLimit = timeLimit,
        ).also { viewModelStoreRule.track(it) }

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
        ).also { viewModelStoreRule.track(it) }

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
        ).also { viewModelStoreRule.track(it) }

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
        ).also { viewModelStoreRule.track(it) }
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
        ).also { viewModelStoreRule.track(it) }

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
        ).also { viewModelStoreRule.track(it) }

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
        ).also { viewModelStoreRule.track(it) }

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
        ).also { viewModelStoreRule.track(it) }

        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        // Anything that's not succeeded or requires_action is considered failure
        fakePoller.emitNextPollResult(StripeIntent.Status.RequiresPaymentMethod)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()
    }

    @Test
    fun `Reports analytics when polling times out`() = runTest(StandardTestDispatcher()) {
        val fakePoller = FakeIntentStatusPoller().apply {
            emitNextPollResult(StripeIntent.Status.RequiresAction)
        }
        val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()

        createPollingViewModel(
            poller = fakePoller,
            timeLimit = 10.seconds,
            pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
        )
        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        fakePoller.enqueueForcePollResult(StripeIntent.Status.RequiresAction)

        advanceTimeBy(10.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

        advanceTimeBy(3.seconds)

        val call = pollingAnalyticsEventReporter.awaitCall()
        assertThat(call).isEqualTo(
            FakePollingAnalyticsEventReporter.Call.PollingTimedOut(
                paymentMethodType = "blik",
                lastKnownStatus = "RequiresAction",
                timeLimitSeconds = 10,
            )
        )
    }

    @Test
    fun `Reports analytics exactly once when PayNow polling times out`() = runTest(StandardTestDispatcher()) {
        val fakePoller = FakeIntentStatusPoller().apply {
            emitNextPollResult(StripeIntent.Status.RequiresAction)
        }
        val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()

        createPollingViewModel(
            poller = fakePoller,
            timeLimit = 10.seconds,
            pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
            paymentMethodType = "paynow",
        )
        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        fakePoller.enqueueForcePollResult(StripeIntent.Status.RequiresAction)

        advanceTimeBy(10.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

        advanceTimeBy(3.seconds)

        assertThat(pollingAnalyticsEventReporter.awaitCall()).isEqualTo(
            FakePollingAnalyticsEventReporter.Call.PollingTimedOut(
                paymentMethodType = "paynow",
                lastKnownStatus = "RequiresAction",
                timeLimitSeconds = 10,
            )
        )
        pollingAnalyticsEventReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `Reports analytics exactly once when PromptPay polling times out`() = runTest(StandardTestDispatcher()) {
        val fakePoller = FakeIntentStatusPoller().apply {
            emitNextPollResult(StripeIntent.Status.RequiresAction)
        }
        val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()

        createPollingViewModel(
            poller = fakePoller,
            timeLimit = 10.seconds,
            pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
            paymentMethodType = "promptpay",
        )
        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        fakePoller.enqueueForcePollResult(StripeIntent.Status.RequiresAction)

        advanceTimeBy(10.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

        advanceTimeBy(3.seconds)

        assertThat(pollingAnalyticsEventReporter.awaitCall()).isEqualTo(
            FakePollingAnalyticsEventReporter.Call.PollingTimedOut(
                paymentMethodType = "promptpay",
                lastKnownStatus = "RequiresAction",
                timeLimitSeconds = 10,
            )
        )
        pollingAnalyticsEventReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `Does not report analytics when polling succeeds after timeout`() = runTest(StandardTestDispatcher()) {
        val fakePoller = FakeIntentStatusPoller().apply {
            emitNextPollResult(StripeIntent.Status.RequiresAction)
        }
        val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()

        val viewModel = createPollingViewModel(
            poller = fakePoller,
            timeLimit = 10.seconds,
            pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
        )
        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        fakePoller.enqueueForcePollResult(StripeIntent.Status.Succeeded)

        advanceTimeBy(10.seconds + 1.milliseconds)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

        advanceTimeBy(3.seconds)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Success)
        pollingAnalyticsEventReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `Canceling during the timeout grace window does not clobber state or report analytics`() =
        // Uses the class-level testDispatcher (also bound to Dispatchers.Main via CoroutineTestRule)
        // rather than an ad-hoc StandardTestDispatcher, so that advanceTimeBy reliably controls
        // viewModelScope's delays and handleCancel() interleaves deterministically during the grace window.
        runTest(testDispatcher) {
            val fakePoller = FakeIntentStatusPoller().apply {
                emitNextPollResult(StripeIntent.Status.RequiresAction)
            }
            val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()

            val viewModel = createPollingViewModel(
                poller = fakePoller,
                timeLimit = 10.seconds,
                pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
            )
            assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

            // If the (buggy) delayed timeout job were allowed to complete, it would force-poll
            // this result and report a timeout for what is actually a user cancellation.
            fakePoller.enqueueForcePollResult(StripeIntent.Status.RequiresAction)

            advanceTimeBy(10.seconds + 1.milliseconds)

            // handleTimeLimitReached() stops the poller and enters its 3-second grace delay.
            assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

            viewModel.handleCancel()
            assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

            advanceTimeBy(3.seconds)

            assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Canceled)
            pollingAnalyticsEventReporter.ensureAllEventsConsumed()
        }

    @Test
    fun `Does not report a delayed timeout for a payment that already declined`() =
        runTest(StandardTestDispatcher()) {
            val fakePoller = FakeIntentStatusPoller()
            val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()

            val viewModel = createPollingViewModel(
                timeLimit = 5.minutes,
                poller = fakePoller,
                initialDelay = ZERO,
                pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
            )
            assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

            // Anything that's not succeeded or requires_action is considered a failure, well
            // before the 5 minute deadline.
            fakePoller.emitNextPollResult(StripeIntent.Status.RequiresPaymentMethod)

            assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
            assertThat(fakePoller.pollingTurbine.awaitItem()).isFalse()

            // Advance past the original deadline (plus the timeout handler's grace delay). If the
            // pending timeout job weren't canceled, it would force-poll and report a timeout for
            // this already-resolved decline.
            advanceTimeBy(5.minutes + 3.seconds + 1.milliseconds)

            assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Failed)
            pollingAnalyticsEventReporter.ensureAllEventsConsumed()
        }

    @Test
    fun `Does not report a delayed timeout for a payment that already succeeded`() =
        runTest(StandardTestDispatcher()) {
            val fakePoller = FakeIntentStatusPoller()
            val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()

            val viewModel = createPollingViewModel(
                timeLimit = 5.minutes,
                poller = fakePoller,
                initialDelay = ZERO,
                pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
            )
            assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

            fakePoller.emitNextPollResult(StripeIntent.Status.Succeeded)

            // Unlike a Failed transition, a Succeeded transition doesn't explicitly stop the
            // poller here (DefaultIntentStatusPoller stops itself once it observes a terminal
            // status), so no second turbine item is expected.
            assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Success)

            advanceTimeBy(5.minutes + 3.seconds + 1.milliseconds)

            assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Success)
            pollingAnalyticsEventReporter.ensureAllEventsConsumed()
        }

    @Test
    fun `QR code shown on start when QR code available`() = runTest(testDispatcher) {
        val viewModel = createPollingViewModel(
            qrCodeUrl = "valid_url"
        ).also { viewModelStoreRule.track(it) }

        assertThat(viewModel.uiState.value.shouldShowQrCode).isTrue()
    }

    @Test
    fun `QR code hidden on start when QR code not available`() = runTest(testDispatcher) {
        val viewModel = createPollingViewModel(
            qrCodeUrl = null,
        ).also { viewModelStoreRule.track(it) }

        assertThat(viewModel.uiState.value.shouldShowQrCode).isFalse()
    }

    @Test
    fun `QR code hidden on cancel`() = runTest(testDispatcher) {
        val viewModel = createPollingViewModel(
            qrCodeUrl = "valid_url"
        ).also { viewModelStoreRule.track(it) }

        viewModel.handleCancel()

        assertThat(viewModel.uiState.value.shouldShowQrCode).isFalse()
    }

    @Test
    fun `QR code hidden on hide QR code`() = runTest(testDispatcher) {
        val viewModel = createPollingViewModel(
            qrCodeUrl = "valid_url"
        ).also { viewModelStoreRule.track(it) }

        viewModel.hideQrCode()

        assertThat(viewModel.uiState.value.shouldShowQrCode).isFalse()
    }

    @Test
    fun `QR code hidden when polling state is not active`() = runTest(StandardTestDispatcher()) {
        val fakePoller = FakeIntentStatusPoller()
        val viewModel = createPollingViewModel(
            qrCodeUrl = "valid_url",
            poller = fakePoller,
        ).also { viewModelStoreRule.track(it) }

        assertThat(viewModel.uiState.value.shouldShowQrCode).isTrue()
        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()

        fakePoller.emitNextPollResult(StripeIntent.Status.Succeeded)

        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Success)
        assertThat(viewModel.uiState.value.shouldShowQrCode).isFalse()
    }

    @Test
    fun `QR code not shown when previously hidden and polling state changes`() = runTest(testDispatcher) {
        val fakePoller = FakeIntentStatusPoller()
        val viewModel = createPollingViewModel(
            qrCodeUrl = "valid_url",
            poller = fakePoller,
        ).also { viewModelStoreRule.track(it) }

        assertThat(viewModel.uiState.value.shouldShowQrCode).isTrue()

        viewModel.hideQrCode()
        assertThat(viewModel.uiState.value.shouldShowQrCode).isFalse()

        fakePoller.emitNextPollResult(StripeIntent.Status.RequiresAction)

        assertThat(fakePoller.pollingTurbine.awaitItem()).isTrue()
        assertThat(viewModel.uiState.value.pollingState).isEqualTo(PollingState.Active)
        assertThat(viewModel.uiState.value.shouldShowQrCode).isFalse()
    }
}

private fun createPollingViewModel(
    timeLimit: Duration = 5.minutes,
    initialDelay: Duration = 5.seconds,
    poller: IntentStatusPoller = FakeIntentStatusPoller(),
    timeProvider: TimeProvider = FakeTimeProvider(),
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    qrCodeUrl: String? = null,
    pollingAnalyticsEventReporter: FakePollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter(),
    paymentMethodType: String = "blik",
): PollingViewModel {
    return PollingViewModel(
        args = PollingViewModel.Args(
            clientSecret = "secret",
            timeLimit = timeLimit,
            initialDelay = initialDelay,
            ctaText = R.string.stripe_blik_confirm_payment,
            stripeAccountId = null,
            qrCodeUrl = qrCodeUrl,
            paymentMethodType = paymentMethodType,
        ),
        poller = poller,
        timeProvider = timeProvider,
        savedStateHandle = savedStateHandle,
        pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
    )
}
