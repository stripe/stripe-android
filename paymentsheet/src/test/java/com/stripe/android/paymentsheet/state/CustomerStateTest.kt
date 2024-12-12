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
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card)
        )

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_1",
                ephemeralKeySecret = "ek_1",
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = false,
                    canRemoveLastPaymentMethod = false,
                    // Always true for `customer_session`
                    canRemoveDuplicates = true,
                ),
                defaultPaymentMethodId = null
            )
        )
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions enabled`() {
        val paymentMethods = PaymentMethodFactory.cards(4)
        val customer = createElementsSessionCustomer(
            customerId = "cus_1",
            ephemeralKeySecret = "ek_1",
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = true,
                canRemoveLastPaymentMethod = true,
                allowRedisplayOverride = null,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card)
        )

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_1",
                ephemeralKeySecret = "ek_1",
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = true,
                    canRemoveLastPaymentMethod = true,
                    // Always true for `customer_session`
                    canRemoveDuplicates = true,
                ),
                defaultPaymentMethodId = null
            )
        )
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions disabled`() {
        val paymentMethods = PaymentMethodFactory.cards(3)
        val customer = createElementsSessionCustomer(
            customerId = "cus_3",
            ephemeralKeySecret = "ek_3",
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = false,
                canRemoveLastPaymentMethod = false,
                allowRedisplayOverride = null,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card)
        )

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_3",
                ephemeralKeySecret = "ek_3",
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = false,
                    canRemoveLastPaymentMethod = false,
                    // Always true for `customer_session`
                    canRemoveDuplicates = true,
                ),
                defaultPaymentMethodId = null
            )
        )
    }


    @Test
    fun `Should create 'CustomerState' for customer session properly with nonnull defaultPaymentMethodId`() {
        val paymentMethods = PaymentMethodFactory.cards(3)
        val customer = createElementsSessionCustomer(
            customerId = "cus_3",
            ephemeralKeySecret = "ek_3",
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = false,
                canRemoveLastPaymentMethod = false,
                allowRedisplayOverride = null,
            ),
            defaultPaymentMethodId = "aaa111"
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card)
        )

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_3",
                ephemeralKeySecret = "ek_3",
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = false,
                    canRemoveLastPaymentMethod = false,
                    // Always true for `customer_session`
                    canRemoveDuplicates = true,
                ),
                defaultPaymentMethodId = "aaa111"
            )
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
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    // Always true for legacy ephemeral keys since un-scoped
                    canRemovePaymentMethods = true,
                    // Always true unless configured client-side
                    canRemoveLastPaymentMethod = true,
                    // Always 'false' for legacy ephemeral keys
                    canRemoveDuplicates = false,
                ),
                defaultPaymentMethodId = null
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
                mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                    isPaymentMethodSaveEnabled = false,
                    isPaymentMethodRemoveEnabled = false,
                    canRemoveLastPaymentMethod = false,
                    allowRedisplayOverride = null,
                ),
            )

            val customerState = CustomerState.createForCustomerSession(
                customer = customer,
                configuration = createConfiguration(),
                supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card)
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
                    ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
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
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card)
        )

        test(customerState)
    }

    private fun createElementsSessionCustomer(
        customerId: String = "cus_1",
        ephemeralKeySecret: String = "ek_1",
        paymentMethods: List<PaymentMethod> = listOf(),
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement,
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
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
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
