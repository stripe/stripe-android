package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.PaymentMethodMessageHeaderElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.filterOutHiddenIdentifiers
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KlarnaDefinitionTest {
    @Test
    fun `createFormElements returns header, email, and country elements for PaymentIntent`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(3)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkCountryField(formElements, 2)
    }

    @Test
    fun `createFormElements preserves country-only form for automatic address collection`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf(
                        PaymentMethod.Type.Card.code,
                        PaymentMethod.Type.Klarna.code,
                    ),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                    allowedCountries = setOf("DE", "FR"),
                ),
                defaultBillingDetails = PaymentSheet.BillingDetails(
                    address = PaymentSheet.Address(country = "DE"),
                ),
            ),
        )

        assertThat(formElements).hasSize(3)
        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)

        val countrySection = formElements[2] as SectionElement
        assertThat(countrySection.identifier).isEqualTo(
            IdentifierSpec.Generic("billing_details[address][country]_section"),
        )
        val countryElement = countrySection.fields.single() as CountryElement
        assertThat(countryElement.identifier).isEqualTo(IdentifierSpec.Country)
        assertThat(countryElement.controller.displayItems).containsExactly(
            "🇫🇷 France",
            "🇩🇪 Germany",
        ).inOrder()
        assertThat(countryElement.controller.rawFieldValue.value).isEqualTo("DE")
        assertThat(
            formElements.filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<CountryElement>(),
        ).hasSize(1)
        assertThat(countrySection.fields.map { it.identifier }).containsExactly(IdentifierSpec.Country)
    }

    @Test
    fun `createFormElements promotes country to automatic tax billing address`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = automaticTaxMetadata(),
        )

        val billingAddressElements = formElements.filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()
        val standaloneCountryElements = formElements.filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<CountryElement>()

        assertThat(billingAddressElements).hasSize(1)
        assertThat(standaloneCountryElements).isEmpty()
        assertThat(billingAddressElements.single().countryElement.controller.rawFieldValue.value).isEqualTo("US")
        assertThat(billingAddressElements.single().shownIdentifierParamPaths()).containsExactly(
            IdentifierSpec.Country.v1,
            IdentifierSpec.Line1.v1,
            IdentifierSpec.City.v1,
            IdentifierSpec.PostalCode.v1,
            IdentifierSpec.State.v1,
        )
    }

    @Test
    fun `createFormElements preserves same as shipping through Klarna production path`() {
        val metadata = automaticTaxMetadata().copy(
            defaultBillingDetails = null,
            shippingDetails = AddressDetails(
                address = PaymentSheet.Address(country = "US"),
                isCheckboxSelected = true,
            ),
        )

        val sameAsShippingElement = KlarnaDefinition.formElements(metadata)
            .filterIsInstance<SameAsShippingElement>()
            .single()

        assertThat(sameAsShippingElement.controller.value.value).isTrue()
    }

    @Test
    fun `createFormElements omits same as shipping when default billing takes precedence`() {
        val metadata = automaticTaxMetadata().copy(
            shippingDetails = AddressDetails(
                address = PaymentSheet.Address(country = "CA"),
                isCheckboxSelected = true,
            ),
        )

        val sameAsShippingElements = KlarnaDefinition.formElements(metadata)
            .filterIsInstance<SameAsShippingElement>()

        assertThat(sameAsShippingElements).isEmpty()
    }

    @Test
    fun `createFormElements orders automatic tax address before conditional mandate`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf(PaymentMethod.Type.Klarna.code),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                allowedCountries = setOf("US", "CA"),
            ),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
        )

        val formElements = KlarnaDefinition.formElements(metadata)

        assertThat(formElements).hasSize(4)
        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        val addressSection = formElements[2] as SectionElement
        assertThat(addressSection.fields.single()).isInstanceOf<BillingAddressElement>()
        checkMandateField(formElements, metadata, 3)
    }

    @Test
    fun `completed automatic tax billing address serializes to billing details`() = runTest {
        val metadata = automaticTaxMetadata()
        val billingAddressElement = KlarnaDefinition.formElements(metadata)
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()
            .single()

        billingAddressElement.setRawValue(
            mapOf(
                IdentifierSpec.Country to "US",
                IdentifierSpec.Line1 to "510 Townsend St",
                IdentifierSpec.City to "San Francisco",
                IdentifierSpec.State to "CA",
                IdentifierSpec.PostalCode to "94103",
            ),
        )
        advanceUntilIdle()
        val visibleValues = billingAddressElement.getFormFieldValueFlow().value
            .toMap()
            .filterKeys { it !in billingAddressElement.hiddenIdentifiers.value }
        val params = FormFieldValues(
            fieldValuePairs = visibleValues,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
        ).transformToPaymentMethodCreateParams(
            paymentMethodCode = PaymentMethod.Type.Klarna.code,
            paymentMethodMetadata = metadata,
        )

        assertThat(visibleValues.values.all { it.isComplete }).isTrue()
        assertThat(params.billingDetails?.address?.country).isEqualTo("US")
        assertThat(params.billingDetails?.address?.line1).isEqualTo("510 Townsend St")
        assertThat(params.billingDetails?.address?.city).isEqualTo("San Francisco")
        assertThat(params.billingDetails?.address?.state).isEqualTo("CA")
        assertThat(params.billingDetails?.address?.postalCode).isEqualTo("94103")
    }

    @Test
    fun `createFormElements returns header, email, country, and mandate elements for SetupIntent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("klarna")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
        )

        val formElements = KlarnaDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(4)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkCountryField(formElements, 2)
        checkMandateField(formElements, metadata, 3)
    }

    @Test
    fun `createFormElements does not return mandate when termsDisplay is NEVER`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("klarna")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
            termsDisplay = mapOf(
                PaymentMethod.Type.Klarna to PaymentSheet.TermsDisplay.NEVER
            )
        )

        val formElements = KlarnaDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(3)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkCountryField(formElements, 2)
    }

    @Test
    fun `createFormElements includes name when requested`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkKlarnaHeaderText(formElements, 0)
        checkNameField(formElements, 1)
        checkEmailField(formElements, 2)
        checkCountryField(formElements, 3)
    }

    @Test
    fun `createFormElements includes phone when requested`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkCountryField(formElements, 3)
    }

    @Test
    fun `createFormElements uses one address owner when full address collection is enabled`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(3)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkBillingField(formElements, 2)
        assertThat(
            formElements.filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<CountryElement>(),
        ).isEmpty()
    }

    @Test
    fun `createFormElements with all billing fields and mandate for SetupIntent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("klarna")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )

        val formElements = KlarnaDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(6)

        checkKlarnaHeaderText(formElements, 0)
        checkNameField(formElements, 1)
        checkEmailField(formElements, 2)
        checkPhoneField(formElements, 3)
        checkBillingField(formElements, 4)
        checkMandateField(formElements, metadata, 5)
    }

    @Test
    fun `createFormElements includes promotion if available`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
            ),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(
                promotions = listOf(
                    PaymentMethodMessagePromotion(
                        paymentMethodType = "Klarna",
                        message = "This is a promotion",
                        learnMore = PaymentMethodMessageLearnMore(
                            url = "https://test.com",
                            message = "Click me."
                        )
                    )
                )
            )
        )

        assertThat(formElements).hasSize(3)

        val element = formElements[0]
        assertThat(element.identifier.v1).isEqualTo("klarna_promotion")
        assertThat(element).isInstanceOf<PaymentMethodMessageHeaderElement>()
        val headerElement = element as PaymentMethodMessageHeaderElement
        assertThat(headerElement.promotion.message).isEqualTo("This is a promotion")
        assertThat(headerElement.promotion.paymentMethodType).isEqualTo("Klarna")
        assertThat(headerElement.promotion.learnMore).isEqualTo(
            PaymentMethodMessageLearnMore(
                url = "https://test.com",
                message = "Click me."
            )
        )
    }

    private fun checkKlarnaHeaderText(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element).isInstanceOf<StaticTextElement>()
        assertThat(element.identifier.v1).isEqualTo("klarna_header_text")

        val textElement = element as StaticTextElement

        assertThat(textElement.text).isEqualTo(R.string.stripe_klarna_buy_now_pay_later.resolvableString)
    }

    private fun checkCountryField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element.identifier.v1).isEqualTo("billing_details[address][country]_section")
        assertThat(element).isInstanceOf<SectionElement>()

        val countrySection = element as SectionElement

        assertThat(countrySection.fields).hasSize(1)
        assertThat(countrySection.fields[0]).isInstanceOf<CountryElement>()
    }

    private fun checkMandateField(
        formElements: List<FormElement>,
        metadata: PaymentMethodMetadata,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element).isInstanceOf<MandateTextElement>()

        val mandateElement = element as MandateTextElement

        assertThat(mandateElement.stringResId).isEqualTo(R.string.stripe_klarna_mandate)
        assertThat(mandateElement.args).isEqualTo(listOf(metadata.merchantName, metadata.merchantName))
    }

    private fun automaticTaxMetadata(): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf(PaymentMethod.Type.Klarna.code),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                allowedCountries = setOf("US", "CA"),
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(country = "US"),
            ),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
        )
    }

    private fun BillingAddressElement.shownIdentifierParamPaths(): List<String> {
        return addressController.value.fieldsFlowable.value
            .filterOutHiddenIdentifiers(hiddenIdentifiers.value)
            .flatMap { field ->
                when (field) {
                    is RowElement -> field.fields.map { it.identifier.v1 }
                    else -> listOf(field.identifier.v1)
                }
            }
    }
}
