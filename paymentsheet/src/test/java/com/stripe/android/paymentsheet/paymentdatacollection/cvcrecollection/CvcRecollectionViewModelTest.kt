package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any

class CvcRecollectionViewModelTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CvcRecollectionViewModel {
        return CvcRecollectionViewModel(
            args = Args(
                lastFour = "4242",
                cardBrand = CardBrand.Visa,
                cvc = null,
                isTestMode = false
            )
        )
    }

    @Test
    fun `view model state initialized properly on init`() {
        val viewModel = createViewModel()

        assertThat(viewModel.viewState.value.cardBrand).isEqualTo(CardBrand.Visa)
        assertThat(viewModel.viewState.value.lastFour).isEqualTo("4242")
        assertThat(viewModel.viewState.value.cvc).isEqualTo(null)
        assertThat(viewModel.viewState.value.isTestMode).isEqualTo(false)
        assertThat(viewModel.viewState.value.element.controller.fieldState.value)
            .isEqualTo(TextFieldStateConstants.Error.Blank)
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
