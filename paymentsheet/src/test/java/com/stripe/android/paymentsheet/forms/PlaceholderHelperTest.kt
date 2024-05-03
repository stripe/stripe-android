package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.removeCorrespondingPlaceholder
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specForPlaceholderField
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specsForConfiguration
import com.stripe.android.ui.core.elements.AddressSpec
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
            AddressSpec(),
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
        )

        assertThat(specsWithSepaPlaceholder).containsExactly(
            NameSpec(),
            SepaMandateTextSpec()
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
            )
        ).isEqualTo(NameSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(EmailSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(PhoneSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(AddressSpec())
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddressWithoutCountry,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(AddressSpec(hideCountry = true))
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.SepaMandate,
                placeholderOverrideList = emptyList(),
                requiresMandate = true,
                configuration = billingDetailsCollectionConfiguration,
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
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddressWithoutCountry,
                placeholderOverrideList = emptyList(),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
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
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.SepaMandate,
                placeholderOverrideList = emptyList(),
                requiresMandate = true,
                configuration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            )
        ).isInstanceOf(
            SepaMandateTextSpec::class.java
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
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isInstanceOf(
            NameSpec::class.java
        )
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Email,
                placeholderOverrideList = listOf(IdentifierSpec.Email),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isInstanceOf(
            EmailSpec::class.java
        )
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.Phone,
                placeholderOverrideList = listOf(IdentifierSpec.Phone),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isInstanceOf(
            PhoneSpec::class.java
        )
        assertThat(
            specForPlaceholderField(
                field = PlaceholderField.BillingAddress,
                placeholderOverrideList = listOf(IdentifierSpec.Generic("billing_details[address]")),
                requiresMandate = false,
                configuration = billingDetailsCollectionConfiguration,
            )
        ).isInstanceOf(
            AddressSpec::class.java
        )
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
        )

        assertThat(specs).containsExactly(
            exampleTextSpec,
            NameSpec(),
            EmailSpec(),
            PhoneSpec(),
            AddressSpec(),
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
        )

        assertThat(specs).containsExactly(
            exampleTextSpec,
            NameSpec(),
            EmailSpec(),
            PhoneSpec(),
            AddressSpec(),
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
