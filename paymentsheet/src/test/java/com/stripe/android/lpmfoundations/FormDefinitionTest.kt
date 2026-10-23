package com.stripe.android.lpmfoundations

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodRegistry
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.LinkCardBrandDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class FormDefinitionTest {
    @get:Rule
    val klarnaFormRemoval = FeatureFlagTestRule(FeatureFlags.enableKlarnaFormRemoval, false)

    @Test
    fun `registered definitions describe materialized forms`(
        @TestParameter(valuesProvider = MethodCodes::class) code: String,
        @TestParameter configuration: Configuration,
        @TestParameter removeKlarnaForm: Boolean,
    ) {
        klarnaFormRemoval.setEnabled(removeKlarnaForm)
        val metadata = metadata(code, configuration)
        val method = if (code == "link_card_brand") {
            LinkCardBrandDefinition
        } else {
            PaymentMethodRegistry.definitionsByCode.getValue(code)
        }
        val factory = method.uiDefinitionFactory(metadata)
        val requiresMandate = method.requiresMandate(metadata)
        val definition = factory.createFormDefinition(metadata, requiresMandate)
        val arguments = TestUiDefinitionFactoryArgumentsFactory.create().create(metadata, requiresMandate)
        assertDescriptionMatches(definition, factory.createFormElements(metadata, arguments))

        // Exercise the supported-method gate as well as every factory above.
        val supportedDefinition = metadata.formDefinitionForCode(method.type.code)
        val supportedElements = metadata.formElementsForCode(
            method.type.code,
            TestUiDefinitionFactoryArgumentsFactory.create(),
        )
        assertThat(supportedDefinition == null).isEqualTo(supportedElements == null)
        if (supportedDefinition != null && supportedElements != null) {
            assertDescriptionMatches(supportedDefinition, supportedElements)
        }
    }

    @Test
    fun `external and custom definitions describe materialized forms`(
        @TestParameter configuration: Configuration,
        @TestParameter kind: AdditionalMethod,
    ) {
        val code = if (kind == AdditionalMethod.External) "external_paypal" else "cpmt_test"
        val metadata = metadata(code, configuration).copy(
            externalPaymentMethodSpecs = if (kind == AdditionalMethod.External) {
                listOf(ExternalPaymentMethodSpec(code, "External", "https://example.com", null))
            } else {
                emptyList()
            },
            displayableCustomPaymentMethods = listOf(
                DisplayableCustomPaymentMethod(
                    id = code,
                    displayName = "Custom",
                    logoUrl = "https://example.com",
                    subtitle = "Custom subtitle".resolvableString,
                    doesNotCollectBillingDetails = kind == AdditionalMethod.CustomWithoutBilling,
                )
            ),
        )
        val definition = requireNotNull(metadata.formDefinitionForCode(code))
        val elements = requireNotNull(
            metadata.formElementsForCode(code, TestUiDefinitionFactoryArgumentsFactory.create())
        )
        assertDescriptionMatches(definition, elements)
        if (kind == AdditionalMethod.CustomWithoutBilling) {
            assertThat(definition.allowsUserInteraction).isFalse()
        }
    }

    @Test
    fun `description never invokes factories and materialization creates fresh elements`() {
        val calls = Turbine<Unit>()
        val text = "First mandate".resolvableString
        val definition = FormDefinition(
            listOf(
                FormElementDefinition(false, text) {
                    calls.add(Unit)
                    listOf(StaticTextElement(FormFieldId.Generic("header"), text = text))
                },
                FormElementDefinition(false, "Second mandate".resolvableString) { error("Not materialized") },
            )
        )
        assertThat(definition.allowsUserInteraction).isFalse()
        assertThat(definition.mandateText).isEqualTo(text)
        calls.expectNoEvents()

        val materializable = FormDefinition(definition.elements.take(1))
        val metadata = PaymentMethodMetadataFactory.create()
        val arguments = TestUiDefinitionFactoryArgumentsFactory.create().create(metadata, false)
        val first = materializable.createFormElements(arguments)
        assertThat(calls.takeItem()).isEqualTo(Unit)
        val reopened = materializable.createFormElements(arguments)
        assertThat(calls.takeItem()).isEqualTo(Unit)
        assertThat(reopened.single()).isNotSameInstanceAs(first.single())
        calls.ensureAllEventsConsumed()
    }

    private fun assertDescriptionMatches(definition: FormDefinition, elements: List<FormElement>) {
        assertThat(definition.allowsUserInteraction).isEqualTo(elements.any { it.allowsUserInteraction })
        assertThat(definition.mandateText).isEqualTo(elements.firstNotNullOfOrNull { it.mandateText })
    }

    private fun metadata(code: String, configuration: Configuration): PaymentMethodMetadata {
        val billing = when (configuration) {
            Configuration.All -> PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
            Configuration.Never -> PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
            else -> PaymentSheet.BillingDetailsCollectionConfiguration()
        }
        val intent = if (configuration in setOf(Configuration.Setup, Configuration.NoMandates)) {
            SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(paymentMethodTypes = listOf(code))
        } else {
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf(code),
                countryCode = if (configuration == Configuration.PixBrazil) "BR" else "US",
            )
        }
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intent,
            billingDetailsCollectionConfiguration = billing,
            hasCustomerConfiguration = configuration == Configuration.SaveOptions,
            isPaymentMethodSetAsDefaultEnabled = configuration == Configuration.SaveOptions,
            forceSetupFutureUseBehaviorAndNewMandate = configuration == Configuration.ForcedMandate,
            termsDisplay = if (configuration == Configuration.NoMandates) {
                PaymentMethod.Type.entries.associateWith { PaymentSheet.TermsDisplay.NEVER }
            } else {
                emptyMap()
            },
        )
        return if (configuration == Configuration.AutomaticTax) {
            metadata.copy(
                integrationMetadata = IntegrationMetadata.CheckoutSession(
                    id = "cs_test",
                    instancesKey = "test",
                    collectedEmail = null,
                    checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                        automaticTaxEnabled = true,
                        taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                    ),
                ),
            )
        } else {
            metadata
        }
    }

    enum class Configuration {
        Default, All, Never, Setup, AutomaticTax, NoMandates, PixBrazil, SaveOptions, ForcedMandate
    }
    enum class AdditionalMethod { External, Custom, CustomWithoutBilling }

    object MethodCodes : TestParameterValuesProvider() {
        override fun provideValues(context: Context?): List<String> {
            return PaymentMethodRegistry.all.map { it.type.code } + "link_card_brand"
        }
    }
}
