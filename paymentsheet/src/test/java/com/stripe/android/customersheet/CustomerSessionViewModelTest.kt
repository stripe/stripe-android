package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSessionViewModelTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val viewModel = CustomerSessionViewModel(application)

    @Before
    fun setup() {
        PaymentConfiguration.clearInstance()
        CustomerSessionViewModel.clear()
    }

    @Test
    fun `createCustomerSessionComponent creates a new session`() {
        PaymentConfiguration.init(
            context = application,
            publishableKey = "ek_123",
        )
        val component1 = viewModel.createCustomerSessionComponent(
            configuration = CustomerSheet.Configuration(merchantDisplayName = "Example"),
            customerAdapter = CustomerAdapter.create(
                context = application,
                customerEphemeralKeyProvider = {
                    CustomerAdapter.Result.success(
                        CustomerEphemeralKey(
                            "cus_123",
                            "ek_123",
                        )
                    )
                },
                setupIntentClientSecretProvider = null,
            ),
            callback = { },
            statusBarColor = { null },
        )

        val component2 = viewModel.createCustomerSessionComponent(
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true
            ),
            customerAdapter = CustomerAdapter.create(
                context = application,
                customerEphemeralKeyProvider = {
                    CustomerAdapter.Result.success(
                        CustomerEphemeralKey(
                            "cus_124",
                            "ek_124",
                        )
                    )
                },
                setupIntentClientSecretProvider = null,
            ),
            callback = { },
            statusBarColor = { null },
        )

        assertThat(component1).isNotEqualTo(component2)
    }

    @Test
    fun `createCustomerSessionComponent creates a new session if configuration changes`() {
        val customerAdapter = mock<CustomerAdapter>()
        val callback = mock<CustomerSheetResultCallback>()
        val component1 = viewModel.createCustomerSessionComponent(
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = false,
            ),
            customerAdapter = customerAdapter,
            callback = callback,
            statusBarColor = { null },
        )

        val component2 = viewModel.createCustomerSessionComponent(
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true,
            ),
            customerAdapter = customerAdapter,
            callback = callback,
            statusBarColor = { null },
        )

        assertThat(component1).isNotEqualTo(component2)
    }

    @Test
    fun `createCustomerSessionComponent returns the same session`() {
        val configuration = mock<CustomerSheet.Configuration>()
        val customerAdapter = mock<CustomerAdapter>()
        val callback = mock<CustomerSheetResultCallback>()

        val component1 = viewModel.createCustomerSessionComponent(
            configuration = configuration,
            customerAdapter = customerAdapter,
            callback = callback,
            statusBarColor = { null },
        )

        val component2 = viewModel.createCustomerSessionComponent(
            configuration = configuration,
            customerAdapter = customerAdapter,
            callback = callback,
            statusBarColor = { null },
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
        val component = viewModel.createCustomerSessionComponent(
            configuration = mock(),
            customerAdapter = mock(),
            callback = mock(),
            statusBarColor = { null },
        )

        assertThat(component).isEqualTo(CustomerSessionViewModel.component)
    }

    @Test
    fun `CustomerSessionViewModel clear clears the session`() {
        val component = viewModel.createCustomerSessionComponent(
            configuration = mock(),
            customerAdapter = mock(),
            callback = mock(),
            statusBarColor = { null },
        )

        assertThat(component).isEqualTo(CustomerSessionViewModel.component)

        CustomerSessionViewModel.clear()

        assertFailsWith<IllegalStateException> {
            CustomerSessionViewModel.component
        }
    }
}
