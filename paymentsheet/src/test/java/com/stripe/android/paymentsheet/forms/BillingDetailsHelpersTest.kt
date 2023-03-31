package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.forms.BillingDetailsHelpers.Companion.removeCorrespondingPlaceholder
import com.stripe.android.paymentsheet.forms.BillingDetailsHelpers.Companion.specForPlaceholderField
import com.stripe.android.paymentsheet.forms.BillingDetailsHelpers.Companion.specsForConfiguration
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec.PlaceholderField
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BillingDetailsHelpersTest {
    @Test
    fun `Test unused elements are removed`() = runBlocking {
        val billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = false,
        )

        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
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
    fun `Test placeholders are not added in Automatic collection mode`() = runBlocking {
        val billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            attachDefaultsToPaymentMethod = false,
        )

        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
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
    fun `Test billing details elements are added where they should`(): Unit = runBlocking {
        val billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        val specs = specsForConfiguration(
            configuration = billingDetailsCollectionConfiguration,
            specs = listOf(
                NameSpec(),
                PlaceholderSpec(
                    field = PlaceholderSpec.PlaceholderField.Email,
                ),
                SimpleTextSpec(
                    apiPath = IdentifierSpec.Generic("dummy"),
                    label = R.string.affirm_buy_now_pay_later,
                ),
            ),
        )

        // Email should replace the placeholder, phone and address should be added at the end.
        assertThat(specs).containsExactly(
            NameSpec(),
            EmailSpec(),
            SimpleTextSpec(
                apiPath = IdentifierSpec.Generic("dummy"),
                label = R.string.affirm_buy_now_pay_later,
            ),
            PhoneSpec(),
            AddressSpec(),
        )
    }

    @Test
    fun `Test correct spec is returned for placeholder fields`() = runBlocking {
        var billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        assertThat(
            specForPlaceholderField(
                PlaceholderField.Name,
                billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(NameSpec())
        assertThat(
            specForPlaceholderField(
                PlaceholderField.Email,
                billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(EmailSpec())
        assertThat(
            specForPlaceholderField(
                PlaceholderField.Phone,
                billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(PhoneSpec())
        assertThat(
            specForPlaceholderField(
                PlaceholderField.BillingAddress,
                billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(AddressSpec())
        assertThat(
            specForPlaceholderField(
                PlaceholderField.BillingAddressWithoutCountry,
                billingDetailsCollectionConfiguration,
            )
        ).isEqualTo(AddressSpec(hideCountry = true))

        billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = false,
        )

        assertThat(
            specForPlaceholderField(
                PlaceholderField.Name,
                billingDetailsCollectionConfiguration,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                PlaceholderField.Email,
                billingDetailsCollectionConfiguration,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                PlaceholderField.Phone,
                billingDetailsCollectionConfiguration,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                PlaceholderField.BillingAddress,
                billingDetailsCollectionConfiguration,
            )
        ).isNull()
        assertThat(
            specForPlaceholderField(
                PlaceholderField.BillingAddressWithoutCountry,
                billingDetailsCollectionConfiguration,
            )
        ).isNull()
    }

    @Test
    fun `Test correct placeholder is removed`(): Unit = runBlocking {
        var placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, NameSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Email,
            PlaceholderField.Phone,
            PlaceholderField.BillingAddress,
        )

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(placeholders, PlaceholderSpec(field = PlaceholderField.Name))
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
        removeCorrespondingPlaceholder(placeholders, PhoneSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
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
        removeCorrespondingPlaceholder(placeholders, AddressSpec())
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
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

        placeholders = basePlaceholders()
        removeCorrespondingPlaceholder(
            placeholders,
            PlaceholderSpec(field = PlaceholderField.BillingAddressWithoutCountry)
        )
        assertThat(placeholders).containsExactly(
            PlaceholderField.Name,
            PlaceholderField.Email,
            PlaceholderField.Phone,
        )
    }

    private fun basePlaceholders() = mutableListOf(
        PlaceholderField.Name,
        PlaceholderField.Email,
        PlaceholderField.Phone,
        PlaceholderField.BillingAddress,
    )
}
