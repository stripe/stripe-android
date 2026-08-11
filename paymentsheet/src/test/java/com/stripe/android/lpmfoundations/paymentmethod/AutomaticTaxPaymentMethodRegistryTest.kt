package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AutomaticTaxPaymentMethodRegistryTest {
    @Test
    fun `locally authored registry forms have one billing address owner for automatic tax`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentMethodRegistry.all.map { it.type.code },
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            ),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
        )
        val argumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create()

        PaymentMethodRegistry.all.forEach { definition ->
            val factory = definition.uiDefinitionFactory(metadata)
            if (factory !is UiDefinitionFactory.Simple) {
                return@forEach
            }
            val formElements = factory.createFormElements(
                metadata = metadata,
                arguments = argumentsFactory.create(
                    metadata = metadata,
                    requiresMandate = definition.requiresMandate(metadata),
                ),
            )

            assertWithMessage(definition.type.code)
                .that(formElements.billingAddressElements())
                .hasSize(1)
            assertWithMessage(definition.type.code)
                .that(formElements.standaloneCountryElements())
                .isEmpty()
        }
    }

    private fun List<FormElement>.billingAddressElements(): List<BillingAddressElement> {
        return filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()
    }

    private fun List<FormElement>.standaloneCountryElements(): List<CountryElement> {
        return filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<CountryElement>()
    }
}
