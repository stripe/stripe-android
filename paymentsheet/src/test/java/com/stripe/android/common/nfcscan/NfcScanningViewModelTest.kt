package com.stripe.android.common.nfcscan

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.tapzone.FakeTapZoneResolver
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class NfcScanningViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `viewState contains tap zone from resolver`() = runScenario(
        tapZone = TapZone(xBias = 0.3f, yBias = 0.7f),
    ) {
        assertThat(viewModel.viewState.value).isEqualTo(
            NfcScanningViewState(tapZone = TapZone(xBias = 0.3f, yBias = 0.7f)),
        )
    }

    @Test
    fun `handleViewAction Close emits Canceled result`() = runScenario {
        viewModel.result.test {
            viewModel.handleViewAction(NfcScanningViewAction.Close)

            assertThat(awaitItem()).isEqualTo(NfcScanningContract.Result.Canceled)
        }
    }

    private fun runScenario(
        tapZone: TapZone = TapZone(xBias = 0.5f, yBias = 0.5f),
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val viewModel = NfcScanningViewModel(
            tapZoneResolver = FakeTapZoneResolver(tapZone),
        )

        Scenario(viewModel = viewModel).block()
    }

    private class Scenario(
        val viewModel: NfcScanningViewModel,
    )
}
