package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.Address
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.elements.payment.CustomPaymentMethod
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.getDefaultCustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AffirmDefinition
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFails
import com.stripe.android.core.R as CoreR
import com.stripe.android.uicore.R as UiCoreR

@RunWith(RobolectricTestRunner::class)
internal class PaymentMethodMetadataTest {

    @Test
    fun `hasIntentToSetup returns true for setup_intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )
        assertThat(metadata.hasIntentToSetup(PaymentMethod.Type.Card.code)).isTrue()
    }

    @Test
    fun `hasIntentToSetup returns true for payment_intent with setup_future_usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
        )
        assertThat(metadata.hasIntentToSetup(PaymentMethod.Type.Card.code)).isTrue()
    }

    @Test
    fun `hasIntentToSetup returns false for payment_intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )
        assertThat(metadata.hasIntentToSetup(PaymentMethod.Type.Card.code)).isFalse()
    }

    @Test
    fun `hasIntentToSetup returns true for payment_intent with PMO SFU set to off_session`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
            ),
        )
        assertThat(metadata.hasIntentToSetup(PaymentMethod.Type.Card.code)).isTrue()
    }

    @Test
    fun `hasIntentToSetup returns false for payment_intent with top level SFU and PMO SFU set to none`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OffSession,
                paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
            ),
        )
        assertThat(metadata.hasIntentToSetup(PaymentMethod.Type.Affirm.code)).isFalse()
    }

    @Test
    fun `hasIntentToSetup returns top level SFU if PMO SFU is not set`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OffSession
            ),
        )
        assertThat(metadata.hasIntentToSetup(PaymentMethod.Type.Klarna.code)).isTrue()
    }

    @Test
    fun `filterSupportedPaymentMethods removes unsupported paymentMethodTypes`() {
        val stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "pay_now"),
        )
        val metadata = PaymentMethodMetadataFactory.create(stripeIntent)
        val supportedPaymentMethods = metadata.supportedPaymentMethodTypes()
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods.first()).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods filters payment methods without shared data specs`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            sharedDataSpecs = listOf(SharedDataSpec("card")),
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodTypes()
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods.first()).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods returns expected items`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            sharedDataSpecs = listOf(SharedDataSpec("card"), SharedDataSpec("klarna")),
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodTypes()
        assertThat(supportedPaymentMethods).hasSize(2)
        assertThat(supportedPaymentMethods[0]).isEqualTo("card")
        assertThat(supportedPaymentMethods[1]).isEqualTo("klarna")
    }

    @Test
    fun `filterSupportedPaymentMethods filters unactivated payment methods in live mode`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
                unactivatedPaymentMethods = listOf("klarna"),
                isLiveMode = true,
            ),
            sharedDataSpecs = listOf(SharedDataSpec("card"), SharedDataSpec("klarna")),
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodTypes()
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods[0]).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods does not filter unactivated payment methods in test mode`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
                unactivatedPaymentMethods = listOf("klarna"),
                isLiveMode = false,
            ),
            sharedDataSpecs = listOf(SharedDataSpec("card"), SharedDataSpec("klarna")),
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodTypes()
        assertThat(supportedPaymentMethods).hasSize(2)
        assertThat(supportedPaymentMethods[0]).isEqualTo("card")
        assertThat(supportedPaymentMethods[1]).isEqualTo("klarna")
    }

    @Test
    fun `supportedPaymentMethodForCode returns expected result`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("klarna")
            ),
            sharedDataSpecs = listOf(SharedDataSpec("klarna")),
        )
        assertThat(metadata.supportedPaymentMethodForCode("klarna")?.code).isEqualTo("klarna")
    }

    @Test
    fun `supportedPaymentMethodForCode returns null when sharedDataSpecs are missing`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("klarna")
            ),
            sharedDataSpecs = emptyList(),
        )
        assertThat(metadata.supportedPaymentMethodForCode("klarna")).isNull()
    }

    @Test
    fun `supportedPaymentMethodForCode returns null when it's not supported`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card")
            ),
            sharedDataSpecs = listOf(SharedDataSpec("klarna")),
        )
        assertThat(metadata.supportedPaymentMethodForCode("klarna")).isNull()
    }

    @Test
    fun `sortedSupportedPaymentMethods returns list sorted by payment_method_types`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "affirm", "klarna"),
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
            sharedDataSpecs = listOf(
                SharedDataSpec("affirm"),
                SharedDataSpec("card"),
                SharedDataSpec("klarna"),
            ),
        )
        val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
        assertThat(sortedSupportedPaymentMethods).hasSize(3)
        assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("card")
        assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("affirm")
        assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("klarna")
    }

    @Test
    fun `sortedSupportedPaymentMethods returns list sorted by payment_method_types with different order`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("affirm", "klarna", "card"),
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
            sharedDataSpecs = listOf(
                SharedDataSpec("affirm"),
                SharedDataSpec("card"),
                SharedDataSpec("klarna"),
            ),
        )
        val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
        assertThat(sortedSupportedPaymentMethods).hasSize(3)
        assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("affirm")
        assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("klarna")
        assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("card")
    }

    @Test
    fun `sortedSupportedPaymentMethods filters payment methods without a sharedDataSpec`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("affirm", "klarna", "card"),
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
            sharedDataSpecs = listOf(
                SharedDataSpec("affirm"),
                SharedDataSpec("card"),
            ),
        )
        val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
        assertThat(sortedSupportedPaymentMethods).hasSize(2)
        assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("affirm")
        assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("card")
    }

    @Test
    fun `sortedSupportedPaymentMethods filters unactivated payment methods`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("affirm", "klarna", "card"),
                unactivatedPaymentMethods = listOf("klarna"),
                isLiveMode = true,
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
            sharedDataSpecs = listOf(
                SharedDataSpec("affirm"),
                SharedDataSpec("klarna"),
                SharedDataSpec("card"),
            ),
        )
        val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
        assertThat(sortedSupportedPaymentMethods).hasSize(2)
        assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("affirm")
        assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("card")
    }

    @Test
    fun `sortedSupportedPaymentMethods keeps us_bank_account without a sharedDataSpec`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "us_bank_account"),
                paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"automatic"}}""",
            ),
            sharedDataSpecs = listOf(
                SharedDataSpec("card"),
            ),
        )
        val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
        assertThat(sortedSupportedPaymentMethods).hasSize(2)
        assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("card")
        assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("us_bank_account")
    }

    @Test
    fun `sortedSupportedPaymentMethods sorts on custom sort`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("affirm", "klarna", "card"),
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
            paymentMethodOrder = listOf("klarna", "affirm", "card", "ignored"),
            sharedDataSpecs = listOf(
                SharedDataSpec("affirm"),
                SharedDataSpec("klarna"),
                SharedDataSpec("card"),
            ),
        )
        val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
        assertThat(sortedSupportedPaymentMethods).hasSize(3)
        assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("klarna")
        assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("affirm")
        assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("card")
    }

    @Test
    fun `sortedSupportedPaymentMethods add unrequested payment methods at the end`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("klarna", "affirm", "card"),
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
            paymentMethodOrder = listOf("card"),
            sharedDataSpecs = listOf(
                SharedDataSpec("affirm"),
                SharedDataSpec("klarna"),
                SharedDataSpec("card"),
            ),
        )
        val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
        assertThat(sortedSupportedPaymentMethods).hasSize(3)
        assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("card")
        assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("klarna")
        assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("affirm")
    }

    @Test
    fun `supportedSavedPaymentMethodTypes filters payment_methods not returned in the payment_intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card"),
            ),
            sharedDataSpecs = listOf(
                SharedDataSpec("card"),
                SharedDataSpec("sepa_debit"),
            ),
        )
        assertThat(metadata.supportedSavedPaymentMethodTypes())
            .containsExactly(PaymentMethod.Type.Card)
    }

    @Test
    fun `supportedSavedPaymentMethodTypes filters payment_methods where supportedAsSavedPaymentMethod is false`() {
        assertThat(AffirmDefinition.supportedAsSavedPaymentMethod).isFalse()
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "affirm", "sepa_debit"),
            ),
            sharedDataSpecs = listOf(
                SharedDataSpec("card"),
                SharedDataSpec("affirm"),
                SharedDataSpec("sepa_debit"),
            ),
        )
        assertThat(metadata.supportedSavedPaymentMethodTypes())
            .containsExactly(PaymentMethod.Type.Card, PaymentMethod.Type.SepaDebit)
    }

    @Test
    fun `amount values match payment intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                amount = 500,
                currency = "USD",
            ),
        )
        assertThat(metadata.amount()).isEqualTo(Amount(500, "USD"))
    }

    @Test
    fun `amount fails if payment intent has null amount`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                amount = null,
                currency = "USD",
            ),
        )
        assertFails {
            metadata.amount()
        }
    }

    @Test
    fun `amount fails if payment intent has null currency`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                amount = 500,
                currency = null,
            ),
        )
        assertFails {
            metadata.amount()
        }
    }

    @Test
    fun `amount is null for setup intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )
        assertThat(metadata.amount()).isNull()
    }

    @Test
    fun `formElementsForCode is constructed correctly`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "bancontact")
            ),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = false,
            )
        )
        val formElement = metadata.formElementsForCode(
            code = "bancontact",
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )!!

        val nameSection = formElement[0] as SectionElement
        val nameElement = nameSection.fields[0] as SimpleTextElement
        assertThat(nameElement.controller.label.first()).isEqualTo(
            resolvableString(CoreR.string.stripe_address_label_full_name)
        )
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")

        val emailSection = formElement[1] as SectionElement
        val emailElement = emailSection.fields[0] as EmailElement
        assertThat(emailElement.controller.label.first()).isEqualTo(
            resolvableString(UiCoreR.string.stripe_email)
        )
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")

        val phoneSection = formElement[2] as SectionElement
        val phoneElement = phoneSection.fields[0] as PhoneNumberElement
        assertThat(phoneElement.controller.label.first()).isEqualTo(
            resolvableString(CoreR.string.stripe_address_label_phone_number)
        )
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")

        val addressSection = formElement[3] as SectionElement
        val addressElement = addressSection.fields[0] as AddressElement

        val identifiers = addressElement.fields.first().map { it.identifier }
        // Check that the address element contains country.
        assertThat(identifiers).contains(IdentifierSpec.Country)
    }

    @Test
    fun `formElementsForCode contains mandate for PMO SFU`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = "cashapp",
                    sfuValue = "off_session"
                )
            )
        )
        val formElement = metadata.formElementsForCode(
            code = "cashapp",
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )!!

        val mandate = formElement[0] as MandateTextElement
        assertThat(mandate.mandateText).isNotNull()
        assertThat(mandate.identifier.v1).isEqualTo("cashapp_mandate")
    }

    @Test
    fun `formElementsForCode does not contain mandate for PMO SFU none override`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
                setupFutureUsage = StripeIntent.Usage.OffSession,
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = "cashapp",
                    sfuValue = "none"
                )
            )
        )
        val formElement = metadata.formElementsForCode(
            code = "cashapp",
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )!!

        assertThat(formElement).isEmpty()
    }

    @Test
    fun `formElementsForCode is constructed correctly for external payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "bancontact")
            ),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = false,
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
        )
        val formElement = metadata.formElementsForCode(
            code = PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC.type,
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )!!

        val nameSection = formElement[0] as SectionElement
        val nameElement = nameSection.fields[0] as SimpleTextElement
        assertThat(nameElement.controller.label.first()).isEqualTo(
            resolvableString(CoreR.string.stripe_address_label_full_name)
        )
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")

        val phoneSection = formElement[1] as SectionElement
        val phoneElement = phoneSection.fields[0] as PhoneNumberElement
        assertThat(phoneElement.controller.label.first()).isEqualTo(
            resolvableString(CoreR.string.stripe_address_label_phone_number)
        )
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")

        val emailSection = formElement[2] as SectionElement
        val emailElement = emailSection.fields[0] as EmailElement
        assertThat(emailElement.controller.label.first()).isEqualTo(
            resolvableString(UiCoreR.string.stripe_email)
        )
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")

        val addressSection = formElement[3] as SectionElement
        val addressElement = addressSection.fields[0] as AddressElement

        val identifiers = addressElement.fields.first().map { it.identifier }
        // Check that the address element contains country.
        assertThat(identifiers).contains(IdentifierSpec.Country)
    }

    @Test
    fun `formElementsForCode is empty by default for external payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "bancontact")
            ),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
        )
        val formElement = metadata.formElementsForCode(
            code = PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC.type,
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )!!

        assertThat(formElement.isEmpty()).isTrue()
    }

    @Test
    fun `formElementsForCode replaces country placeholder fields correctly`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = false,
            )
        )
        val formElement = metadata.formElementsForCode(
            code = "klarna",
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )!!

        val countrySection = formElement[4] as SectionElement
        val countryElement = countrySection.fields[0] as CountryElement
        assertThat(countryElement.identifier).isEqualTo(IdentifierSpec.Country)

        val addressSection = formElement[5] as SectionElement
        val addressElement = addressSection.fields[0] as AddressElement
        val addressIdentifiers = addressElement.fields.first().map { it.identifier }
        // Check that the address element doesn't contain country.
        assertThat(addressIdentifiers).doesNotContain(IdentifierSpec.Country)
    }

    @Test
    fun `formHeaderInformationForCode is correct for UiDefinitionFactorySimple`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create()
        val headerInformation = metadata.formHeaderInformationForCode(
            code = "card",
            customerHasSavedPaymentMethods = true
        )!!
        assertThat(headerInformation.displayName).isEqualTo(R.string.stripe_paymentsheet_add_new_card.resolvableString)
        assertThat(headerInformation.shouldShowIcon).isFalse()
    }

    @Test
    fun `formHeaderInformationForCode is correct for UiDefinitionFactoryRequiresSharedDataSpec`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "bancontact")
            ),
        )
        val headerInformation = metadata.formHeaderInformationForCode(
            code = "bancontact",
            customerHasSavedPaymentMethods = true,
        )!!
        assertThat(headerInformation.displayName)
            .isEqualTo(R.string.stripe_paymentsheet_payment_method_bancontact.resolvableString)
        assertThat(headerInformation.shouldShowIcon).isTrue()
        assertThat(headerInformation.icon(IconStyle.Filled))
            .isEqualTo(R.drawable.stripe_ic_paymentsheet_pm_bancontact)
        assertThat(headerInformation.icon(IconStyle.Outlined))
            .isEqualTo(R.drawable.stripe_ic_paymentsheet_pm_bancontact)
    }

    @Test
    fun `formHeaderInformationForCode is constructed correctly for external payment method`() = runTest {
        val paypalSpec = PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
        val metadata = PaymentMethodMetadataFactory.create(externalPaymentMethodSpecs = listOf(paypalSpec))
        val headerInformation = metadata.formHeaderInformationForCode(
            code = paypalSpec.type,
            customerHasSavedPaymentMethods = true,
        )!!
        assertThat(headerInformation.displayName).isEqualTo(paypalSpec.label.resolvableString)
        assertThat(headerInformation.shouldShowIcon).isTrue()
        assertThat(headerInformation.icon(IconStyle.Filled)).isEqualTo(0)
        assertThat(headerInformation.icon(IconStyle.Outlined)).isEqualTo(0)
        assertThat(headerInformation.lightThemeIconUrl).isEqualTo(paypalSpec.lightImageUrl)
        assertThat(headerInformation.darkThemeIconUrl).isEqualTo(paypalSpec.darkImageUrl)
        assertThat(headerInformation.iconRequiresTinting).isFalse()
    }

    @Test
    fun `When external payment methods are present and no payment method order, EPMs are shown last`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "affirm", "klarna"),
                ),
                allowsPaymentMethodsRequiringShippingAddress = true,
                sharedDataSpecs = listOf(
                    SharedDataSpec("affirm"),
                    SharedDataSpec("card"),
                    SharedDataSpec("klarna"),
                ),
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
            )
            val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
            assertThat(sortedSupportedPaymentMethods).hasSize(4)
            assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("card")
            assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("affirm")
            assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("klarna")
            assertThat(sortedSupportedPaymentMethods[3].code).isEqualTo("external_paypal")
        }

    @Test
    fun `When external payment methods are present and in payment method order, payment method order works`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "affirm", "klarna"),
                ),
                allowsPaymentMethodsRequiringShippingAddress = true,
                sharedDataSpecs = listOf(
                    SharedDataSpec("affirm"),
                    SharedDataSpec("card"),
                    SharedDataSpec("klarna"),
                ),
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
                paymentMethodOrder = listOf("affirm", "external_paypal")
            )
            val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
            assertThat(sortedSupportedPaymentMethods).hasSize(4)
            assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("affirm")
            assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("external_paypal")
            assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("card")
            assertThat(sortedSupportedPaymentMethods[3].code).isEqualTo("klarna")
        }

    @Test
    fun `When external payment methods are present and not in payment method order, payment method order works`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "affirm", "klarna"),
                ),
                allowsPaymentMethodsRequiringShippingAddress = true,
                sharedDataSpecs = listOf(
                    SharedDataSpec("affirm"),
                    SharedDataSpec("card"),
                    SharedDataSpec("klarna"),
                ),
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
                paymentMethodOrder = listOf("affirm")
            )
            val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
            assertThat(sortedSupportedPaymentMethods).hasSize(4)
            assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("affirm")
            assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("card")
            assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("klarna")
            assertThat(sortedSupportedPaymentMethods[3].code).isEqualTo("external_paypal")
        }

    @Test
    fun `External payment method does not require mandate`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

        assertThat(metadata.requiresMandate("external_paypal")).isFalse()
    }

    @Test
    fun `External payment methods are included in supported payment method types`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

        assertThat(metadata.supportedPaymentMethodTypes().contains("external_paypal")).isTrue()
    }

    @Test
    fun `External payment methods are not included in supported saved payment method types`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

        assertThat(metadata.supportedSavedPaymentMethodTypes().map { it.code }.contains("external_paypal")).isFalse()
    }

    @Test
    fun `External payment methods return correct supported payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )
        val expectedSupportedPaymentMethod = SupportedPaymentMethod(
            code = "external_paypal",
            displayName = "PayPal".resolvableString,
            lightThemeIconUrl = "example_url",
            darkThemeIconUrl = null,
            iconResource = 0,
            iconResourceNight = 0,
            iconRequiresTinting = false,
        )

        val actualSupportedPaymentMethod = metadata.supportedPaymentMethodForCode("external_paypal")

        assertThat(actualSupportedPaymentMethod).isEqualTo(expectedSupportedPaymentMethod)
    }

    @Test
    fun `isExternalPaymentMethod returns true for supported EPM`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

        assertThat(metadata.isExternalPaymentMethod("external_paypal")).isTrue()
    }

    @Test
    fun `isExternalPaymentMethod returns false for unsupported EPM`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

        assertThat(metadata.isExternalPaymentMethod("external_venmo")).isFalse()
    }

    @Test
    fun `isExternalPaymentMethod returns false for non-custom payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

        assertThat(metadata.isExternalPaymentMethod("card")).isFalse()
    }

    @Test
    fun `formHeaderInformationForCode is constructed correctly for custom payment method`() = runTest {
        val paypalCpm = PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD
        val metadata = PaymentMethodMetadataFactory.create(
            displayableCustomPaymentMethods = listOf(paypalCpm),
        )
        val headerInformation = metadata.formHeaderInformationForCode(
            code = paypalCpm.id,
            customerHasSavedPaymentMethods = false,
        )!!
        assertThat(headerInformation.displayName).isEqualTo(paypalCpm.displayName.resolvableString)
        assertThat(headerInformation.shouldShowIcon).isTrue()
        assertThat(headerInformation.icon(IconStyle.Filled)).isEqualTo(0)
        assertThat(headerInformation.icon(IconStyle.Outlined)).isEqualTo(0)
        assertThat(headerInformation.lightThemeIconUrl).isEqualTo(paypalCpm.logoUrl)
        assertThat(headerInformation.darkThemeIconUrl).isEqualTo(paypalCpm.logoUrl)
        assertThat(headerInformation.iconRequiresTinting).isFalse()
    }

    @Test
    fun `When custom payment methods are present and no payment method order, CPMs are shown last`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "affirm", "klarna"),
                ),
                allowsPaymentMethodsRequiringShippingAddress = true,
                sharedDataSpecs = listOf(
                    SharedDataSpec("affirm"),
                    SharedDataSpec("card"),
                    SharedDataSpec("klarna"),
                ),
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
                displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
            )
            val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
            assertThat(sortedSupportedPaymentMethods).hasSize(5)
            assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("card")
            assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("affirm")
            assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("klarna")
            assertThat(sortedSupportedPaymentMethods[3].code).isEqualTo("external_paypal")
            assertThat(sortedSupportedPaymentMethods[4].code).isEqualTo("cpmt_paypal")
        }

    @Test
    fun `When custom payment methods are present and in payment method order, payment method order works`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "affirm", "klarna"),
                ),
                allowsPaymentMethodsRequiringShippingAddress = true,
                sharedDataSpecs = listOf(
                    SharedDataSpec("affirm"),
                    SharedDataSpec("card"),
                    SharedDataSpec("klarna"),
                ),
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
                displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
                paymentMethodOrder = listOf("affirm", "cpmt_paypal", "external_paypal")
            )
            val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
            assertThat(sortedSupportedPaymentMethods).hasSize(5)
            assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("affirm")
            assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("cpmt_paypal")
            assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("external_paypal")
            assertThat(sortedSupportedPaymentMethods[3].code).isEqualTo("card")
            assertThat(sortedSupportedPaymentMethods[4].code).isEqualTo("klarna")
        }

    @Test
    fun `When custom payment methods are present and not in payment method order, payment method order works`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "affirm", "klarna"),
                ),
                allowsPaymentMethodsRequiringShippingAddress = true,
                sharedDataSpecs = listOf(
                    SharedDataSpec("affirm"),
                    SharedDataSpec("card"),
                    SharedDataSpec("klarna"),
                ),
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
                displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
                paymentMethodOrder = listOf("affirm", "external_paypal")
            )
            val sortedSupportedPaymentMethods = metadata.sortedSupportedPaymentMethods()
            assertThat(sortedSupportedPaymentMethods).hasSize(5)
            assertThat(sortedSupportedPaymentMethods[0].code).isEqualTo("affirm")
            assertThat(sortedSupportedPaymentMethods[1].code).isEqualTo("external_paypal")
            assertThat(sortedSupportedPaymentMethods[2].code).isEqualTo("card")
            assertThat(sortedSupportedPaymentMethods[3].code).isEqualTo("klarna")
            assertThat(sortedSupportedPaymentMethods[4].code).isEqualTo("cpmt_paypal")
        }

    @Test
    fun `Custom payment method does not require mandate`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        )

        assertThat(metadata.requiresMandate("cpmt_paypal")).isFalse()
    }

    @Test
    fun `Custom payment methods are included in supported payment method types`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        )

        assertThat(metadata.supportedPaymentMethodTypes().contains("cpmt_paypal")).isTrue()
    }

    @Test
    fun `Custom payment methods are not included in supported saved payment method types`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        )

        assertThat(metadata.supportedSavedPaymentMethodTypes().map { it.code }.contains("cpmt_paypal")).isFalse()
    }

    @Test
    fun `Custom payment methods return correct supported payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        )
        val expectedSupportedPaymentMethod = SupportedPaymentMethod(
            code = "cpmt_paypal",
            subtitle = "Pay now with PayPal".resolvableString,
            displayName = "PayPal".resolvableString,
            lightThemeIconUrl = "example_url",
            darkThemeIconUrl = "example_url",
            iconResource = 0,
            iconResourceNight = 0,
            iconRequiresTinting = false,
        )

        val actualSupportedPaymentMethod = metadata.supportedPaymentMethodForCode("cpmt_paypal")

        assertThat(actualSupportedPaymentMethod).isEqualTo(expectedSupportedPaymentMethod)
    }

    @Test
    fun `isCustomPaymentMethod returns true for supported CPM`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        )

        assertThat(metadata.isCustomPaymentMethod("cpmt_paypal")).isTrue()
    }

    @Test
    fun `isCustomPaymentMethod returns false for unsupported CPM`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        )

        assertThat(metadata.isCustomPaymentMethod("cpmt_venmo")).isFalse()
    }

    @Test
    fun `isCustomPaymentMethod returns false for non-custom payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        )

        assertThat(metadata.isCustomPaymentMethod("card")).isFalse()
    }

    @Suppress("LongMethod")
    @Test
    fun `should create metadata properly with elements session response, payment sheet config, and data specs`() {
        val billingDetailsCollectionConfiguration = createBillingDetailsCollectionConfiguration()
        val defaultBillingDetails = BillingDetails(
            address = Address(line1 = "123 Apple Street")
        )
        val shippingDetails = AddressDetails(address = Address(line1 = "123 Pear Street"))
        val cardBrandAcceptance = CardBrandAcceptance.allowed(
            listOf(CardBrandAcceptance.BrandCategory.Amex)
        )
        val customPaymentMethods = listOf(
            CustomPaymentMethod(
                id = "cpmt_123",
                subtitle = "Pay now".resolvableString,
                disableBillingDetailCollection = true,
            ),
            CustomPaymentMethod(
                id = "cpmt_456",
                subtitle = "Pay later".resolvableString,
                disableBillingDetailCollection = false,
            ),
            CustomPaymentMethod(
                id = "cpmt_789",
                subtitle = "Pay never".resolvableString,
                disableBillingDetailCollection = false,
            )
        )
        val configuration = createPaymentSheetConfiguration(
            billingDetailsCollectionConfiguration,
            defaultBillingDetails,
            shippingDetails,
            customPaymentMethods,
            cardBrandAcceptance,
        )
        val elementsSession = createElementsSession(
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires"),
            ),
            customPaymentMethods = listOf(
                ElementsSession.CustomPaymentMethod.Available(
                    type = "cpmt_123",
                    displayName = "CPM #1",
                    logoUrl = "https://image1",
                ),
                ElementsSession.CustomPaymentMethod.Available(
                    type = "cpmt_456",
                    displayName = "CPM #2",
                    logoUrl = "https://image2",
                ),
                ElementsSession.CustomPaymentMethod.Unavailable(
                    type = "cpmt_789",
                    error = "not_found",
                ),
            )
        )

        val sharedDataSpecs = listOf(SharedDataSpec("card"))
        val externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)

        val metadata = PaymentMethodMetadata.createForPaymentElement(
            elementsSession = elementsSession,
            configuration = configuration.asCommonConfiguration(),
            sharedDataSpecs = sharedDataSpecs,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            isGooglePayReady = false,
            linkState = LinkState(
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                configuration = createLinkConfiguration(),
                loginState = LinkState.LoginState.LoggedOut,
            ),
            customerMetadata = DEFAULT_CUSTOMER_METADATA
        )

        val expectedMetadata = PaymentMethodMetadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = true,
            allowsPaymentMethodsRequiringShippingAddress = false,
            allowsLinkInSavedPaymentMethods = false,
            availableWallets = listOf(WalletType.Link),
            paymentMethodOrder = listOf("us_bank_account", "card", "sepa_debit"),
            cbcEligibility = CardBrandChoiceEligibility.Eligible(
                preferredNetworks = listOf(
                    CardBrand.CartesBancaires,
                    CardBrand.Visa
                )
            ),
            merchantName = "Merchant Inc.",
            defaultBillingDetails = defaultBillingDetails,
            shippingDetails = shippingDetails,
            sharedDataSpecs = sharedDataSpecs,
            displayableCustomPaymentMethods = listOf(
                DisplayableCustomPaymentMethod(
                    id = "cpmt_123",
                    displayName = "CPM #1",
                    logoUrl = "https://image1",
                    subtitle = "Pay now".resolvableString,
                    doesNotCollectBillingDetails = true,
                ),
                DisplayableCustomPaymentMethod(
                    id = "cpmt_456",
                    displayName = "CPM #2",
                    logoUrl = "https://image2",
                    subtitle = "Pay later".resolvableString,
                    doesNotCollectBillingDetails = false,
                )
            ),
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            customerMetadata = getDefaultCustomerMetadata(
                isPaymentMethodSetAsDefaultEnabled = false
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            isGooglePayReady = false,
            linkConfiguration = com.stripe.android.elements.payment.LinkConfiguration(),
            linkMode = null,
            linkState = LinkState(
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                configuration = createLinkConfiguration(),
                loginState = LinkState.LoginState.LoggedOut,
            ),
            cardBrandFilter = PaymentSheetCardBrandFilter(cardBrandAcceptance),
            paymentMethodIncentive = null,
            elementsSessionId = "session_1234",
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
            shopPayConfiguration = null
        )

        assertThat(metadata).isEqualTo(expectedMetadata)
    }

    @Suppress("LongMethod")
    @Test
    fun `should create metadata properly with elements session response, customer sheet config, and data specs`() {
        val billingDetailsCollectionConfiguration = createBillingDetailsCollectionConfiguration()
        val defaultBillingDetails = BillingDetails(
            address = Address(line1 = "123 Apple Street")
        )
        val cardBrandAcceptance = CardBrandAcceptance.allowed(
            listOf(CardBrandAcceptance.BrandCategory.Amex)
        )

        val configuration = createCustomerSheetConfiguration(
            billingDetailsCollectionConfiguration,
            defaultBillingDetails,
            cardBrandAcceptance
        )

        val elementsSession = createElementsSession(
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires")
            ),
        )

        val paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
            overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        )

        val metadata = PaymentMethodMetadata.createForCustomerSheet(
            elementsSession = elementsSession,
            configuration = configuration,
            paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
            sharedDataSpecs = listOf(SharedDataSpec("card")),
            isGooglePayReady = true,
            customerMetadata = DEFAULT_CUSTOMER_METADATA,
        )

        val expectedMetadata = PaymentMethodMetadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = true,
            allowsPaymentMethodsRequiringShippingAddress = false,
            allowsLinkInSavedPaymentMethods = false,
            availableWallets = emptyList(),
            paymentMethodOrder = listOf("us_bank_account", "card", "sepa_debit"),
            cbcEligibility = CardBrandChoiceEligibility.Eligible(
                preferredNetworks = listOf(CardBrand.CartesBancaires, CardBrand.Visa)
            ),
            merchantName = "Merchant Inc.",
            defaultBillingDetails = defaultBillingDetails,
            shippingDetails = null,
            sharedDataSpecs = listOf(SharedDataSpec("card")),
            displayableCustomPaymentMethods = emptyList(),
            externalPaymentMethodSpecs = listOf(),
            customerMetadata = getDefaultCustomerMetadata(
                isPaymentMethodSetAsDefaultEnabled = false
            ),
            isGooglePayReady = true,
            paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
            linkConfiguration = com.stripe.android.elements.payment.LinkConfiguration(),
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
            linkMode = null,
            linkState = null,
            cardBrandFilter = PaymentSheetCardBrandFilter(cardBrandAcceptance),
            paymentMethodIncentive = null,
            elementsSessionId = "session_1234",
            shopPayConfiguration = null
        )
        assertThat(metadata).isEqualTo(expectedMetadata)
    }

    @Test
    fun `consent behavior should be Always for Payment Sheet is customer session save is enabled`() {
        val metadata = createPaymentMethodMetadataForPaymentSheet(
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                isPaymentMethodSaveEnabled = true,
                isPaymentMethodRemoveEnabled = true,
                canRemoveLastPaymentMethod = true,
                allowRedisplayOverride = null,
                isPaymentMethodSetAsDefaultEnabled = false,
            )
        )

        assertThat(metadata.paymentMethodSaveConsentBehavior).isEqualTo(PaymentMethodSaveConsentBehavior.Enabled)
    }

    @Test
    fun `consent behavior should be Disabled for Payment Sheet is customer session save is disabled`() {
        val metadata = createPaymentMethodMetadataForPaymentSheet(
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = true,
                canRemoveLastPaymentMethod = true,
                allowRedisplayOverride = null,
                isPaymentMethodSetAsDefaultEnabled = false,
            ),
        )

        assertThat(metadata.paymentMethodSaveConsentBehavior)
            .isEqualTo(
                PaymentMethodSaveConsentBehavior.Disabled(
                    overrideAllowRedisplay = null,
                ),
            )
    }

    @Test
    fun `consent behavior should be Legacy for Payment Sheet if payment sheet component is disabled`() {
        val metadata = createPaymentMethodMetadataForPaymentSheet(
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
        )

        assertThat(metadata.paymentMethodSaveConsentBehavior).isEqualTo(PaymentMethodSaveConsentBehavior.Legacy)
    }

    @Test
    fun `consent behavior should be Legacy for Payment Sheet if no customer session provided`() {
        val metadata = createPaymentMethodMetadataForPaymentSheet(
            mobilePaymentElementComponent = null,
        )

        assertThat(metadata.paymentMethodSaveConsentBehavior).isEqualTo(PaymentMethodSaveConsentBehavior.Legacy)
    }

    private fun createPaymentMethodMetadataForPaymentSheet(
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement?,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata.createForPaymentElement(
            elementsSession = createElementsSession(
                mobilePaymentElementComponent = mobilePaymentElementComponent
            ),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
            sharedDataSpecs = listOf(),
            externalPaymentMethodSpecs = listOf(),
            isGooglePayReady = false,
            linkState = null,
            customerMetadata = DEFAULT_CUSTOMER_METADATA
        )
    }

    private fun createElementsSession(
        intent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        cardBrandChoice: ElementsSession.CardBrandChoice = ElementsSession.CardBrandChoice(
            eligible = true,
            preferredNetworks = listOf("cartes_bancaires")
        ),
        orderedPaymentMethodTypesAndWallets: List<String> = intent.paymentMethodTypes,
        customPaymentMethods: List<ElementsSession.CustomPaymentMethod> = emptyList(),
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement? = null
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = intent,
            cardBrandChoice = cardBrandChoice,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = mobilePaymentElementComponent?.let { component ->
                ElementsSession.Customer(
                    paymentMethods = listOf(),
                    session = ElementsSession.Customer.Session(
                        id = "cuss_123",
                        customerId = "cus_123",
                        liveMode = false,
                        apiKey = "123",
                        apiKeyExpiry = 999999999,
                        components = ElementsSession.Customer.Components(
                            mobilePaymentElement = component,
                            customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            },
            linkSettings = null,
            customPaymentMethods = customPaymentMethods,
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = emptyMap(),
            orderedPaymentMethodTypesAndWallets = orderedPaymentMethodTypesAndWallets,
            experimentsData = null
        )
    }

    @Test
    fun `allowRedisplay returns Unspecified when consent behavior is Legacy`() = runTest {
        val metadataForPaymentIntent = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadataForPaymentIntent,
            expectedValue = PaymentMethod.AllowRedisplay.UNSPECIFIED
        )

        val metadataForSetupIntent = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadataForSetupIntent,
            expectedValue = PaymentMethod.AllowRedisplay.UNSPECIFIED
        )

        val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadataForPaymentIntentWithSfu,
            expectedValue = PaymentMethod.AllowRedisplay.UNSPECIFIED
        )

        val metadataForPaymentIntentWithPmoSfu = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadataForPaymentIntentWithPmoSfu,
            expectedValue = PaymentMethod.AllowRedisplay.UNSPECIFIED
        )
    }

    @Test
    fun `allowRedisplay returns Always when consent behavior is Enabled, setting up, and is saving for future use`() =
        runTest {
            val metadataForSetupIntent = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadataForSetupIntent.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)

            val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadataForPaymentIntentWithSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)

            val metadataForPaymentIntentWithPmoSfu = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadataForPaymentIntentWithPmoSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)
        }

    @Test
    fun `allowRedisplay returns Limited when consent behavior is Enabled, setting up, and is not saving`() =
        runTest {
            val metadataForSetupIntent = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadataForSetupIntent.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

            assertThat(
                metadataForSetupIntent.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

            val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadataForPaymentIntentWithSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

            assertThat(
                metadataForPaymentIntentWithSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

            val metadataForPaymentIntentWithPmoSfu = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadataForPaymentIntentWithPmoSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

            assertThat(
                metadataForPaymentIntentWithPmoSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)
        }

    @Test
    fun `allowRedisplay returns Always when consent behavior is Enabled, not setting up, and is saving`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadata.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)
        }

    @Test
    fun `allowRedisplay returns Unspecified when consent behavior is Enabled, not setting up, and is not saving`() =
        runTest {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled
            )

            assertThat(
                metadata.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

            assertThat(
                metadata.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                    code = PaymentMethod.Type.Card.code
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)
        }

    @Test
    fun `allowRedisplay returns Unspecified when consent behavior is Disabled and not setting up`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadata,
            expectedValue = PaymentMethod.AllowRedisplay.UNSPECIFIED
        )
    }

    @Test
    fun `allowRedisplay returns Limited when consent behavior is Disabled and setting up`() = runTest {
        val metadataForSetupIntent = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadataForSetupIntent,
            expectedValue = PaymentMethod.AllowRedisplay.LIMITED
        )

        val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadataForPaymentIntentWithSfu,
            expectedValue = PaymentMethod.AllowRedisplay.LIMITED
        )

        val metadataForPaymentIntentWithPmoSfu = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            )
        )

        testAllowRedisplayValueForCustomerRequestedSave(
            metadata = metadataForPaymentIntentWithPmoSfu,
            expectedValue = PaymentMethod.AllowRedisplay.LIMITED
        )
    }

    @Test
    fun `allowRedisplay returns override when consent behavior is Disabled with override value and setting up`() =
        runTest {
            val metadataForSetupIntent = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                    overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
                ),
            )

            testAllowRedisplayValueForCustomerRequestedSave(
                metadata = metadataForSetupIntent,
                expectedValue = PaymentMethod.AllowRedisplay.ALWAYS
            )

            val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                    overrideAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
                ),
            )

            testAllowRedisplayValueForCustomerRequestedSave(
                metadata = metadataForPaymentIntentWithSfu,
                expectedValue = PaymentMethod.AllowRedisplay.UNSPECIFIED
            )

            val metadataForPaymentIntentWithPmoSfu = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
            )

            testAllowRedisplayValueForCustomerRequestedSave(
                metadata = metadataForPaymentIntentWithPmoSfu,
                expectedValue = PaymentMethod.AllowRedisplay.UNSPECIFIED
            )
        }

    private fun testAllowRedisplayValueForCustomerRequestedSave(
        metadata: PaymentMethodMetadata,
        expectedValue: PaymentMethod.AllowRedisplay
    ) {
        assertThat(
            metadata.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                code = PaymentMethod.Type.Card.code
            )
        ).isEqualTo(expectedValue)

        assertThat(
            metadata.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                code = PaymentMethod.Type.Card.code
            )
        ).isEqualTo(expectedValue)

        assertThat(
            metadata.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                code = PaymentMethod.Type.Card.code
            )
        ).isEqualTo(expectedValue)
    }

    @Test
    fun `Adds LinkCardBrand if supported`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                linkFundingSources = listOf("card", "bank_account"),
                paymentMethodTypes = listOf("card"),
            ),
            linkMode = LinkMode.LinkCardBrand,
        )

        val displayedPaymentMethodTypes = metadata.supportedPaymentMethodTypes()
        assertThat(displayedPaymentMethodTypes).containsExactly("card", "link")
    }

    @Test
    fun `Does not add LinkCardBrand if not supported`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                linkFundingSources = listOf("card", "bank_account"),
                paymentMethodTypes = listOf("card"),
            ),
            linkMode = LinkMode.Passthrough,
        )

        val displayedPaymentMethodTypes = metadata.supportedPaymentMethodTypes()
        assertThat(displayedPaymentMethodTypes).containsExactly("card")
    }

    @Test
    fun `Passes eligible CBC along to Link`() {
        val linkConfiguration = LinkTestUtils.createLinkConfiguration(
            cardBrandChoice = LinkConfiguration.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires"),
            )
        )

        val metadata = PaymentMethodMetadata.createForNativeLink(
            configuration = linkConfiguration,
            linkAccount = linkAccount()
        )

        assertThat(metadata.cbcEligibility).isEqualTo(
            CardBrandChoiceEligibility.Eligible(
                preferredNetworks = listOf(CardBrand.CartesBancaires)
            )
        )
    }

    @Test
    fun `Passes ineligible CBC along to Link`() {
        val linkConfiguration = LinkTestUtils.createLinkConfiguration(
            cardBrandChoice = LinkConfiguration.CardBrandChoice(
                eligible = false,
                preferredNetworks = emptyList(),
            )
        )

        val metadata = PaymentMethodMetadata.createForNativeLink(
            configuration = linkConfiguration,
            linkAccount = linkAccount()
        )

        assertThat(metadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
    }

    @Test
    fun `requiresMandate returns true for PMO SFU`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodOptionsJsonString = PaymentIntentFixtures.PMO_SETUP_FUTURE_USAGE
            ),
        )
        assertThat(metadata.requiresMandate(PaymentMethod.Type.AmazonPay.code)).isTrue()
    }

    @Test
    fun `requiresMandate returns false for PMO SFU none override`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OffSession,
                paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                    code = "amazon_pay",
                    sfuValue = "none"
                )
            ),
        )
        assertThat(metadata.requiresMandate(PaymentMethod.Type.AmazonPay.code)).isFalse()
    }

    @Test
    fun `availableWallets contains all wallet types`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("google_pay", "link", "shop_pay", "card"),
        isGooglePayReady = true,
        hasLinkState = true,
        hasShopPayConfiguration = true,
        expectedWalletTypes = listOf(WalletType.Link, WalletType.GooglePay, WalletType.ShopPay),
    )

    @Test
    fun `availableWallets contains all wallet types in order with Link first`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("shop_pay", "link", "card", "google_pay"),
        isGooglePayReady = true,
        hasLinkState = true,
        hasShopPayConfiguration = true,
        expectedWalletTypes = listOf(WalletType.Link, WalletType.ShopPay, WalletType.GooglePay),
    )

    @Test
    fun `availableWallets contains only Google Pay`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "google_pay"),
        isGooglePayReady = true,
        hasLinkState = false,
        hasShopPayConfiguration = false,
        expectedWalletTypes = listOf(WalletType.GooglePay),
    )

    @Test
    fun `availableWallets does not contain Google Pay if not ready`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "google_pay"),
        isGooglePayReady = false,
        hasLinkState = false,
        hasShopPayConfiguration = false,
        expectedWalletTypes = emptyList(),
    )

    @Test
    fun `availableWallets contains only Link`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "link"),
        isGooglePayReady = false,
        hasLinkState = true,
        hasShopPayConfiguration = false,
        expectedWalletTypes = listOf(WalletType.Link),
    )

    @Test
    fun `availableWallets contains only ShopPay`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "shop_pay"),
        isGooglePayReady = false,
        hasLinkState = false,
        hasShopPayConfiguration = true,
        expectedWalletTypes = listOf(WalletType.ShopPay),
    )

    @Test
    fun `availableWallets does not contain ShopPay if no configuration`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "shop_pay"),
        isGooglePayReady = false,
        hasLinkState = false,
        hasShopPayConfiguration = false,
        expectedWalletTypes = emptyList(),
    )

    @Test
    fun `availableWallets does not contain ShopPay if not in types and no configuration`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "google_pay"),
        isGooglePayReady = false,
        hasLinkState = false,
        hasShopPayConfiguration = false,
        expectedWalletTypes = emptyList(),
    )

    @Test
    fun `availableWallets contains Link if state is available but not in types`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "google_pay"),
        isGooglePayReady = false,
        hasLinkState = true,
        hasShopPayConfiguration = false,
        expectedWalletTypes = listOf(WalletType.Link),
    )

    @Test
    fun `availableWallets puts Link first if available but not in types`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "google_pay"),
        isGooglePayReady = true,
        hasLinkState = true,
        hasShopPayConfiguration = false,
        expectedWalletTypes = listOf(WalletType.Link, WalletType.GooglePay),
    )

    @Test
    fun `availableWallets does not include Shop Pay if not in types`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("card", "google_pay"),
        isGooglePayReady = true,
        hasLinkState = true,
        hasShopPayConfiguration = true,
        expectedWalletTypes = listOf(WalletType.Link, WalletType.GooglePay),
    )

    @Test
    fun `availableWallets contains ShopPay and Link but not GooglePay`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("shop_pay", "link", "card"),
        isGooglePayReady = false,
        hasLinkState = true,
        hasShopPayConfiguration = true,
        expectedWalletTypes = listOf(WalletType.Link, WalletType.ShopPay),
    )

    @Test
    fun `availableWallets contains ShopPay and GooglePay but not Link`() = availableWalletsTest(
        orderedPaymentMethodTypesAndWallets = listOf("google_pay", "shop_pay", "card"),
        isGooglePayReady = true,
        hasLinkState = false,
        hasShopPayConfiguration = true,
        expectedWalletTypes = listOf(WalletType.GooglePay, WalletType.ShopPay),
    )

    private fun availableWalletsTest(
        orderedPaymentMethodTypesAndWallets: List<String>,
        isGooglePayReady: Boolean,
        hasLinkState: Boolean,
        hasShopPayConfiguration: Boolean,
        expectedWalletTypes: List<WalletType>,
    ) {
        val elementsSession = createElementsSession(
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            orderedPaymentMethodTypesAndWallets = orderedPaymentMethodTypesAndWallets,
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires")
            ),
        )

        val shopPayConfiguration = if (hasShopPayConfiguration) {
            SHOP_PAY_CONFIGURATION
        } else {
            null
        }

        val configuration = createPaymentSheetConfiguration(
            defaultBillingDetails = BillingDetails(),
            shippingDetails = AddressDetails(),
            billingDetailsCollectionConfiguration = createBillingDetailsCollectionConfiguration(),
            customPaymentMethods = listOf(),
            cardBrandAcceptance = CardBrandAcceptance.all(),
            shopPayConfiguration = shopPayConfiguration,
        )

        val metadata = PaymentMethodMetadata.createForPaymentElement(
            elementsSession = elementsSession,
            configuration = configuration.asCommonConfiguration(),
            sharedDataSpecs = emptyList(),
            externalPaymentMethodSpecs = emptyList(),
            isGooglePayReady = isGooglePayReady,
            linkState = if (hasLinkState) {
                LinkState(
                    configuration = mock(),
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                )
            } else {
                null
            },
            customerMetadata = DEFAULT_CUSTOMER_METADATA
        )

        assertThat(metadata.availableWallets)
            .containsExactlyElementsIn(expectedWalletTypes)
            .inOrder()
    }

    fun `Passes CBF along to Link`() {
        val linkConfiguration = LinkTestUtils.createLinkConfiguration(
            cardBrandFilter = PaymentSheetCardBrandFilter(CardBrandAcceptance.all())
        )

        val metadata = PaymentMethodMetadata.createForNativeLink(
            configuration = linkConfiguration,
            linkAccount = linkAccount()
        )

        assertThat(metadata.cardBrandFilter).isEqualTo(linkConfiguration.cardBrandFilter)
    }

    private fun createLinkConfiguration(): LinkConfiguration {
        return LinkConfiguration(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customerInfo = LinkConfiguration.CustomerInfo(
                email = "john@email.com",
                name = "John Doe",
                billingCountryCode = "CA",
                phone = "1234567890"
            ),
            merchantName = "Merchant Inc.",
            merchantCountryCode = "CA",
            shippingDetails = null,
            flags = mapOf(),
            cardBrandChoice = LinkConfiguration.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires")
            ),
            cardBrandFilter = DefaultCardBrandFilter,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
            passthroughModeEnabled = false,
            useAttestationEndpointsForLink = false,
            suppress2faModal = false,
            initializationMode = PaymentSheetFixtures.INITIALIZATION_MODE_PAYMENT_INTENT,
            elementsSessionId = "session_1234",
            linkMode = LinkMode.LinkPaymentMethod,
            allowDefaultOptIn = false,
            disableRuxInFlowController = false,
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
            defaultBillingDetails = null,
            collectMissingBillingDetailsForExistingPaymentMethods = true,
            allowUserEmailEdits = true,
            enableDisplayableDefaultValuesInEce = false,
            customerId = null
        )
    }

    private fun linkAccount() = LinkAccount(
        consumerSession = TestFactory.CONSUMER_SESSION
    )

    private fun createBillingDetailsCollectionConfiguration() =
        BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )

    private fun createCustomerSheetConfiguration(
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        defaultBillingDetails: BillingDetails,
        cardBrandAcceptance: CardBrandAcceptance
    ) = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant Inc.")
        .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
        .defaultBillingDetails(defaultBillingDetails)
        .preferredNetworks(listOf(CardBrand.CartesBancaires, CardBrand.Visa))
        .paymentMethodOrder(listOf("us_bank_account", "card", "sepa_debit"))
        .cardBrandAcceptance(cardBrandAcceptance)
        .build()

    private fun createPaymentSheetConfiguration(
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        defaultBillingDetails: BillingDetails,
        shippingDetails: AddressDetails,
        customPaymentMethods: List<CustomPaymentMethod>,
        cardBrandAcceptance: CardBrandAcceptance,
        shopPayConfiguration: PaymentSheet.ShopPayConfiguration? = null
    ) = PaymentSheet.Configuration(
        merchantDisplayName = "Merchant Inc.",
        allowsDelayedPaymentMethods = true,
        allowsPaymentMethodsRequiringShippingAddress = false,
        paymentMethodOrder = listOf("us_bank_account", "card", "sepa_debit"),
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        customer = CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_1"),
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        preferredNetworks = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
        customPaymentMethods = customPaymentMethods,
        cardBrandAcceptance = cardBrandAcceptance,
        shopPayConfiguration = shopPayConfiguration
    )
}
