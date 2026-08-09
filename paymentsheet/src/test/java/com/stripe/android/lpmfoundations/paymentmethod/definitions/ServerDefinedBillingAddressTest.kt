package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextElement
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test

/**
 * Production coverage for billing-address resolution in server-defined payment method forms.
 *
 * OXXO exercises the no-mandate path, while AU BECS exercises ordering before a real mandate.
 */
class ServerDefinedBillingAddressTest {
    @Test
    fun `OXXO appends the tax address when no mandate exists`() {
        val formElements = OxxoDefinition.formElements(
            metadata = createMetadata(
                paymentMethodType = PaymentMethod.Type.Oxxo,
                automaticTaxEnabled = true,
            ),
        )

        val billingAddressElements = formElements.billingAddressElements()

        assertThat(billingAddressElements).hasSize(1)
        assertThat((formElements.last() as SectionElement).fields.single())
            .isInstanceOf(BillingAddressElement::class.java)
    }

    @Test
    fun `OXXO remains address-free when automatic tax is disabled`() {
        val formElements = OxxoDefinition.formElements(
            metadata = createMetadata(
                paymentMethodType = PaymentMethod.Type.Oxxo,
                automaticTaxEnabled = false,
            ),
        )

        assertThat(formElements.billingAddressElements()).isEmpty()
    }

    @Test
    fun `AU BECS inserts the tax address immediately before its real mandate`() {
        val formElements = AuBecsDebitDefinition.formElements(
            metadata = createMetadata(
                paymentMethodType = PaymentMethod.Type.AuBecsDebit,
                automaticTaxEnabled = true,
            ),
        )

        val billingAddressElements = formElements.billingAddressElements()

        assertThat(billingAddressElements).hasSize(1)
        val billingAddressSectionIndex = formElements.indexOfFirst { element ->
            element is SectionElement && billingAddressElements.single() in element.fields
        }
        assertThat(billingAddressSectionIndex).isAtLeast(0)
        assertThat(formElements[billingAddressSectionIndex + 1])
            .isInstanceOf(AuBecsDebitMandateTextElement::class.java)
    }

    private fun createMetadata(
        paymentMethodType: PaymentMethod.Type,
        automaticTaxEnabled: Boolean,
    ) = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf(paymentMethodType.code),
        ),
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        ),
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = automaticTaxEnabled,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        ),
    )

    private fun List<Any>.billingAddressElements(): List<BillingAddressElement> {
        return filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()
    }
}
