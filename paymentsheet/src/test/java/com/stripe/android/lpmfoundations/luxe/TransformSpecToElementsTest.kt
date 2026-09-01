package com.stripe.android.lpmfoundations.luxe

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR

@RunWith(RobolectricTestRunner::class)
internal class TransformSpecToElementsTest {

    private val nameSection = NameSpec()

    private val emailSection = EmailSpec()

    private lateinit var transformSpecToElements: TransformSpecToElements

    @Before
    fun beforeTest() {
        transformSpecToElements = TransformSpecToElementsFactory.create()
    }

    @Test
    fun `Add a name section spec sets up the name element correctly`() = runBlocking {
        val formElement = transformSpecToElements.transform(
            specs = listOf(nameSection),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        val nameElement = (formElement.first() as SectionElement)
            .fields[0] as SimpleTextElement

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label.first()).isEqualTo(NameConfig().label)
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")

        assertThat(nameElement.controller.capitalization).isEqualTo(
            KeyboardCapitalization.Words
        )
        assertThat(nameElement.controller.keyboardType).isEqualTo(
            androidx.compose.ui.text.input.KeyboardType.Text
        )
    }

    @Test
    fun `Add a email section spec sets up the email element correctly`() = runBlocking {
        val formElement = transformSpecToElements.transform(
            specs = listOf(emailSection),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        val emailSectionElement = formElement.first() as SectionElement
        val emailElement = emailSectionElement.fields[0] as EmailElement

        // Verify the correct config is setup for the controller
        assertThat(emailElement.controller.label.first()).isEqualTo(EmailConfig().label)
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")
    }

    @Test
    fun `Add a phone section spec sets up the phone element correctly`() = runBlocking {
        val phoneSpec = PhoneSpec()
        val formElement = transformSpecToElements.transform(
            specs = listOf(phoneSpec),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        val phoneSectionElement = formElement.first() as SectionElement
        val phoneElement = phoneSectionElement.fields[0] as PhoneNumberElement

        // Verify the correct config is setup for the controller
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `Address placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.BillingAddress,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            )
        ).transform(
            specs = listOf(placeholderSpec),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val addressElement = sectionElement.fields.first() as AddressElement
        assertThat(addressElement.identifier.v1).isEqualTo("billing_details[address]")
    }

    @Test
    fun `Phone placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.Phone,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        ).transform(
            specs = listOf(placeholderSpec),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val phoneNumberElement = sectionElement.fields.first() as PhoneNumberElement
        assertThat(phoneNumberElement.identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `Name placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.Name,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        ).transform(
            specs = listOf(placeholderSpec),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val nameElement = sectionElement.fields.first() as SimpleTextElement
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")
    }

    @Test
    fun `Email placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.Email,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        ).transform(
            specs = listOf(placeholderSpec),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val emailElement = sectionElement.fields.first() as EmailElement
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")
    }

    @Test
    fun `AutocompleteAddressElement should be used if a factory is provided`() = runTest {
        val formElement = TransformSpecToElementsFactory.create(
            requiresMandate = false,
            autocompleteAddressInteractorFactory = {
                TestAutocompleteAddressInteractor.noOp()
            }
        ).transform(
            specs = listOf(AddressSpec()),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(formElement).hasSize(1)
        assertThat(formElement.firstOrNull()).isInstanceOf<SectionElement>()

        val sectionElement = formElement.first() as SectionElement

        assertThat(sectionElement.fields.size).isEqualTo(1)
        assertThat(sectionElement.fields.firstOrNull()).isInstanceOf<AutocompleteAddressElement>()
    }
}

private object TransformSpecToElementsFactory {
    fun create(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        requiresMandate: Boolean = false,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
    ): TransformSpecToElements {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            StripeR.style.StripeDefaultTheme
        )

        return TransformSpecToElements(
            UiDefinitionFactory.Arguments(
                coroutineScope = CoroutineScope(Dispatchers.Unconfined),
                initialValues = mapOf(),
                initialLinkUserInput = null,
                saveForFutureUseInitialValue = true,
                merchantName = "Merchant, Inc.",
                cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
                shippingValues = null,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                requiresBillingAddressForAutomaticTax = false,
                requiresMandate = requiresMandate,
                linkConfigurationCoordinator = null,
                onLinkInlineSignupStateChanged = { throw AssertionError("Not implemented") },
                cardBrandFilter = DefaultCardBrandFilter,
                cardFundingFilter = DefaultCardFundingFilter,
                setAsDefaultMatchesSaveForFutureUse = false,
                autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                linkInlineHandler = null,
            )
        )
    }
}
