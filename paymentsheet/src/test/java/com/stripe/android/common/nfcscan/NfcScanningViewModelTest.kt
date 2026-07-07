package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcCardScanner
import com.stripe.android.common.nfcscan.scanner.NfcCardScanner
import com.stripe.android.common.nfcscan.scanner.ScannedCardData
import com.stripe.android.common.nfcscan.tapzone.FakeTapZoneResolver
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.common.nfcscan.ui.NfcScanningStatus
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
            NfcScanningViewState(
                tapZone = TapZone(xBias = 0.3f, yBias = 0.7f),
                status = NfcScanningStatus.Idle,
            ),
        )
    }

    @Test
    fun `handleViewAction Close emits Canceled result`() = runScenario {
        viewModel.result.test {
            viewModel.handleViewAction(NfcScanningViewAction.Close)

            assertThat(awaitItem()).isEqualTo(NfcScanningContract.Result.Canceled)
        }
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
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle)
            scannerState.emit(NfcCardScanner.State.Scanning)
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Scanning)
        }
    }

    @Test
    fun `card scanner in scanned status updates the view model state to scanned`() = runScenario {
        viewModel.viewState.test {
            assertThat(awaitItem().status).isEqualTo(NfcScanningStatus.Idle)

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
    }

    @Test
    fun `onCleared cancels view model scope`() = runTest(dispatcher) {
        val viewModelScope = CoroutineScope(dispatcher + Job())
        val viewModel = NfcScanningViewModel(
            viewModelScope = viewModelScope,
            tapZoneResolver = FakeTapZoneResolver(),
            cardScanner = FakeNfcCardScanner(),
        )
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
        val viewModel = NfcScanningViewModel(
            viewModelScope = CoroutineScope(dispatcher),
            tapZoneResolver = FakeTapZoneResolver(tapZone),
            cardScanner = fakeCardScanner,
        )

        Scenario(
            viewModel = viewModel,
            fakeCardScanner = fakeCardScanner,
            scannerState = scannerState,
        ).block()

        fakeCardScanner.ensureAllEventsConsumed()
    }

    private class Scenario(
        val viewModel: NfcScanningViewModel,
        val fakeCardScanner: FakeNfcCardScanner,
        val scannerState: MutableSharedFlow<NfcCardScanner.State>,
    )
}
