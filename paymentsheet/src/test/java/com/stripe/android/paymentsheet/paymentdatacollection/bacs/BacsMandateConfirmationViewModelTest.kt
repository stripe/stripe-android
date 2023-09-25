package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class BacsMandateConfirmationViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `on init, view model state should be initialized properly`() {
        val viewModel = createViewModel()

        assertThat(viewModel.viewState.value).isEqualTo(
            BacsMandateConfirmationViewState(
                accountNumber = "00012345",
                sortCode = "10-88-00",
                email = "email@email.com",
                nameOnAccount = "John Doe",
                debitGuaranteeAsHtml = "<a href=\"https://stripe.com/legal/bacs-direct-debit-guarantee\">" +
                    "Direct Debit Guarantee</a>",
                supportAddressAsHtml = "Stripe, 7th Floor The Bower Warehouse" +
                    "<br>207–211 Old St, London EC1V 9NR" +
                    "<br><a href=\"mailto:support@stripe.com\">support@stripe.com</a>"
            )
        )
    }

    @Test
    fun `on confirm pressed, should emit confirmed result`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnConfirmPressed)

            assertThat(awaitItem()).isEqualTo( BacsMandateConfirmationResult.Confirmed)
        }
    }

    @Test
    fun `on cancel pressed, should emit cancelled result`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnCancelPressed)

            assertThat(awaitItem()).isEqualTo(BacsMandateConfirmationResult.Cancelled)
        }
    }

    private fun createViewModel(): BacsMandateConfirmationViewModel {
        return BacsMandateConfirmationViewModel(
            args = BacsMandateConfirmationViewModel.Args(
                accountNumber = "00012345",
                sortCode = "108800",
                email = "email@email.com",
                nameOnAccount = "John Doe",
                guarantee = BacsMandateConfirmationViewModel.Args.Guarantee(
                    name = "Direct Debit Guarantee",
                    url = "https://stripe.com/legal/bacs-direct-debit-guarantee"
                ),
                defaultAddress = BacsMandateConfirmationViewModel.Args.DefaultAddress(
                    lineOne = "Stripe, 7th Floor The Bower Warehouse",
                    lineTwo = "207–211 Old St, London EC1V 9NR",
                    supportEmail = "support@stripe.com"
                ),
                defaultPayer = "Stripe"
            )
        )
    }
}
