package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.coroutineContext

class CustomerSessionInitializationDataSourceTest {
    @Test
    fun `on load, should return expected successful state`() = runTest {
        val paymentMethods = PaymentMethodFactory.cards(size = 6)
        val intent = SetupIntentFactory.create()

        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                intent = intent,
                paymentMethods = paymentMethods,
                customerSheetComponent = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                    isPaymentMethodRemoveEnabled = true,
                )
            ),
            savedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(
                savedSelection = CustomerSheetDataResult.success(
                    SavedSelection.PaymentMethod(id = "pm_1")
                )
            )
        )

        val result = dataSource.loadCustomerSheetSession()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<CustomerSheetSession>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.elementsSession.stripeIntent).isEqualTo(intent)
        assertThat(customerSheetSession.savedSelection).isEqualTo(SavedSelection.PaymentMethod(id = "pm_1"))
        assertThat(customerSheetSession.paymentMethods).isEqualTo(paymentMethods)
        assertThat(customerSheetSession.paymentMethodSaveConsentBehavior).isEqualTo(
            PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
            )
        )
        assertThat(customerSheetSession.permissions.canRemovePaymentMethods).isTrue()
    }

    @Test
    fun `on load, should have no remove permissions if component has no remove permissions`() = runTest {
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                customerSheetComponent = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                    isPaymentMethodRemoveEnabled = false,
                ),
            ),
        )

        val result = dataSource.loadCustomerSheetSession()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<CustomerSheetSession>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.permissions.canRemovePaymentMethods).isFalse()
    }

    @Test
    fun `on load, should have no remove permissions if component is disabled`() = runTest {
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                customerSheetComponent = ElementsSession.Customer.Components.CustomerSheet.Disabled,
            ),
        )

        val result = dataSource.loadCustomerSheetSession()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<CustomerSheetSession>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.permissions.canRemovePaymentMethods).isFalse()
    }

    @Test
    fun `on load, should fail if we cannot retrieve elements session`() = runTest {
        val exception = IllegalStateException("Failed to load!")
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                elementsSession = Result.failure(exception)
            ),
        )

        val result = dataSource.loadCustomerSheetSession()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<CustomerSheetSession>>()

        val returnedCause = result.asFailure().cause

        assertThat(returnedCause).isEqualTo(exception)
    }

    @Test
    fun `on load, should fail if we cannot retrieve saved selection`() = runTest {
        val exception = IllegalStateException("Failed to retrieve!")
        val dataSource = createInitializationDataSource(
            savedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(
                savedSelection = CustomerSheetDataResult.failure(
                    cause = exception,
                    displayMessage = null,
                )
            )
        )

        val result = dataSource.loadCustomerSheetSession()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<CustomerSheetSession>>()

        val returnedCause = result.asFailure().cause

        assertThat(returnedCause).isEqualTo(exception)
    }

    private suspend fun createInitializationDataSource(
        elementsSessionManager: CustomerSessionElementsSessionManager = FakeCustomerSessionElementsSessionManager(),
        savedSelectionDataSource: CustomerSheetSavedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(),
    ): CustomerSheetInitializationDataSource {
        return CustomerSessionInitializationDataSource(
            elementsSessionManager = elementsSessionManager,
            savedSelectionDataSource = savedSelectionDataSource,
            workContext = coroutineContext,
        )
    }
}
