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
import com.stripe.android.common.nfcscan.ui.NfcScanningStatus
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

    @Test
    fun `viewState contains tap zone from resolver`() = runScenario(
        tapZone = TapZone(xBias = 0.3f, yBias = 0.7f),
    ) {
        assertThat(viewModel.viewState.value).isEqualTo(
            NfcScanningViewState.Available(
                tapZone = TapZone(xBias = 0.3f, yBias = 0.7f),
                status = NfcScanningStatus.Idle(error = null),
            ),
        )
    }

    @Test
    fun `viewState contains error when scanner is disabled`() = runScenario(
        enablementState = NfcCardScanner.EnablementState.Disabled(
            "Turn developer options off and try again.".resolvableString,
        ),
    ) {
        assertThat(viewModel.viewState.value).isEqualTo(
            NfcScanningViewState.Unavailable(
                "Turn developer options off and try again.".resolvableString,
            ),
        )
    }

    @Test
    fun `handleViewAction Close emits Canceled result`() = runScenario {
        viewModel.result.test {
            viewModel.handleViewAction(NfcScanningViewAction.Close)

            assertThat(awaitItem()).isEqualTo(NfcScanningContract.Result.Canceled)
        }

        assertThat(fakeEventReporter.onNfcScanCancelledCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `handleViewAction Close does not report cancelled when scanner is disabled`() = runScenario(
        enablementState = NfcCardScanner.EnablementState.Disabled(
            "Turn developer options off and try again.".resolvableString,
        ),
    ) {
        viewModel.result.test {
            viewModel.handleViewAction(NfcScanningViewAction.Close)

            assertThat(awaitItem()).isEqualTo(NfcScanningContract.Result.Canceled)
        }

        fakeEventReporter.onNfcScanCancelledCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `card scanner failed reports attempt failed`() = runScenario {
        scannerState.emit(
            NfcCardScanner.State.Failed(
                error = NfcCardScanner.Error(
                    userMessage = R.string.stripe_nfc_expired_error.resolvableString,
                ),
            ),
        )
        assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isNotNull()
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
            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Idle(error = null))
            scannerState.emit(NfcCardScanner.State.Scanning)
            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Scanning)
        }

        assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner in scanned status updates the view model state to scanned`() = runScenario {
        viewModel.viewState.test {
            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Idle(error = null))

            scannerState.emit(
                NfcCardScanner.State.Complete(
                    ScannedCardData(
                        cardNumber = "4242424242424242",
                        expirationMonth = 12,
                        expirationYear = 2030,
                    ),
                ),
            )

            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Scanned)
        }

        assertThat(fakeEventReporter.onNfcScanAttemptSucceededCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `card scanner in failed status updates the view model state to idle with error`() = runScenario {
        val errorMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString

        viewModel.viewState.test {
            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Idle(error = null))

            scannerState.emit(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(userMessage = errorMessage),
                ),
            )

            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Idle(error = errorMessage))
            assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `card scanner in scanning status clears idle error`() = runScenario {
        val errorMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString

        viewModel.viewState.test {
            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Idle(error = null))

            scannerState.emit(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(userMessage = errorMessage),
                ),
            )
            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Idle(error = errorMessage))
            assertThat(fakeEventReporter.onNfcScanAttemptFailedCalls.awaitItem()).isNotNull()

            scannerState.emit(NfcCardScanner.State.Scanning)
            assertThat((awaitItem().asAvailable()).status)
                .isEqualTo(NfcScanningStatus.Scanning)
            assertThat(fakeEventReporter.onNfcScanAttemptStartedCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `card scanner Complete state emits Complete result after success animation`() = runScenario {
        viewModel.result.test {
            scannerState.emit(
                NfcCardScanner.State.Complete(
                    ScannedCardData(
                        cardNumber = "4242424242424242",
                        expirationMonth = 12,
                        expirationYear = 2030,
                    ),
                ),
            )

            expectNoEvents()

            viewModel.handleViewAction(NfcScanningViewAction.SuccessShown)

            assertThat(awaitItem()).isEqualTo(
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
    fun `onCleared cancels view model scope`() = runTest(dispatcher) {
        val viewModelScope = CoroutineScope(dispatcher + Job())
        val viewModel = NfcScanningViewModel(
            viewModelScope = viewModelScope,
            tapZoneResolver = FakeTapZoneResolver(),
            cardScanner = FakeNfcCardScanner(),
            eventReporter = FakeNfcScanningEventReporter(),
        )
        val viewModelStore = ViewModelStore().apply {
            put("test", viewModel)
        }

        viewModelStore.clear()

        assertThat(viewModelScope.coroutineContext[Job]?.isCancelled).isTrue()
    }

    private fun NfcScanningViewState.asAvailable(): NfcScanningViewState.Available {
        assertThat(this).isInstanceOf<NfcScanningViewState.Available>()

        return this as NfcScanningViewState.Available
    }

    private fun runScenario(
        tapZone: TapZone = TapZone(xBias = 0.5f, yBias = 0.5f),
        enablementState: NfcCardScanner.EnablementState = NfcCardScanner.EnablementState.Enabled,
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val scannerState = MutableSharedFlow<NfcCardScanner.State>()
        val fakeCardScanner = FakeNfcCardScanner(
            stateFlow = scannerState,
            enablementState = enablementState,
        )
        val fakeEventReporter = FakeNfcScanningEventReporter()
        val viewModel = NfcScanningViewModel(
            viewModelScope = CoroutineScope(dispatcher),
            tapZoneResolver = FakeTapZoneResolver(tapZone),
            cardScanner = fakeCardScanner,
            eventReporter = fakeEventReporter,
        )

        if (enablementState.enabled) {
            assertThat(fakeEventReporter.onNfcScanStartedCalls.awaitItem()).isNotNull()
        }

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
