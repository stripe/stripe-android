package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test

class OxxoDefinitionTest {
    @Test
    fun `formElements adds automatic tax billing address through shared spec path`() {
        val formElements = OxxoDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(PaymentMethod.Type.Oxxo.code),
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
        assertThat((formElements.last() as SectionElement).fields.single())
            .isInstanceOf(BillingAddressElement::class.java)
    }
}
