package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import kotlin.test.assertFailsWith

class CustomerStateTest {
    @Test
    fun `Should create 'CustomerState' for customer session properly with permissions disabled`() {
        val paymentMethods = PaymentMethodFactory.cards(4)
        val elementsSession = createElementsSession(
            customerId = "cus_1",
            ephemeralKeySecret = "ek_1",
            paymentMethods = paymentMethods,
            paymentSheetComponent = ElementsSession.Customer.Components.PaymentSheet.Disabled
        )

        val customerState = CustomerState.createForCustomerSession(elementsSession)

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_1",
                ephemeralKeySecret = "ek_1",
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = false,
                    // Always true for `customer_session`
                    canRemoveDuplicates = true,
                ),
            )
        )
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions enabled`() {
        val paymentMethods = PaymentMethodFactory.cards(4)
        val elementsSession = createElementsSession(
            customerId = "cus_1",
            ephemeralKeySecret = "ek_1",
            paymentMethods = paymentMethods,
            paymentSheetComponent = ElementsSession.Customer.Components.PaymentSheet.Enabled(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = true,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(elementsSession)

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_1",
                ephemeralKeySecret = "ek_1",
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = true,
                    // Always true for `customer_session`
                    canRemoveDuplicates = true,
                ),
            )
        )
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions disabled`() {
        val paymentMethods = PaymentMethodFactory.cards(3)
        val elementsSession = createElementsSession(
            customerId = "cus_3",
            ephemeralKeySecret = "ek_3",
            paymentMethods = paymentMethods,
            paymentSheetComponent = ElementsSession.Customer.Components.PaymentSheet.Enabled(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = false,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(elementsSession)

        assertThat(customerState).isEqualTo(
            CustomerState(
                id = "cus_3",
                ephemeralKeySecret = "ek_3",
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = false,
                    // Always true for `customer_session`
                    canRemoveDuplicates = true,
                ),
            )
        )
    }

    @Test
    fun `Should throw 'IllegalStateException' for customer session if 'customer' field is null`() {
        val elementsSession = ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customer = null,
            linkSettings = null,
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            isEligibleForCardBrandChoice = false,
            isGooglePayEnabled = false,
            merchantCountry = null,
        )

        val exception = assertFailsWith<IllegalStateException> {
            CustomerState.createForCustomerSession(elementsSession)
        }

        assertThat(exception.message)
            .isEqualTo("Excepted 'customer' attribute as part of 'elements_session' response!")
    }

    @Test
    fun `Should create 'CustomerState' for legacy ephemeral keys properly`() {
        val paymentMethods = PaymentMethodFactory.cards(7)
        val customerState = CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_1",
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey(
                ephemeralKeySecret = "ek_1",
            ),
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
                    // Always 'false' for legacy ephemeral keys
                    canRemoveDuplicates = false,
                ),
            )
        )
    }

    private fun createElementsSession(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethods: List<PaymentMethod>,
        paymentSheetComponent: ElementsSession.Customer.Components.PaymentSheet
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customer = ElementsSession.Customer(
                paymentMethods = paymentMethods,
                defaultPaymentMethod = null,
                session = ElementsSession.Customer.Session(
                    id = "cuss_1",
                    customerId = customerId,
                    apiKey = ephemeralKeySecret,
                    apiKeyExpiry = 999999999,
                    liveMode = false,
                    components = ElementsSession.Customer.Components(
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                        paymentSheet = paymentSheetComponent
                    )
                ),
            ),
            linkSettings = null,
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            isEligibleForCardBrandChoice = false,
            isGooglePayEnabled = false,
            merchantCountry = null
        )
    }
}
