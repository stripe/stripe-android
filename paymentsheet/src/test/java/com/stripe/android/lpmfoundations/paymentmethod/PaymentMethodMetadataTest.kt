package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AffirmDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.Amount
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
            displayName = resolvableString("PayPal"),
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
}
