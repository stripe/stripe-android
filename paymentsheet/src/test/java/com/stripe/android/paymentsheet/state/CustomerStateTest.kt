package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CustomerStateTest {

    private fun createCustomerSessionForTestingDefaultPaymentMethod(
        defaultPaymentMethodId: String?,
    ): CustomerState {
        val paymentMethods = PaymentMethodFactory.cards(3)

        val mobilePaymentElementComponent = createEnabledMobilePaymentElement(
            isPaymentMethodSetAsDefaultEnabled = true,
        )
        val customer = createElementsSessionCustomer(
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = mobilePaymentElementComponent,
            defaultPaymentMethodId = defaultPaymentMethodId
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        return customerState
    }

    @Test
    fun `Create 'CustomerState' for customer session with nonnull default PM`() {
        val defaultPaymentMethodId = "pm_123"

        val customerState = createCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
        )

        assertThat(customerState.defaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
    }

    @Test
    fun `Create 'CustomerState' for customer session with null default PM`() {
        val customerState = createCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = null,
        )

        assertThat(customerState.defaultPaymentMethodId).isNull()
    }

    @Test
    fun `Should create 'CustomerState' for legacy ephemeral keys properly`() {
        val paymentMethods = PaymentMethodFactory.cards(7)

        val customerState = createLegacyEphemeralKeyCustomerState(
            paymentMethods = paymentMethods,
        )

        assertThat(customerState.paymentMethods).isEqualTo(paymentMethods)
        assertThat(customerState.customerSessionClientSecret).isNull()
        assertThat(customerState.defaultPaymentMethodId).isNull()
        assertThat(customerState.id).isEqualTo("cus_1")
        assertThat(customerState.ephemeralKeySecret).isEqualTo("ek_1")
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
                supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
                customerSessionClientSecret = "cuss_123",
            )

            assertThat(customerState.paymentMethods).containsExactlyElementsIn(cards)
        }

    private fun createEnabledMobilePaymentElement(
        isPaymentMethodSaveEnabled: Boolean = true,
        isPaymentMethodRemoveEnabled: Boolean = false,
        canRemoveLastPaymentMethod: Boolean = false,
        allowRedisplayOverride: PaymentMethod.AllowRedisplay? = null,
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
    ): ElementsSession.Customer.Components.MobilePaymentElement {
        return ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
            isPaymentMethodSaveEnabled = isPaymentMethodSaveEnabled,
            isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
            canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
            allowRedisplayOverride = allowRedisplayOverride,
            isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
        )
    }

    private fun createLegacyEphemeralKeyCustomerState(
        paymentMethods: List<PaymentMethod> = emptyList()
    ): CustomerState {
        return CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_1",
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey(
                ephemeralKeySecret = "ek_1",
            ),
            paymentMethods = paymentMethods,
        )
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
}
