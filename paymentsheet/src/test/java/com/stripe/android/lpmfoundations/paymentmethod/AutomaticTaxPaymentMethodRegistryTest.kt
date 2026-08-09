package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutomaticTaxPaymentMethodRegistryTest {
    @Test
    fun `direct registry definitions have one billing country controller for automatic tax`() {
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
            val uiDefinitionFactory = definition.uiDefinitionFactory(metadata)
            if (uiDefinitionFactory !is UiDefinitionFactory.Simple) {
                return@forEach
            }
            val formElements = requireNotNull(
                uiDefinitionFactory.formElements(
                    definition = definition,
                    metadata = metadata,
                    sharedDataSpecs = metadata.sharedDataSpecs,
                    arguments = argumentsFactory.create(
                        metadata = metadata,
                        requiresMandate = definition.requiresMandate(metadata),
                    ),
                ),
            ) { "No form elements for ${definition.type.code}" }
            val addressControllerCount = formElements.addressFields().size +
                formElements.standaloneCountries().size

            assertWithMessage(definition.type.code)
                .that(addressControllerCount)
                .isEqualTo(1)
        }
    }

    private fun List<FormElement>.addressFields(): List<AddressFieldsElement> {
        return filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<AddressFieldsElement>()
    }

    private fun List<FormElement>.standaloneCountries(): List<CountryElement> {
        return filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<CountryElement>()
    }
}
