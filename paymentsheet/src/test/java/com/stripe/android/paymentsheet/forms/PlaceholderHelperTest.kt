package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.removeCorrespondingPlaceholder
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specForPlaceholderField
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specsForConfiguration
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec.PlaceholderField
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
            specs = listOf(
                NameSpec(),
                PlaceholderSpec(
                    field = PlaceholderSpec.PlaceholderField.Email,
                ),
            ),
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        // Email should replace the placeholder, phone and address should be added at the end.
        assertThat(specs).containsExactly(
            NameSpec(),
            EmailSpec(),
            PhoneSpec(),
            AddressSpec(allowedCountryCodes = emptySet()),
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
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(NameSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = emptyList(),
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(EmailSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = emptyList(),
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(PhoneSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = emptyList(),
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isEqualTo(AddressSpec(allowedCountryCodes = emptySet()),)
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
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = emptyList(),
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = emptyList(),
                configuration = billingDetailsCollectionConfiguration,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = emptyList(),
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
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, EmailSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, PhoneSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.BillingAddress,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, AddressSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
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
        )
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
            configuration = billingDetailsCollectionConfigurationWithAllCountries,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(placeholderSpecForBillingAddress).isInstanceOf<AddressSpec>()

        val addressSpec = placeholderSpecForBillingAddress as AddressSpec

        assertThat(addressSpec.allowedCountryCodes).isEmpty()
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
            configuration = billingDetailsCollectionConfigurationWithAllCountries,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        )

        assertThat(placeholderSpecForBillingAddress).isInstanceOf<AddressSpec>()

        val addressSpec = placeholderSpecForBillingAddress as AddressSpec

        assertThat(addressSpec.allowedCountryCodes).containsExactly("US", "CA")
    }

    private fun basePlaceholders() = mutableListOf(
        PlaceholderField.Name,
        PlaceholderField.Email,
        PlaceholderField.Phone,
        PlaceholderField.BillingAddress,
    )
}
