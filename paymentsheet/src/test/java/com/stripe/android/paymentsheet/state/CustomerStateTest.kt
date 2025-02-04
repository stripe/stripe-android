package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CustomerStateTest {

    @Test
    fun `Should create 'CustomerState' for customer session properly with permissions disabled`() {
        val paymentMethods = PaymentMethodFactory.cards(4)
        val customer = createElementsSessionCustomer(
            customerId = "cus_1",
            ephemeralKeySecret = "ek_1",
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Disabled
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        assertThat(customerState.permissions.canRemovePaymentMethods).isFalse()
        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        // Always true for `customer_session`
        assertThat(customerState.permissions.canRemoveDuplicates).isTrue()
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions enabled`() {
        val paymentMethods = PaymentMethodFactory.cards(4)
        val customer = createElementsSessionCustomer(
            customerId = "cus_1",
            ephemeralKeySecret = "ek_1",
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = createEnabledMobilePaymentElement(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = true,
                canRemoveLastPaymentMethod = true,
                allowRedisplayOverride = null,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        assertThat(customerState.permissions.canRemovePaymentMethods).isTrue()
        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isTrue()
        // Always true for `customer_session`
        assertThat(customerState.permissions.canRemoveDuplicates).isTrue()
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions disabled`() {
        val paymentMethods = PaymentMethodFactory.cards(3)
        val customer = createElementsSessionCustomer(
            customerId = "cus_3",
            ephemeralKeySecret = "ek_3",
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = createEnabledMobilePaymentElement(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = false,
                canRemoveLastPaymentMethod = false,
                allowRedisplayOverride = null,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        assertThat(customerState.permissions.canRemovePaymentMethods).isFalse()
        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        // Always true for `customer_session`
        assertThat(customerState.permissions.canRemoveDuplicates).isTrue()
    }

    private fun createCsCustomerSessionForTestingDefaultPaymentMethod(
        defaultPaymentMethodId: String?,
        isSyncDefaultEnabled: Boolean,
    ): CustomerState {
        val customerId = "cus_3"
        val ephemeralKeySecret = "ek_3"
        val paymentMethods = PaymentMethodFactory.cards(3)

        val customerSheetComponent = createEnabledCustomerSheetComponent(
            isSyncDefaultEnabled = isSyncDefaultEnabled,
        )
        val customer = createElementsSessionCustomer(
            customerId = customerId,
            ephemeralKeySecret = ephemeralKeySecret,
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
            customerSheetComponent = customerSheetComponent,
            defaultPaymentMethodId = defaultPaymentMethodId
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        return customerState
    }

    private fun createMpeCustomerSessionForTestingDefaultPaymentMethod(
        defaultPaymentMethodId: String?,
        isSetAsDefaultEnabled: Boolean,
    ): CustomerState {
        val customerId = "cus_3"
        val ephemeralKeySecret = "ek_3"
        val paymentMethods = PaymentMethodFactory.cards(3)

        val mobilePaymentElementComponent = createEnabledMobilePaymentElement(
            isSetAsDefaultEnabled = isSetAsDefaultEnabled,
        )
        val customer = createElementsSessionCustomer(
            customerId = customerId,
            ephemeralKeySecret = ephemeralKeySecret,
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = mobilePaymentElementComponent,
            defaultPaymentMethodId = defaultPaymentMethodId
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        return customerState
    }

    @Test
    fun `Create 'CustomerState' for customer session with nonnull defaultPaymentMethodId & set as default feature enabled`() {
        val defaultPaymentMethodId = "pm_123"

        val customerState = createMpeCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSetAsDefaultEnabled = true,
        )

        assertThat(customerState.defaultPaymentMethodState).isInstanceOf(
            CustomerState.DefaultPaymentMethodState.Enabled::class.java
        )
        val actualDefaultPaymentMethodId = (
            customerState.defaultPaymentMethodState as CustomerState.DefaultPaymentMethodState.Enabled
            ).defaultPaymentMethodId
        assertThat(actualDefaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
    }

    @Test
    fun `Create 'CustomerState' for customer session with nonnull default PM & set as default feature disabled`() {
        val defaultPaymentMethodId = "pm_123"

        val customerState = createMpeCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSetAsDefaultEnabled = false,
        )

        assertThat(customerState.defaultPaymentMethodState).isEqualTo(CustomerState.DefaultPaymentMethodState.Disabled)
    }

    @Test
    fun `Create 'CustomerState' for customer session with null default PM & set as default feature enabled`() {
        val defaultPaymentMethodId = null

        val customerState = createMpeCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSetAsDefaultEnabled = true,
        )

        assertThat(customerState.defaultPaymentMethodState).isInstanceOf(
            CustomerState.DefaultPaymentMethodState.Enabled::class.java
        )
        val actualDefaultPaymentMethodId = (
            customerState.defaultPaymentMethodState as CustomerState.DefaultPaymentMethodState.Enabled
            ).defaultPaymentMethodId
        assertThat(actualDefaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
    }

    @Test
    fun `Create 'CustomerState' for customer session with null default PM & set as default feature disabled`() {
        val defaultPaymentMethodId = null

        val customerState = createMpeCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSetAsDefaultEnabled = false,
        )

        assertThat(customerState.defaultPaymentMethodState).isEqualTo(CustomerState.DefaultPaymentMethodState.Disabled)
    }

    @Test
    fun `Create 'CustomerState' for customer session with non-null default PM & sync default enabled`() {
        val defaultPaymentMethodId = "pm_123"

        val customerState = createCsCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSyncDefaultEnabled = true,
        )

        assertThat(customerState.defaultPaymentMethodState).isInstanceOf(
            CustomerState.DefaultPaymentMethodState.Enabled::class.java
        )
        val actualDefaultPaymentMethodId = (
            customerState.defaultPaymentMethodState as CustomerState.DefaultPaymentMethodState.Enabled
            ).defaultPaymentMethodId
        assertThat(actualDefaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
    }

    @Test
    fun `Create 'CustomerState' for customer session with null default PM & sync default enabled`() {
        val defaultPaymentMethodId = null

        val customerState = createCsCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSyncDefaultEnabled = true,
        )

        assertThat(customerState.defaultPaymentMethodState).isInstanceOf(
            CustomerState.DefaultPaymentMethodState.Enabled::class.java
        )
        val actualDefaultPaymentMethodId = (
            customerState.defaultPaymentMethodState as CustomerState.DefaultPaymentMethodState.Enabled
            ).defaultPaymentMethodId
        assertThat(actualDefaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
    }

    @Test
    fun `Create 'CustomerState' for customer session with non-null default PM & sync default disabled`() {
        val defaultPaymentMethodId = "pm_123"

        val customerState = createCsCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSyncDefaultEnabled = false,
        )

        assertThat(customerState.defaultPaymentMethodState).isInstanceOf(
            CustomerState.DefaultPaymentMethodState.Disabled::class.java
        )
    }

    @Test
    fun `Create 'CustomerState' for customer session with null default PM & sync default disabled`() {
        val defaultPaymentMethodId = "pm_123"

        val customerState = createCsCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSyncDefaultEnabled = false,
        )

        assertThat(customerState.defaultPaymentMethodState).isInstanceOf(
            CustomerState.DefaultPaymentMethodState.Disabled::class.java
        )
    }

    @Test
    fun `Should create 'CustomerState' for legacy ephemeral keys properly`() {
        val paymentMethods = PaymentMethodFactory.cards(7)
        val customerState = CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_1",
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey(
                ephemeralKeySecret = "ek_1",
            ),
            configuration = createConfiguration(),
            paymentMethods = paymentMethods,
        )

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_1",
                ephemeralKeySecret = "ek_1",
                customerSessionClientSecret = null,
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    // Always true for legacy ephemeral keys since un-scoped
                    canRemovePaymentMethods = true,
                    // Always true unless configured client-side
                    canRemoveLastPaymentMethod = true,
                    // Always 'false' for legacy ephemeral keys
                    canRemoveDuplicates = false,
                ),
                defaultPaymentMethodState = CustomerState.DefaultPaymentMethodState.Disabled
            )
        )
    }

    @Test
    fun `Should create 'CustomerState' with filtered payment methods`() =
        runTest {
            val cards = PaymentMethodFixtures.createCards(2)

            val customer = createElementsSessionCustomer(
                paymentMethods = cards + listOf(
                    PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                    PaymentMethodFixtures.LINK_PAYMENT_METHOD,
                    PaymentMethodFixtures.AU_BECS_DEBIT,
                ),
                mobilePaymentElementComponent = createEnabledMobilePaymentElement(
                    isPaymentMethodSaveEnabled = false,
                    isPaymentMethodRemoveEnabled = false,
                    canRemoveLastPaymentMethod = false,
                    allowRedisplayOverride = null,
                ),
            )

            val customerState = CustomerState.createForCustomerSession(
                customer = customer,
                configuration = createConfiguration(),
                supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
                customerSessionClientSecret = "cuss_123",
            )

            assertThat(customerState.paymentMethods).containsExactlyElementsIn(cards)
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value is true for legacy ephemeral keys`() {
        val customerState = CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_1",
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey(
                ephemeralKeySecret = "ek_1",
            ),
            configuration = createConfiguration(allowsRemovalOfLastSavedPaymentMethod = true),
            paymentMethods = listOf(),
        )

        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isTrue()
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false for legacy ephemeral keys`() {
        val customerState = CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_1",
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey(
                ephemeralKeySecret = "ek_1",
            ),
            configuration = createConfiguration(allowsRemovalOfLastSavedPaymentMethod = false),
            paymentMethods = listOf(),
        )

        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value & server value are false`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = false,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false & server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = false,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = true,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server value is false`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = false,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false but server value is true`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = true,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value & server value are true`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = true,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isTrue()
        }

    private fun customerSessionPermissionsTest(
        paymentElementDisabled: Boolean = false,
        canRemoveLastPaymentMethodConfigValue: Boolean = true,
        canRemoveLastPaymentMethod: Boolean = true,
        test: (customerState: CustomerState) -> Unit,
    ) {
        val customerState = CustomerState.createForCustomerSession(
            customer = createElementsSessionCustomer(
                mobilePaymentElementComponent = if (paymentElementDisabled) {
                    ElementsSession.Customer.Components.MobilePaymentElement.Disabled
                } else {
                    createEnabledMobilePaymentElement(
                        isPaymentMethodRemoveEnabled = true,
                        isPaymentMethodSaveEnabled = false,
                        canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                        allowRedisplayOverride = null,
                    )
                }
            ),
            configuration = createConfiguration(
                allowsRemovalOfLastSavedPaymentMethod = canRemoveLastPaymentMethodConfigValue
            ),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        test(customerState)
    }

    private fun createEnabledMobilePaymentElement(
        isPaymentMethodSaveEnabled: Boolean = true,
        isPaymentMethodRemoveEnabled: Boolean = false,
        canRemoveLastPaymentMethod: Boolean = false,
        allowRedisplayOverride: PaymentMethod.AllowRedisplay? = null,
        isSetAsDefaultEnabled: Boolean = false,
    ): ElementsSession.Customer.Components.MobilePaymentElement {
        return ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
            isPaymentMethodSaveEnabled = isPaymentMethodSaveEnabled,
            isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
            canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
            allowRedisplayOverride = allowRedisplayOverride,
            isSetAsDefaultEnabled = isSetAsDefaultEnabled,
        )
    }

    private fun createEnabledCustomerSheetComponent(
        isPaymentMethodRemoveEnabled: Boolean = false,
        canRemoveLastPaymentMethod: Boolean = false,
        isSyncDefaultEnabled: Boolean = false,
    ): ElementsSession.Customer.Components.CustomerSheet.Enabled {
        return ElementsSession.Customer.Components.CustomerSheet.Enabled(
            isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
            canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
            isSyncDefaultEnabled = isSyncDefaultEnabled,
        )
    }

    private fun createElementsSessionCustomer(
        customerId: String = "cus_1",
        ephemeralKeySecret: String = "ek_1",
        paymentMethods: List<PaymentMethod> = listOf(),
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement,
        customerSheetComponent: ElementsSession.Customer.Components.CustomerSheet? = null,
        defaultPaymentMethodId: String? = null,
    ): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = paymentMethods,
            defaultPaymentMethod = defaultPaymentMethodId,
            session = ElementsSession.Customer.Session(
                id = "cuss_1",
                customerId = customerId,
                apiKey = ephemeralKeySecret,
                apiKeyExpiry = 999999999,
                liveMode = false,
                components = ElementsSession.Customer.Components(
                    customerSheet = customerSheetComponent
                        ?: ElementsSession.Customer.Components.CustomerSheet.Disabled,
                    mobilePaymentElement = mobilePaymentElementComponent
                )
            ),
        )
    }

    private fun createConfiguration(
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
    ): CommonConfiguration {
        return PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration().copy(
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        )
    }
}
