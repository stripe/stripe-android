package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AutomaticTaxPaymentMethodRegistryTest {
    @Test
    fun `built-in direct definitions have one billing country owner for automatic tax`() {
        val metadata = automaticTaxMetadata(
            paymentMethodTypes = PaymentMethodRegistry.all.map { it.type.code },
        )
        val argumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create()
        val directDefinitions = PaymentMethodRegistry.all.filter { definition ->
            definition.uiDefinitionFactory(metadata) is UiDefinitionFactory.Simple
        }

        directDefinitions.forEach { definition ->
            val formElements = requireNotNull(
                metadata.formElementsForCode(
                    code = definition.type.code,
                    uiDefinitionFactoryArgumentsFactory = argumentsFactory,
                )
            ) { "No production form elements for ${definition.type.code}" }

            assertWithMessage(definition.type.code)
                .that(formElements.billingCountryOwnerCount())
                .isEqualTo(1)
        }
    }

    @Test
    fun `custom payment methods do not add automatic tax billing fields`() {
        val customPaymentMethod = PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD
        val metadata = automaticTaxMetadata(
            paymentMethodTypes = emptyList(),
            displayableCustomPaymentMethods = listOf(customPaymentMethod),
        )

        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = customPaymentMethod.id,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
            )
        )

        assertThat(formElements.billingCountryOwnerCount()).isEqualTo(0)
    }

    @Test
    fun `external payment methods do not add automatic tax billing fields`() {
        val externalPaymentMethod = PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
        val metadata = automaticTaxMetadata(
            paymentMethodTypes = emptyList(),
            externalPaymentMethodSpecs = listOf(externalPaymentMethod),
        )

        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = externalPaymentMethod.type,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
            )
        )

        assertThat(formElements.billingCountryOwnerCount()).isEqualTo(0)
    }

    private fun automaticTaxMetadata(
        paymentMethodTypes: List<String>,
        externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec> = emptyList(),
        displayableCustomPaymentMethods: List<DisplayableCustomPaymentMethod> = emptyList(),
    ): PaymentMethodMetadata {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes,
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            displayableCustomPaymentMethods = displayableCustomPaymentMethods,
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = checkoutSessionResponse.id,
                instancesKey = "key",
                checkoutSessionResponse = checkoutSessionResponse,
            ),
        )
    }

    private fun List<FormElement>.billingCountryOwnerCount(): Int {
        val sectionFields = filterIsInstance<SectionElement>().flatMap { it.fields }
        return sectionFields.filterIsInstance<AddressFieldsElement>().size +
            sectionFields.filterIsInstance<CountryElement>().size
    }
}
