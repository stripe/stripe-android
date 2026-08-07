package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BacsDebitDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.OxxoDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AddressElement
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
    fun `OXXO adds automatic tax billing address through shared spec path`() {
        val formElements = OxxoDefinition.formElements(
            metadata = automaticTaxMetadata(PaymentMethod.Type.Oxxo),
        )

        assertThat(formElements.addressFields()).hasSize(1)
        assertThat(formElements.addressFields().single()).isInstanceOf(BillingAddressElement::class.java)
    }

    @Test
    fun `Bacs Debit preserves payment method required normal address`() {
        val formElements = BacsDebitDefinition.formElements(
            metadata = automaticTaxMetadata(PaymentMethod.Type.BacsDebit),
        )

        assertThat(formElements.addressFields()).hasSize(1)
        assertThat(formElements.addressFields().single()).isInstanceOf(AddressElement::class.java)
        assertThat(formElements.addressFields().single())
            .isNotInstanceOf(BillingAddressElement::class.java)
    }

    @Test
    fun `normal registry definitions have one billing country controller for automatic tax`() {
        val metadata = automaticTaxMetadata(
            paymentMethodTypes = PaymentMethodRegistry.all.map { it.type.code },
        )
        val argumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create()

        PaymentMethodRegistry.all.forEach { definition ->
            val uiDefinitionFactory = definition.uiDefinitionFactory(metadata)
            if (uiDefinitionFactory is UiDefinitionFactory.Custom) {
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
                )
            ) { "No form elements for ${definition.type.code}" }
            val addressControllerCount = formElements.addressFields().size +
                formElements.standaloneCountries().size

            assertWithMessage(definition.type.code)
                .that(addressControllerCount)
                .isEqualTo(1)
        }
    }

    private fun automaticTaxMetadata(
        paymentMethodType: PaymentMethod.Type,
    ): PaymentMethodMetadata {
        return automaticTaxMetadata(paymentMethodTypes = listOf(paymentMethodType.code))
    }

    private fun automaticTaxMetadata(
        paymentMethodTypes: List<String>,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes,
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            ),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
        )
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
