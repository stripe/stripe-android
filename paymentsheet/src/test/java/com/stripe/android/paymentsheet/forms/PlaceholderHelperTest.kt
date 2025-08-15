package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.removeCorrespondingPlaceholder
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specForPlaceholderField
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specsForConfiguration
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.CashAppPayMandateTextSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec.PlaceholderField
import com.stripe.android.ui.core.elements.SepaMandateTextSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR

@RunWith(RobolectricTestRunner::class)
class PlaceholderHelperTest {
    @Test
    fun `Test unused elements are removed`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = false,
        )

        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
            placeholderOverrideList = emptyList(),
            requiresMandate = false,
            specs = listOf(
                NameSpec(),
                EmailSpec(),
                PhoneSpec(),
                AddressSpec(),
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )
        assertThat(specs).isEmpty()
    }

    @Test
    fun `Test placeholders are not added in Automatic collection mode`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            attachDefaultsToPaymentMethod = false,
        )

        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
            placeholderOverrideList = emptyList(),
            requiresMandate = false,
            specs = listOf(
                PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.Name),
                PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.Email),
                PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.Phone),
                PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.BillingAddress),
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )
        assertThat(specs).isEmpty()
    }

    @Test
    fun `Test billing details elements are added where they should`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
            placeholderOverrideList = emptyList(),
            requiresMandate = false,
            specs = listOf(
                NameSpec(),
                PlaceholderSpec(
                    field = PlaceholderSpec.PlaceholderField.Email,
                ),
                SimpleTextSpec(
                    apiPath = IdentifierSpec.Generic("dummy"),
                    label = StripeR.string.stripe_affirm_buy_now_pay_later,
                ),
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        // Email should replace the placeholder, phone and address should be added at the end.
        assertThat(specs).containsExactly(
            NameSpec(),
            EmailSpec(),
            SimpleTextSpec(
                apiPath = IdentifierSpec.Generic("dummy"),
                label = StripeR.string.stripe_affirm_buy_now_pay_later,
            ),
            PhoneSpec(),
            AddressSpec(allowedCountryCodes = emptySet()),
        )
    }

    @Test
    fun `Test when requiresMandate is true, SepaMandateSpec is only added when specified`() {
        val specs = specsForConfiguration(
            configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                NameSpec(),
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(specs).containsExactly(
            NameSpec(),
        )

        val specsWithSepa = specsForConfiguration(
            configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                NameSpec(),
                SepaMandateTextSpec()
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(specsWithSepa).containsExactly(
            NameSpec(),
            SepaMandateTextSpec()
        )

        val specsWithSepaPlaceholder = specsForConfiguration(
            configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                NameSpec(),
                PlaceholderSpec(
                    field = PlaceholderSpec.PlaceholderField.SepaMandate,
                )
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(specsWithSepaPlaceholder).containsExactly(
            NameSpec(),
            SepaMandateTextSpec()
        )
    }

    @Test
    fun `Test when termsDisplay=NEVER, SepaMandateSpec is not added`() {
        val specsWithSepa = specsForConfiguration(
            configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                NameSpec(),
                SepaMandateTextSpec()
            ),
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        )

        assertThat(specsWithSepa).containsExactly(
            NameSpec(),
        )

        val specsWithSepaPlaceholder = specsForConfiguration(
            configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                NameSpec(),
                PlaceholderSpec(
                    field = PlaceholderSpec.PlaceholderField.SepaMandate,
                )
            ),
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        )

        assertThat(specsWithSepaPlaceholder).containsExactly(
            NameSpec(),
        )
    }

    @Test
    fun `Test when termsDisplay=NEVER, AuBecsDebitMandateTextSpec is not added`() {
        val specs = specsForConfiguration(
            configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                NameSpec(),
                AuBecsDebitMandateTextSpec()
            ),
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        )

        assertThat(specs).containsExactly(
            NameSpec(),
        )
    }

    @Test
    fun `Test when termsDisplay=AUTOMATIC, AuBecsDebitMandateTextSpec is retained`() {
        val specs = specsForConfiguration(
            configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                NameSpec(),
                AuBecsDebitMandateTextSpec(),
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(specs).containsExactly(
            NameSpec(),
            AuBecsDebitMandateTextSpec(),
        )
    }

    @Test
    fun `Test correct spec is returned for placeholder fields`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Name,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(NameSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(EmailSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(PhoneSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(AddressSpec(allowedCountryCodes = emptySet()),)
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddressWithoutCountry,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(AddressSpec(allowedCountryCodes = emptySet(), hideCountry = true))
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.SepaMandate,
                placeholderOverrideList = emptyList(),
                requiresMandate = true,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(SepaMandateTextSpec())
    }

    @Test
    fun `Test null specs returned when not collecting field`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = false,
        )

        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Name,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddressWithoutCountry,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
    }

    @Test
    fun `Test correct placeholder is removed for normal spec`() {
        var placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, NameSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, EmailSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, PhoneSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.BillingAddress,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, AddressSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, SepaMandateTextSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
        )
    }

    @Test
    fun `Test correct placeholder is removed for placeholder spec`() {
        var placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, PlaceholderSpec(field = PlaceholderField.Name))
        assertThat(placeholders).containsExactly(
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(
            placeholders,
            PlaceholderSpec(field = PlaceholderField.Email)
        )
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(
            placeholders,
            PlaceholderSpec(field = PlaceholderField.Phone)
        )
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.BillingAddress,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(
            placeholders,
            PlaceholderSpec(field = PlaceholderField.BillingAddress)
        )
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(
            placeholders,
            PlaceholderSpec(field = PlaceholderField.BillingAddressWithoutCountry)
        )
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.SepaMandate,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(
            placeholders,
            PlaceholderSpec(field = PlaceholderField.SepaMandate)
        )
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
        )
    }

    @Test
    fun `Test requires mandate`() {
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.SepaMandate,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.SepaMandate,
                placeholderOverrideList = emptyList(),
                requiresMandate = true,
                configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isInstanceOf<
            SepaMandateTextSpec
            >()
    }

    @Test
    fun `Test overrideable placeholders`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            attachDefaultsToPaymentMethod = false,
        )

        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Name,
                placeholderOverrideList = listOf(IdentifierSpec.Name),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isInstanceOf<
            NameSpec
            >()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = listOf(IdentifierSpec.Email),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isInstanceOf<
            EmailSpec
            >()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = listOf(IdentifierSpec.Phone),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isInstanceOf<
            PhoneSpec
            >()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = listOf(IdentifierSpec.Generic("billing_details[address]")),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isInstanceOf<
            AddressSpec
            >()
    }

    @Test
    fun `Test with empty set of allowed billing countries`() {
        val billingDetailsCollectionConfigurationWithAllCountries =
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                attachDefaultsToPaymentMethod = false,
                allowedCountries = emptySet()
            )

        val placeholderSpecForBillingAddress = specForPlaceholderField(
            field = PlaceholderField.BillingAddress,
            placeholderOverrideList = listOf(IdentifierSpec.BillingAddress),
            requiresMandate = false,
            configuration = billingDetailsCollectionConfigurationWithAllCountries,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(placeholderSpecForBillingAddress).isInstanceOf<AddressSpec>()

        val addressSpec = placeholderSpecForBillingAddress as AddressSpec

        assertThat(addressSpec.allowedCountryCodes).isEmpty()

        val placeholderSpecForBillingAddressWithoutCountry = specForPlaceholderField(
            field = PlaceholderField.BillingAddressWithoutCountry,
            placeholderOverrideList = listOf(IdentifierSpec.BillingAddress),
            requiresMandate = false,
            configuration = billingDetailsCollectionConfigurationWithAllCountries,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(placeholderSpecForBillingAddressWithoutCountry).isInstanceOf<AddressSpec>()

        val addressSpecWithoutCountry = placeholderSpecForBillingAddressWithoutCountry as AddressSpec

        assertThat(addressSpecWithoutCountry.allowedCountryCodes).isEmpty()
    }

    @Test
    fun `Test with limited set of allowed billing countries`() {
        val billingDetailsCollectionConfigurationWithAllCountries =
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                attachDefaultsToPaymentMethod = false,
                allowedCountries = setOf("US", "CA"),
            )

        val placeholderSpecForBillingAddress = specForPlaceholderField(
            field = PlaceholderField.BillingAddress,
            placeholderOverrideList = listOf(IdentifierSpec.BillingAddress),
            requiresMandate = false,
            configuration = billingDetailsCollectionConfigurationWithAllCountries,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(placeholderSpecForBillingAddress).isInstanceOf<AddressSpec>()

        val addressSpec = placeholderSpecForBillingAddress as AddressSpec

        assertThat(addressSpec.allowedCountryCodes).containsExactly("US", "CA")

        val placeholderSpecForBillingAddressWithoutCountry = specForPlaceholderField(
            field = PlaceholderField.BillingAddressWithoutCountry,
            placeholderOverrideList = listOf(IdentifierSpec.BillingAddress),
            requiresMandate = false,
            configuration = billingDetailsCollectionConfigurationWithAllCountries,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(placeholderSpecForBillingAddressWithoutCountry).isInstanceOf<AddressSpec>()

        val addressSpecWithoutCountry = placeholderSpecForBillingAddressWithoutCountry as AddressSpec

        assertThat(addressSpecWithoutCountry.allowedCountryCodes).containsExactly("US", "CA")
    }

    @Test
    fun `Test mandate is moved to the end of the list`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        val exampleTextSpec = SimpleTextSpec(
            apiPath = IdentifierSpec.Generic("dummy"),
            label = StripeR.string.stripe_affirm_buy_now_pay_later,
        )
        val mandateTextSpec = MandateTextSpec(stringResId = 0)
        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                exampleTextSpec,
                mandateTextSpec,
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(specs).containsExactly(
            exampleTextSpec,
            NameSpec(),
            EmailSpec(),
            PhoneSpec(),
            AddressSpec(allowedCountryCodes = emptySet()),
            mandateTextSpec
        )
    }

    @Test
    fun `Test cashapp mandate is moved to the end of the list`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        val exampleTextSpec = SimpleTextSpec(
            apiPath = IdentifierSpec.Generic("dummy"),
            label = StripeR.string.stripe_affirm_buy_now_pay_later,
        )
        val mandateTextSpec = CashAppPayMandateTextSpec()
        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
            placeholderOverrideList = emptyList(),
            requiresMandate = true,
            specs = listOf(
                exampleTextSpec,
                mandateTextSpec,
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(specs).containsExactly(
            exampleTextSpec,
            NameSpec(),
            EmailSpec(),
            PhoneSpec(),
            AddressSpec(allowedCountryCodes = emptySet()),
            mandateTextSpec
        )
    }

    private fun basePlaceholders() = mutableListOf(
        PlaceholderField.Name,
        PlaceholderField.Email,
        PlaceholderField.Phone,
        PlaceholderField.BillingAddress,
        PlaceholderField.SepaMandate,
    )
}
