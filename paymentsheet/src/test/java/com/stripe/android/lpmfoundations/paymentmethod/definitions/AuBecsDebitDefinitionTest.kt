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

class AuBecsDebitDefinitionTest {
    @Test
    fun `formElements adds automatic tax billing address before mandate`() {
        val formElements = AuBecsDebitDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(PaymentMethod.Type.AuBecsDebit.code),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                ),
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    automaticTaxEnabled = true,
                    taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                ),
            ),
        )

        val billingAddressElements = formElements
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()

        assertThat(billingAddressElements).hasSize(1)
        val billingAddressSectionIndex = formElements.indexOfFirst { element ->
            element is SectionElement && billingAddressElements.single() in element.fields
        }
        assertThat(billingAddressSectionIndex).isAtLeast(0)
        assertThat(formElements[billingAddressSectionIndex + 1])
            .isInstanceOf(AuBecsDebitMandateTextElement::class.java)
    }
}
