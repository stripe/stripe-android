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
    private val interactor = FakeCvcRecollectionInteractor(
        initialState = CvcRecollectionViewState(
            cardBrand = CardBrand.Visa,
            lastFour = "4242",
            cvc = null,
            isTestMode = true
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CvcRecollectionViewModel {
        return CvcRecollectionViewModel(interactor)
    }

    @Test
    fun `view model state initialized properly on init`() {
        val viewModel = createViewModel()

        assertThat(viewModel.viewState.value).isEqualTo(
            CvcRecollectionViewState(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                cvc = null,
                isTestMode = true
            )
        )
    }

    @Test
    fun `on confirm pressed viewModel emits confirmed result`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleViewAction(CvcRecollectionViewAction.OnConfirmPressed("555"))

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
