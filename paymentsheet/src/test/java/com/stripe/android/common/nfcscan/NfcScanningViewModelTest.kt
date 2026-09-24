package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.analytics.FakeNfcScanningEventReporter
import com.stripe.android.common.nfcscan.analytics.NfcScanCancellationReason
import com.stripe.android.common.nfcscan.scanner.FakeNfcCardScanner
import com.stripe.android.common.nfcscan.scanner.GenericNfcScanningError
import com.stripe.android.common.nfcscan.scanner.NfcCardScanner
import com.stripe.android.common.nfcscan.scanner.ScannedCardData
import com.stripe.android.common.nfcscan.security.FakeIsDeviceSecureForNfc
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
            NfcScanningViewState.Ready(
                tapZone = TapZone(xBias = 0.3f, yBias = 0.7f),
                status = NfcScanningStatus.Idle(),
            ),
        )
    }

    @Test
    fun `viewState is NotSecure when device is not secure`() = runScenario(
        isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = false),
    ) {
        assertThat(viewModel.viewState.value).isEqualTo(NfcScanningViewState.NotSecure)
    }

    @Test
    fun `register does not start card scanner when device is not secure`() = runScenario(
        isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = false),
    ) {
        viewModel.register(mock())

        fakeCardScanner.startCalls.expectNoEvents()
        fakeTimeoutManager.startCalls.expectNoEvents()
    }

    @Test
    fun `register starts scanning after device becomes secure`() {
        val security = FakeIsDeviceSecureForNfc(result = false)

        runScenario(isDeviceSecureForNfc = security) {
            security.result = true
            val activity = mock<AppCompatActivity>()

            viewModel.register(activity)

            assertThat(viewModel.viewState.value).isEqualTo(
                NfcScanningViewState.Ready(
                    tapZone = TapZone(xBias = 0.5f, yBias = 0.5f),
                    status = NfcScanningStatus.Idle(),
                ),
            )
            assertThat(fakeTimeoutManager.startCalls.awaitItem()).isNotNull()
            assertThat(fakeCardScanner.startCalls.awaitItem()).isEqualTo(activity)
        }
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

        assertThat(fakeEventReporter.onNfcScanCancelledCalls.awaitItem()).isEqualTo(
            FakeNfcScanningEventReporter.NfcScanCancelledCall(
                reason = NfcScanCancellationReason.UserInitiated,
                numberOfAttempts = 0,
            ),
        )

        assertThat(fakeTimeoutManager.cancelCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `handleViewAction OpenDeveloperOptions emits event`() = runScenario {
        viewModel.event.test {
            viewModel.handleViewAction(NfcScanningViewAction.OpenDeveloperOptions)

            assertThat(awaitItem()).isEqualTo(NfcScanningEvent.OpenDeveloperOptions)
        }
    }

    @Test
    fun `card scanner failed reports attempt failed with error code`() = runScenario {
        val error = GenericNfcScanningError(
            errorCode = "expiredCard",
            userMessage = R.string.stripe_nfc_expired_error.resolvableString,
        )
        scannerState.emit(NfcCardScanner.State.Failed(error = error))
        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo(error)
    }

    @Test
    fun `card scanner failed reports attempt failed with error parameters`() = runScenario {
        val error = GenericNfcScanningError(
            errorCode = "nfcCardReadFailed",
            userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
            parameters = mapOf(
                "sw1" to "64",
                "sw2" to "00",
            ),
        )
        scannerState.emit(NfcCardScanner.State.Failed(error = error))

        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo(error)
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
            assertReadyStatus(awaitItem(), NfcScanningStatus.Idle())
            scannerState.emit(NfcCardScanner.State.Scanning)
            assertReadyStatus(awaitItem(), NfcScanningStatus.Scanning)
        }

        assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()
        assertThat(fakeTimeoutManager.resetCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner in scanned status updates the view model state to scanned`() = runScenario {
        viewModel.viewState.test {
            assertReadyStatus(awaitItem(), NfcScanningStatus.Idle())

            scannerState.emit(
                NfcCardScanner.State.Complete(
                    ScannedCardData(
                        cardNumber = "4242424242424242",
                        expirationMonth = 12,
                        expirationYear = 2030,
                    ),
                ),
            )

            assertReadyStatus(awaitItem(), NfcScanningStatus.Scanned)
        }

        assertThat(fakeEventReporter.onNfcScanAttemptSucceededCalls.awaitItem()).isNotNull()
        assertThat(fakeTimeoutManager.cancelCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner in failed status updates the view model state to error`() = runScenario {
        val errorMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString
        val error = GenericNfcScanningError(
            errorCode = "unknown",
            userMessage = errorMessage,
        )

        viewModel.viewState.test {
            assertReadyStatus(awaitItem(), NfcScanningStatus.Idle())

            scannerState.emit(NfcCardScanner.State.Failed(error = error))

            assertReadyStatus(awaitItem(), NfcScanningStatus.Idle(errorMessage))
            assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo(error)
            assertThat(fakeTimeoutManager.resetCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `card scanner in scanning status clears error`() = runScenario {
        val errorMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString
        val error = GenericNfcScanningError(
            errorCode = "unknown",
            userMessage = errorMessage,
        )

        viewModel.viewState.test {
            assertReadyStatus(awaitItem(), NfcScanningStatus.Idle())

            scannerState.emit(NfcCardScanner.State.Failed(error = error))
            assertReadyStatus(awaitItem(), NfcScanningStatus.Idle(errorMessage))
            assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo(error)

            scannerState.emit(NfcCardScanner.State.Scanning)
            assertReadyStatus(awaitItem(), NfcScanningStatus.Scanning)
            assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `handleViewAction ErrorShown updates status to idle`() = runScenario {
        val errorMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString
        val error = GenericNfcScanningError(
            errorCode = "unknown",
            userMessage = errorMessage,
        )

        scannerState.emit(NfcCardScanner.State.Failed(error = error))

        assertReadyStatus(viewModel.viewState.value, NfcScanningStatus.Idle(errorMessage))
        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo(error)

        viewModel.handleViewAction(NfcScanningViewAction.ErrorShown)

        assertReadyStatus(viewModel.viewState.value, NfcScanningStatus.Idle())
    }

    @Test
    fun `card scanner Complete state emits Complete result after success animation`() = runScenario {
        scannerState.emit(NfcCardScanner.State.Scanning)

        assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()

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
        assertThat(fakeEventReporter.onNfcScanSucceededCalls.awaitItem()).isEqualTo(1)
    }

    @Test
    fun `card scanner failed emits failed haptic feedback event`() = runScenario {
        val error = GenericNfcScanningError(
            errorCode = "expiredCard",
            userMessage = R.string.stripe_nfc_expired_error.resolvableString,
        )

        viewModel.event.test {
            scannerState.emit(NfcCardScanner.State.Failed(error = error))

            assertThat(awaitItem()).isEqualTo(
                NfcScanningEvent.TriggerHapticFeedback(HapticFeedbackType.Failed),
            )
        }

        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isEqualTo(error)
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
    fun `timeout emits Canceled result with timeout reason`() = runScenario {
        viewModel.event.test {
            fakeTimeoutManager.emitTimeout()

            val event = awaitItem()

            assertThat(event).isInstanceOf<NfcScanningEvent.CloseWithResult>()

            val resultEvent = event as NfcScanningEvent.CloseWithResult

            assertThat(resultEvent.result).isEqualTo(NfcScanningContract.Result.Canceled)
        }

        assertThat(fakeEventReporter.onNfcScanCancelledCalls.awaitItem()).isEqualTo(
            FakeNfcScanningEventReporter.NfcScanCancelledCall(
                reason = NfcScanCancellationReason.Timeout,
                numberOfAttempts = 0,
            ),
        )
        assertThat(fakeTimeoutManager.cancelCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `timeout does not cause flow cancellation after successful scan`() = runScenario {
        scannerState.emit(
            NfcCardScanner.State.Complete(
                ScannedCardData(
                    cardNumber = "4242424242424242",
                    expirationMonth = 12,
                    expirationYear = 2030,
                ),
            ),
        )

        assertThat(fakeEventReporter.onNfcScanAttemptSucceededCalls.awaitItem()).isNotNull()
        assertThat(fakeTimeoutManager.cancelCalls.awaitItem()).isNotNull()

        viewModel.event.test {
            fakeTimeoutManager.emitTimeout()
            expectNoEvents()
        }
    }

    @Test
    fun `A successful scan reports number of attempts`() = runScenario {
        scannerState.emit(NfcCardScanner.State.Scanning)
        assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()

        scannerState.emit(
            NfcCardScanner.State.Failed(
                error = GenericNfcScanningError(
                    errorCode = "expiredCard",
                    userMessage = R.string.stripe_nfc_expired_error.resolvableString,
                ),
            ),
        )
        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isNotNull()

        scannerState.emit(NfcCardScanner.State.Scanning)
        assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()

        scannerState.emit(
            NfcCardScanner.State.Complete(
                ScannedCardData(
                    cardNumber = "4242424242424242",
                    expirationMonth = 12,
                    expirationYear = 2030,
                ),
            ),
        )
        assertThat(fakeEventReporter.onNfcScanAttemptSucceededCalls.awaitItem()).isNotNull()

        viewModel.handleViewAction(NfcScanningViewAction.SuccessShown)

        assertThat(fakeEventReporter.onNfcScanSucceededCalls.awaitItem()).isEqualTo(2)
    }

    @Test
    fun `Cancellation reports number of attempts`() = runScenario {
        repeat(5) {
            scannerState.emit(NfcCardScanner.State.Scanning)
            assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()

            scannerState.emit(
                NfcCardScanner.State.Failed(
                    error = GenericNfcScanningError(
                        errorCode = "expiredCard",
                        userMessage = R.string.stripe_nfc_expired_error.resolvableString,
                    ),
                ),
            )

            assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isNotNull()
        }

        viewModel.handleViewAction(NfcScanningViewAction.Close)

        val cancelledEventCall = fakeEventReporter.onNfcScanCancelledCalls.awaitItem()

        assertThat(cancelledEventCall.reason).isEqualTo(NfcScanCancellationReason.UserInitiated)
        assertThat(cancelledEventCall.numberOfAttempts).isEqualTo(5)
    }

    @Test
    fun `onCleared cancels view model scope & timeout manager`() = runTest(dispatcher) {
        val viewModelScope = coroutineScopeCleanupRule.track(CoroutineScope(dispatcher + Job()))
        val fakeTimeoutManager = FakeNfcScanningTimeoutManager()
        val viewModel = NfcScanningViewModel(
            viewModelScope = viewModelScope,
            tapZoneResolver = FakeTapZoneResolver(),
            cardScanner = FakeNfcCardScanner(),
            timeoutManager = fakeTimeoutManager,
            eventReporter = FakeNfcScanningEventReporter(),
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(),
        ).also { viewModelStoreRule.track(it) }
        val viewModelStore = ViewModelStore().apply {
            put("test", viewModel)
        }

        assertThat(fakeTimeoutManager.startCalls.awaitItem()).isNotNull()

        viewModelStore.clear()

        assertThat(fakeTimeoutManager.cancelCalls.awaitItem()).isNotNull()
        assertThat(viewModelScope.coroutineContext[Job]?.isCancelled).isTrue()
        fakeTimeoutManager.ensureAllEventsConsumed()
    }

    private fun runScenario(
        tapZone: TapZone = TapZone(xBias = 0.5f, yBias = 0.5f),
        isDeviceSecureForNfc: FakeIsDeviceSecureForNfc = FakeIsDeviceSecureForNfc(),
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val scannerState = MutableSharedFlow<NfcCardScanner.State>()
        val fakeCardScanner = FakeNfcCardScanner(stateFlow = scannerState)
        val fakeEventReporter = FakeNfcScanningEventReporter()
        val fakeTimeoutManager = FakeNfcScanningTimeoutManager()
        val viewModel = NfcScanningViewModel(
            viewModelScope = coroutineScopeCleanupRule.track(CoroutineScope(dispatcher)),
            tapZoneResolver = FakeTapZoneResolver(tapZone),
            cardScanner = fakeCardScanner,
            timeoutManager = fakeTimeoutManager,
            eventReporter = fakeEventReporter,
            isDeviceSecureForNfc = isDeviceSecureForNfc,
        ).also { viewModelStoreRule.track(it) }

        assertThat(fakeEventReporter.onNfcScanStartedCalls.awaitItem()).isNotNull()
        if (isDeviceSecureForNfc.result) {
            assertThat(fakeTimeoutManager.startCalls.awaitItem()).isNotNull()
        } else {
            fakeTimeoutManager.startCalls.expectNoEvents()
        }

        Scenario(
            viewModel = viewModel,
            fakeCardScanner = fakeCardScanner,
            fakeEventReporter = fakeEventReporter,
            fakeTimeoutManager = fakeTimeoutManager,
            scannerState = scannerState,
        ).block()

        fakeCardScanner.ensureAllEventsConsumed()
        fakeEventReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        val viewModel: NfcScanningViewModel,
        val fakeCardScanner: FakeNfcCardScanner,
        val fakeEventReporter: FakeNfcScanningEventReporter,
        val fakeTimeoutManager: FakeNfcScanningTimeoutManager,
        val scannerState: MutableSharedFlow<NfcCardScanner.State>,
    )

    private fun assertReadyStatus(
        state: NfcScanningViewState,
        status: NfcScanningStatus,
    ) {
        assertThat(state).isInstanceOf<NfcScanningViewState.Ready>()
        assertThat((state as NfcScanningViewState.Ready).status).isEqualTo(status)
    }
}
