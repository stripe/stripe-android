package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class CvcRecollectionViewModelTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(cvc: String? = null): CvcRecollectionViewModel {
        return CvcRecollectionViewModel(
            args = Args(
                lastFour = "4242",
                cardBrand = CardBrand.Visa,
                cvc = cvc,
                isTestMode = false
            )
        )
    }

    @Test
    fun `view model state initialized properly on init`() {
        val viewModel = createViewModel()

        assertThat(viewModel.viewState.value.cvcState).isEqualTo(
            CvcState(
                cvc = "",
                cardBrand = CardBrand.Visa
            )
        )
        assertThat(viewModel.viewState.value.lastFour).isEqualTo("4242")
        assertThat(viewModel.viewState.value.isTestMode).isEqualTo(false)
    }

    @Test
    fun `on confirm pressed viewModel emits confirmed result`() = runTest {
        val viewModel = createViewModel("555")

        viewModel.result.test {
            viewModel.handleViewAction(CvcRecollectionViewAction.OnConfirmPressed)

            assertThat(awaitItem()).isEqualTo(CvcRecollectionResult.Confirmed("555"))
        }
    }

    @Test
    fun `on back pressed viewModel emits on cancelled result`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleViewAction(CvcRecollectionViewAction.OnBackPressed)
            assertThat(awaitItem()).isEqualTo(CvcRecollectionResult.Cancelled)
        }
    }
}
