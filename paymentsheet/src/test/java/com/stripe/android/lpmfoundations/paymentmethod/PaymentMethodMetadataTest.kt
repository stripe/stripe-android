package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AffirmDefinition
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkInlineConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.SharedDataSpec
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
        assertThat(metadata.hasIntentToSetup()).isTrue()
    }

    @Test
    fun `hasIntentToSetup returns true for payment_intent with setup_future_usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
        )
        assertThat(metadata.hasIntentToSetup()).isTrue()
    }

    @Test
    fun `hasIntentToSetup returns false for payment_intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )
        assertThat(metadata.hasIntentToSetup()).isFalse()
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
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = false,
            )
        )
        val formElement = metadata.formElementsForCode(
            code = "bancontact",
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )!!

        val nameSection = formElement[0] as SectionElement
        val nameElement = nameSection.fields[0] as SimpleTextElement
        assertThat(nameElement.controller.label.first()).isEqualTo(CoreR.string.stripe_address_label_full_name)
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")

        val emailSection = formElement[1] as SectionElement
        val emailElement = emailSection.fields[0] as EmailElement
        assertThat(emailElement.controller.label.first()).isEqualTo(UiCoreR.string.stripe_email)
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")

        val phoneSection = formElement[2] as SectionElement
        val phoneElement = phoneSection.fields[0] as PhoneNumberElement
        assertThat(phoneElement.controller.label.first()).isEqualTo(CoreR.string.stripe_address_label_phone_number)
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")

        val addressSection = formElement[3] as SectionElement
        val addressElement = addressSection.fields[0] as AddressElement

        val identifiers = addressElement.fields.first().map { it.identifier }
        // Check that the address element contains country.
        assertThat(identifiers).contains(IdentifierSpec.Country)
    }

    @Test
    fun `formElementsForCode is constructed correctly for external payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "bancontact")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
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
        assertThat(nameElement.controller.label.first()).isEqualTo(CoreR.string.stripe_address_label_full_name)
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")

        val phoneSection = formElement[1] as SectionElement
        val phoneElement = phoneSection.fields[0] as PhoneNumberElement
        assertThat(phoneElement.controller.label.first()).isEqualTo(CoreR.string.stripe_address_label_phone_number)
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")

        val emailSection = formElement[2] as SectionElement
        val emailElement = emailSection.fields[0] as EmailElement
        assertThat(emailElement.controller.label.first()).isEqualTo(UiCoreR.string.stripe_email)
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
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
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
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
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
        assertThat(headerInformation.iconResource).isEqualTo(R.drawable.stripe_ic_paymentsheet_pm_bancontact)
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
        assertThat(headerInformation.iconResource).isEqualTo(0)
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
    fun `isExternalPaymentMethod returns false for non-external payment method`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

        assertThat(metadata.isExternalPaymentMethod("card")).isFalse()
    }

    @Suppress("LongMethod")
    @Test
    fun `should create metadata properly with elements session response, payment sheet config, and data specs`() {
        val billingDetailsCollectionConfiguration = createBillingDetailsCollectionConfiguration()
        val defaultBillingDetails = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(line1 = "123 Apple Street")
        )
        val shippingDetails = AddressDetails(address = PaymentSheet.Address(line1 = "123 Pear Street"))
        val linkInlineConfiguration = createLinkInlineConfiguration()
        val cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.allowed(
            listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex)
        )
        val customPaymentMethods = listOf(
            PaymentSheet.CustomPaymentMethod(
                id = "cpmt_123",
                subtitle = "Pay now".resolvableString,
                disableBillingDetailCollection = true,
            ),
            PaymentSheet.CustomPaymentMethod(
                id = "cpmt_456",
                subtitle = "Pay later".resolvableString,
                disableBillingDetailCollection = false,
            ),
            PaymentSheet.CustomPaymentMethod(
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
            linkInlineConfiguration = linkInlineConfiguration,
            linkState = null,
        )

        val expectedMetadata = PaymentMethodMetadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = true,
            allowsPaymentMethodsRequiringShippingAddress = false,
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
            customerMetadata = CustomerMetadata(
                hasCustomerConfiguration = true,
                isPaymentMethodSetAsDefaultEnabled = false,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            isGooglePayReady = false,
            linkConfiguration = PaymentSheet.LinkConfiguration(),
            linkInlineConfiguration = linkInlineConfiguration,
            linkMode = null,
            linkState = null,
            cardBrandFilter = PaymentSheetCardBrandFilter(cardBrandAcceptance),
            paymentMethodIncentive = null,
            elementsSessionId = "session_1234",
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full
        )

        assertThat(metadata).isEqualTo(expectedMetadata)
    }

    @Suppress("LongMethod")
    @Test
    fun `should create metadata properly with elements session response, customer sheet config, and data specs`() {
        val billingDetailsCollectionConfiguration = createBillingDetailsCollectionConfiguration()
        val defaultBillingDetails = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(line1 = "123 Apple Street")
        )
        val cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.allowed(
            listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex)
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
            isPaymentMethodSyncDefaultEnabled = false,
        )

        val expectedMetadata = PaymentMethodMetadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = true,
            allowsPaymentMethodsRequiringShippingAddress = false,
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
            customerMetadata = CustomerMetadata(
                hasCustomerConfiguration = true,
                isPaymentMethodSetAsDefaultEnabled = false,
            ),
            isGooglePayReady = true,
            paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
            linkConfiguration = PaymentSheet.LinkConfiguration(),
            linkInlineConfiguration = null,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
            linkMode = null,
            linkState = null,
            cardBrandFilter = PaymentSheetCardBrandFilter(cardBrandAcceptance),
            paymentMethodIncentive = null,
            elementsSessionId = "session_1234"
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
            linkInlineConfiguration = null,
            linkState = null,
        )
    }

    private fun createElementsSession(
        intent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        cardBrandChoice: ElementsSession.CardBrandChoice = ElementsSession.CardBrandChoice(
            eligible = true,
            preferredNetworks = listOf("cartes_bancaires")
        ),
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
            flags = emptyMap()
        )
    }

    @Test
    fun `allowRedisplay returns Unspecified when consent behavior is Legacy`() = runTest {
        val metadataForPaymentIntent = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
        )

        assertThat(
            metadataForPaymentIntent.allowRedisplay(PaymentSelection.CustomerRequestedSave.RequestReuse)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadataForPaymentIntent.allowRedisplay(PaymentSelection.CustomerRequestedSave.RequestNoReuse)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadataForPaymentIntent.allowRedisplay(PaymentSelection.CustomerRequestedSave.NoRequest)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        val metadataForSetupIntent = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
        )

        assertThat(
            metadataForSetupIntent.allowRedisplay(PaymentSelection.CustomerRequestedSave.RequestReuse)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadataForSetupIntent.allowRedisplay(PaymentSelection.CustomerRequestedSave.RequestNoReuse)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadataForSetupIntent.allowRedisplay(PaymentSelection.CustomerRequestedSave.NoRequest)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy
        )

        assertThat(
            metadataForPaymentIntentWithSfu.allowRedisplay(PaymentSelection.CustomerRequestedSave.RequestReuse)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadataForPaymentIntentWithSfu.allowRedisplay(PaymentSelection.CustomerRequestedSave.RequestNoReuse)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadataForPaymentIntentWithSfu.allowRedisplay(PaymentSelection.CustomerRequestedSave.NoRequest)
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)
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
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
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
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
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
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

            assertThat(
                metadataForSetupIntent.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
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
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

            assertThat(
                metadataForPaymentIntentWithSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
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
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
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
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

            assertThat(
                metadata.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
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

        assertThat(
            metadata.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadata.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

        assertThat(
            metadata.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)
    }

    @Test
    fun `allowRedisplay returns Limited when consent behavior is Disabled and setting up`() = runTest {
        val metadataForSetupIntent = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
        )

        assertThat(
            metadataForSetupIntent.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

        assertThat(
            metadataForSetupIntent.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

        assertThat(
            metadataForSetupIntent.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

        val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
        )

        assertThat(
            metadataForPaymentIntentWithSfu.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

        assertThat(
            metadataForPaymentIntentWithSfu.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)

        assertThat(
            metadataForPaymentIntentWithSfu.allowRedisplay(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
            )
        ).isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)
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

            assertThat(
                metadataForSetupIntent.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)

            assertThat(
                metadataForSetupIntent.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)

            assertThat(
                metadataForSetupIntent.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)

            val metadataForPaymentIntentWithSfu = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                    overrideAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
                ),
            )

            assertThat(
                metadataForPaymentIntentWithSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

            assertThat(
                metadataForPaymentIntentWithSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)

            assertThat(
                metadataForPaymentIntentWithSfu.allowRedisplay(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
                )
            ).isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)
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

    private fun createLinkInlineConfiguration(): LinkInlineConfiguration {
        return LinkInlineConfiguration(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            linkConfiguration = LinkConfiguration(
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
                passthroughModeEnabled = false,
                useAttestationEndpointsForLink = false,
                suppress2faModal = false,
                initializationMode = PaymentSheetFixtures.INITIALIZATION_MODE_PAYMENT_INTENT,
                elementsSessionId = "session_1234",
                linkMode = LinkMode.LinkPaymentMethod,
            ),
        )
    }

    private fun createBillingDetailsCollectionConfiguration() =
        PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )

    private fun createCustomerSheetConfiguration(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        defaultBillingDetails: PaymentSheet.BillingDetails,
        cardBrandAcceptance: PaymentSheet.CardBrandAcceptance
    ) = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant Inc.")
        .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
        .defaultBillingDetails(defaultBillingDetails)
        .preferredNetworks(listOf(CardBrand.CartesBancaires, CardBrand.Visa))
        .paymentMethodOrder(listOf("us_bank_account", "card", "sepa_debit"))
        .cardBrandAcceptance(cardBrandAcceptance)
        .build()

    private fun createPaymentSheetConfiguration(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        defaultBillingDetails: PaymentSheet.BillingDetails,
        shippingDetails: AddressDetails,
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        cardBrandAcceptance: PaymentSheet.CardBrandAcceptance
    ) = PaymentSheet.Configuration(
        merchantDisplayName = "Merchant Inc.",
        allowsDelayedPaymentMethods = true,
        allowsPaymentMethodsRequiringShippingAddress = false,
        paymentMethodOrder = listOf("us_bank_account", "card", "sepa_debit"),
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_1"),
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        preferredNetworks = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
        customPaymentMethods = customPaymentMethods,
        cardBrandAcceptance = cardBrandAcceptance
    )
}
