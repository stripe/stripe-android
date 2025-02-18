package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetFixtures
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
                customerSheetComponent = createEnabledCustomerSheetComponent(
                    isPaymentMethodRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                    isPaymentMethodSyncDefaultEnabled = false,
                ),
            ),
            savedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(
                savedSelection = CustomerSheetDataResult.success(
                    SavedSelection.PaymentMethod(id = "pm_1")
                )
            )
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

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
        assertThat(customerSheetSession.permissions.canRemoveLastPaymentMethod).isTrue()
        assertThat(customerSheetSession.defaultPaymentMethodId).isNull()
    }

    @Test
    fun `on load, should have no remove permissions if component has no remove permissions`() = runTest {
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                customerSheetComponent = createEnabledCustomerSheetComponent(
                    isPaymentMethodRemoveEnabled = false,
                    canRemoveLastPaymentMethod = false,
                ),
            ),
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<CustomerSheetSession>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.permissions.canRemovePaymentMethods).isFalse()
        assertThat(customerSheetSession.permissions.canRemoveLastPaymentMethod).isFalse()
    }

    @Test
    fun `on load, should have no remove permissions if component is disabled`() = runTest {
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                customerSheetComponent = ElementsSession.Customer.Components.CustomerSheet.Disabled,
            ),
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<CustomerSheetSession>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.permissions.canRemovePaymentMethods).isFalse()
    }

    @Test
    fun `on load, should use customer default PM if default PM feature is enabled`() = runTest {
        val expectedDefaultPaymentMethodId = "pm_123"
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                defaultPaymentMethodId = expectedDefaultPaymentMethodId,
                customerSheetComponent = createEnabledCustomerSheetComponent(
                    isPaymentMethodSyncDefaultEnabled = true,
                )
            ),
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<CustomerSheetSession>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.defaultPaymentMethodId).isEqualTo(expectedDefaultPaymentMethodId)
    }

    @Test
    fun `on load, should fail if we cannot retrieve elements session`() = runTest {
        val exception = IllegalStateException("Failed to load!")
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                elementsSession = Result.failure(exception)
            ),
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

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

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<CustomerSheetSession>>()

        val returnedCause = result.asFailure().cause

        assertThat(returnedCause).isEqualTo(exception)
    }

    @Test
    fun `When config value is false & server value is false, remove last PM permissions should be false`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = false,
            canRemoveLastPaymentMethod = false,
            customerSheetComponentIsDisabled = false,
            expected = false,
        )

    @Test
    fun `When config value is false & server component is disabled, remove last PM permissions should be false`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = false,
            canRemoveLastPaymentMethod = false,
            customerSheetComponentIsDisabled = true,
            expected = false,
        )

    @Test
    fun `When config value is false & server value is true, remove last PM permissions should be false`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = false,
            canRemoveLastPaymentMethod = true,
            customerSheetComponentIsDisabled = false,
            expected = false,
        )

    @Test
    fun `When config value is true & server value is false, remove last PM permissions should be false`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = true,
            canRemoveLastPaymentMethod = false,
            customerSheetComponentIsDisabled = false,
            expected = false,
        )

    @Test
    fun `When config value is true & server component is disabled, remove last PM permissions should be false`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = true,
            canRemoveLastPaymentMethod = false,
            customerSheetComponentIsDisabled = true,
            expected = false,
        )

    @Test
    fun `When config value is true & server value is true, remove last PM permissions should be true`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = true,
            canRemoveLastPaymentMethod = true,
            customerSheetComponentIsDisabled = false,
            expected = true,
        )

    private fun runRemoveLastPaymentMethodPermissionsTest(
        allowsRemovalOfLastSavedPaymentMethod: Boolean,
        customerSheetComponentIsDisabled: Boolean,
        canRemoveLastPaymentMethod: Boolean,
        expected: Boolean,
    ) = runTest {
        val dataSource = createInitializationDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                customerSheetComponent = if (customerSheetComponentIsDisabled) {
                    ElementsSession.Customer.Components.CustomerSheet.Disabled
                } else {
                    createEnabledCustomerSheetComponent(
                        isPaymentMethodRemoveEnabled = true,
                        canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                    )
                },
            ),
        )

        val result = dataSource.loadCustomerSheetSession(
            configuration = createConfiguration(
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
            )
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<*>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.permissions.canRemoveLastPaymentMethod).isEqualTo(expected)
    }

    @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
    private fun createConfiguration(
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
    ): CustomerSheet.Configuration {
        return CustomerSheetFixtures.CONFIG_WITH_EVERYTHING.newBuilder()
            .allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod)
            .build()
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

    private fun createEnabledCustomerSheetComponent(
        isPaymentMethodRemoveEnabled: Boolean = true,
        canRemoveLastPaymentMethod: Boolean = true,
        isPaymentMethodSyncDefaultEnabled: Boolean = false,
    ): ElementsSession.Customer.Components.CustomerSheet.Enabled {
        return ElementsSession.Customer.Components.CustomerSheet.Enabled(
            isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
            canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
            isPaymentMethodSyncDefaultEnabled = isPaymentMethodSyncDefaultEnabled,
        )
    }
}
