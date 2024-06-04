package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
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
                payer = resolvableString(R.string.stripe_paymentsheet_bacs_notice_default_payer),
                debitGuaranteeAsHtml = resolvableString(
                    R.string.stripe_paymentsheet_bacs_guarantee_format,
                    resolvableString(R.string.stripe_paymentsheet_bacs_guarantee_url),
                    resolvableString(R.string.stripe_paymentsheet_bacs_guarantee)
                ),
                supportAddressAsHtml = resolvableString(
                    R.string.stripe_paymentsheet_bacs_support_address_format,
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_one),
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_two),
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email),
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email)
                ),
            )
        )
    }

    @Test
    fun `on confirm pressed, should emit confirmed result`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnConfirmPressed)

            assertThat(awaitItem()).isEqualTo(BacsMandateConfirmationResult.Confirmed)
        }
    }

    @Test
    fun `on cancel pressed, should emit cancelled result`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnBackPressed)

            assertThat(awaitItem()).isEqualTo(BacsMandateConfirmationResult.Cancelled)
        }
    }

    private fun createViewModel(): BacsMandateConfirmationViewModel {
        return BacsMandateConfirmationViewModel(
            args = BacsMandateConfirmationViewModel.Args(
                accountNumber = "00012345",
                sortCode = "108800",
                email = "email@email.com",
                nameOnAccount = "John Doe"
            )
        )
    }
}
