package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSessionViewModelTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `createCustomerSessionComponent creates a new session`() {
        val viewModel = CustomerSessionViewModel(application)

        val component1 = viewModel.createCustomerSessionComponent(
            configuration = mock(),
            customerAdapter = mock(),
            callback = mock(),
        )

        val component2 = viewModel.createCustomerSessionComponent(
            configuration = mock(),
            customerAdapter = mock(),
            callback = mock(),
        )

        assertThat(component1).isNotEqualTo(component2)
    }

    @Test
    fun `createCustomerSessionComponent returns the same session`() {
        val viewModel = CustomerSessionViewModel(application)

        val configuration = mock<CustomerSheet.Configuration>()
        val customerAdapter = mock<CustomerAdapter>()
        val callback = mock<CustomerSheetResultCallback>()

        val component1 = viewModel.createCustomerSessionComponent(
            configuration = configuration,
            customerAdapter = customerAdapter,
            callback = callback,
        )

        val component2 = viewModel.createCustomerSessionComponent(
            configuration = configuration,
            customerAdapter = customerAdapter,
            callback = callback,
        )

        assertThat(component1).isEqualTo(component2)
    }

    @Test
    fun `CustomerSessionViewModel component throws if not initialized`() {
        assertFailsWith<IllegalStateException> {
            CustomerSessionViewModel.component
        }
    }

    @Test
    fun `CustomerSessionViewModel component does not throw if initialized`() {
        val viewModel = CustomerSessionViewModel(application)

        val component = viewModel.createCustomerSessionComponent(
            configuration = mock(),
            customerAdapter = mock(),
            callback = mock(),
        )

        assertThat(component).isEqualTo(CustomerSessionViewModel.component)
    }
}
