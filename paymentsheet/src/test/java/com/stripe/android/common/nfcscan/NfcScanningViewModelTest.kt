package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.analytics.FakeNfcScanningEventReporter
import com.stripe.android.common.nfcscan.scanner.FakeNfcCardScanner
import com.stripe.android.common.nfcscan.scanner.NfcCardScanner
import com.stripe.android.common.nfcscan.scanner.ScannedCardData
import com.stripe.android.common.nfcscan.tapzone.FakeTapZoneResolver
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.common.nfcscan.ui.HapticFeedbackType
import com.stripe.android.common.nfcscan.ui.NfcScanningStatus
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

internal class NfcScanningViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @Test
    fun `viewState contains tap zone from resolver`() = runScenario(
        tapZone = TapZone(xBias = 0.3f, yBias = 0.7f),
    ) {
        assertThat(viewModel.viewState.value).isEqualTo(
            NfcScanningViewState(
                tapZone = TapZone(xBias = 0.3f, yBias = 0.7f),
                status = NfcScanningStatus.Idle(error = null),
            ),
        )
    }

    @Test
    fun `handleViewAction Close emits Canceled result`() = runScenario {
        viewModel.event.test {
            viewModel.handleViewAction(NfcScanningViewAction.Close)

            val event = awaitItem()

            assertThat(event).isInstanceOf<NfcScanningEvent.CloseWithResult>()

            val resultEvent = event as NfcScanningEvent.CloseWithResult

            assertThat(resultEvent.result).isEqualTo(NfcScanningContract.Result.Canceled)
        }

        assertThat(fakeEventReporter.onNfcScanCancelledCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner failed reports attempt failed with error code`() = runScenario {
        scannerState.emit(
            NfcCardScanner.State.Failed(
                error = NfcCardScanner.Error(
                    code = "expiredCard",
                    userMessage = R.string.stripe_nfc_expired_error.resolvableString,
                ),
            ),
        )
        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo("expiredCard")
    }

    @Test
    fun `register starts card scanner with activity`() = runScenario {
        val activity = mock<AppCompatActivity>()

        viewModel.register(activity)

        assertThat(fakeCardScanner.startCalls.awaitItem()).isEqualTo(activity)
    }

    @Test
    fun `card scanner in scanning status updates the view model state to scanning`() = runScenario {
        viewModel.viewState.test {
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle(error = null))
            scannerState.emit(NfcCardScanner.State.Scanning)
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Scanning)
        }

        assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner in scanned status updates the view model state to scanned`() = runScenario {
        viewModel.viewState.test {
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle(error = null))

            scannerState.emit(
                NfcCardScanner.State.Complete(
                    ScannedCardData(
                        cardNumber = "4242424242424242",
                        expirationMonth = 12,
                        expirationYear = 2030,
                    ),
                ),
            )

            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Scanned)
        }

        assertThat(fakeEventReporter.onNfcScanAttemptSucceededCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner in failed status updates the view model state to idle with error`() = runScenario {
        val errorMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString

        viewModel.viewState.test {
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle(error = null))

            scannerState.emit(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(
                        code = "unknown",
                        userMessage = errorMessage,
                    ),
                ),
            )

            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle(error = errorMessage))
            assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo("unknown")
        }
    }

    @Test
    fun `card scanner in scanning status clears idle error`() = runScenario {
        val errorMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString

        viewModel.viewState.test {
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle(error = null))

            scannerState.emit(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(
                        code = "unknown",
                        userMessage = errorMessage,
                    ),
                ),
            )
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle(error = errorMessage))
            assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo("unknown")

            scannerState.emit(NfcCardScanner.State.Scanning)
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Scanning)
            assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `card scanner Complete state emits Complete result after success animation`() = runScenario {
        viewModel.event.test {
            scannerState.emit(
                NfcCardScanner.State.Complete(
                    ScannedCardData(
                        cardNumber = "4242424242424242",
                        expirationMonth = 12,
                        expirationYear = 2030,
                    ),
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                NfcScanningEvent.TriggerHapticFeedback(HapticFeedbackType.Success),
            )

            viewModel.handleViewAction(NfcScanningViewAction.SuccessShown)

            val event = awaitItem()

            assertThat(event).isInstanceOf<NfcScanningEvent.CloseWithResult>()

            val resultEvent = event as NfcScanningEvent.CloseWithResult

            assertThat(resultEvent.result).isEqualTo(
                NfcScanningContract.Result.Complete(
                    cardNumber = "4242424242424242",
                    expirationMonth = 12,
                    expirationYear = 2030,
                ),
            )
        }

        assertThat(fakeEventReporter.onNfcScanAttemptSucceededCalls.awaitItem()).isNotNull()
        assertThat(fakeEventReporter.onNfcScanSucceededCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner failed emits failed haptic feedback event`() = runScenario {
        viewModel.event.test {
            scannerState.emit(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(
                        code = "expiredCard",
                        userMessage = R.string.stripe_nfc_expired_error.resolvableString,
                    ),
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                NfcScanningEvent.TriggerHapticFeedback(HapticFeedbackType.Failed),
            )
        }

        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo("expiredCard")
    }

    @Test
    fun `card scanner complete emits success haptic feedback event`() = runScenario {
        viewModel.event.test {
            scannerState.emit(
                NfcCardScanner.State.Complete(
                    ScannedCardData(
                        cardNumber = "4242424242424242",
                        expirationMonth = 12,
                        expirationYear = 2030,
                    ),
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                NfcScanningEvent.TriggerHapticFeedback(HapticFeedbackType.Success),
            )
        }

        assertThat(fakeEventReporter.onNfcScanAttemptSucceededCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `onCleared cancels view model scope`() = runTest(dispatcher) {
        val viewModelScope = coroutineScopeCleanupRule.track(CoroutineScope(dispatcher + Job()))
        val viewModel = NfcScanningViewModel(
            viewModelScope = viewModelScope,
            tapZoneResolver = FakeTapZoneResolver(),
            cardScanner = FakeNfcCardScanner(),
            eventReporter = FakeNfcScanningEventReporter(),
        ).also { viewModelStoreRule.track(it) }
        val viewModelStore = ViewModelStore().apply {
            put("test", viewModel)
        }

        viewModelStore.clear()

        assertThat(viewModelScope.coroutineContext[Job]?.isCancelled).isTrue()
    }

    private fun runScenario(
        tapZone: TapZone = TapZone(xBias = 0.5f, yBias = 0.5f),
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val scannerState = MutableSharedFlow<NfcCardScanner.State>()
        val fakeCardScanner = FakeNfcCardScanner(stateFlow = scannerState)
        val fakeEventReporter = FakeNfcScanningEventReporter()
        val viewModel = NfcScanningViewModel(
            viewModelScope = coroutineScopeCleanupRule.track(CoroutineScope(dispatcher)),
            tapZoneResolver = FakeTapZoneResolver(tapZone),
            cardScanner = fakeCardScanner,
            eventReporter = fakeEventReporter,
        ).also { viewModelStoreRule.track(it) }

        assertThat(fakeEventReporter.onNfcScanStartedCalls.awaitItem()).isNotNull()

        Scenario(
            viewModel = viewModel,
            fakeCardScanner = fakeCardScanner,
            fakeEventReporter = fakeEventReporter,
            scannerState = scannerState,
        ).block()

        fakeCardScanner.ensureAllEventsConsumed()
        fakeEventReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        val viewModel: NfcScanningViewModel,
        val fakeCardScanner: FakeNfcCardScanner,
        val fakeEventReporter: FakeNfcScanningEventReporter,
        val scannerState: MutableSharedFlow<NfcCardScanner.State>,
    )
}
