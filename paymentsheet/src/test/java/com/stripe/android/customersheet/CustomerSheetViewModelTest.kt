package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetViewModelTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = application.resources,
            isFinancialConnectionsAvailable = { true },
            enableACHV2InDeferredFlow = true
        )
    )

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod`() = runTest {
        val viewModel = createViewModel()
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(
                CustomerSheetViewState.SelectPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `CustomerSheetViewAction#OnBackPress emits CustomerSheetAction#NavigateUp`() = runTest {
        val viewModel = createViewModel()
        viewModel.action.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPress)
            assertThat(awaitItem()).isEqualTo(CustomerSheetAction.NavigateUp)
        }
    }

    private fun createViewModel(
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(),
        lpmRepository: LpmRepository = this.lpmRepository
    ): CustomerSheetViewModel {
        return CustomerSheetViewModel(
            resources = application.resources,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Testing"
            )
        )
    }
}
