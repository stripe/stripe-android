package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AutomaticTaxBillingAddressSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Test

class BillingAddressSpecResolverTest {
    @Test
    fun `resolve replaces country with any API path in place and preserves allowed countries`() {
        val countrySpec = CountrySpec(
            apiPath = IdentifierSpec.Generic("server_country"),
            allowedCountryCodes = setOf("DE", "FR"),
        )
        val mandate = MandateTextSpec(stringResId = R.string.stripe_sepa_mandate)

        val result = resolve(
            specs = listOf(
                NameSpec(),
                countrySpec,
                mandate,
            ),
        )

        assertThat(result).containsExactly(
            NameSpec(),
            AutomaticTaxBillingAddressSpec(
                allowedCountryCodes = setOf("DE", "FR"),
            ),
            mandate,
        ).inOrder()
        assertThat(result.filterIsInstance<CountrySpec>()).isEmpty()
    }

    @Test
    fun `resolve replaces multiple countries with one automatic tax billing address`() {
        val result = resolve(
            specs = listOf(
                CountrySpec(allowedCountryCodes = setOf("US", "CA")),
                NameSpec(),
                CountrySpec(allowedCountryCodes = setOf("US")),
            ),
        )

        assertThat(result.filterIsInstance<CountrySpec>()).isEmpty()
        assertThat(result.filterIsInstance<AutomaticTaxBillingAddressSpec>()).hasSize(1)
    }

    @Test
    fun `resolve preserves normal address, exposes its country, and removes separate country`() {
        val normalAddress = AddressSpec(
            allowedCountryCodes = setOf("GB"),
            hideCountry = true,
        )
        val mandate = MandateTextSpec(stringResId = R.string.stripe_sepa_mandate)

        val result = resolve(
            specs = listOf(
                CountrySpec(apiPath = IdentifierSpec.Generic("server_country")),
                NameSpec(),
                normalAddress,
                mandate,
            ),
        )

        assertThat(result).containsExactly(
            NameSpec(),
            normalAddress.copy(hideCountry = false),
            mandate,
        ).inOrder()
        assertThat(result.filterIsInstance<CountrySpec>()).isEmpty()
        assertThat(result.filterIsInstance<AutomaticTaxBillingAddressSpec>()).isEmpty()
    }

    @Test
    fun `resolve preserves visible normal address without adding another address`() {
        val addressSpec = AddressSpec(allowedCountryCodes = setOf("GB"))
        val mandate = MandateTextSpec(stringResId = R.string.stripe_sepa_mandate)

        val result = resolve(
            specs = listOf(
                NameSpec(),
                addressSpec,
                mandate,
            ),
        )

        assertThat(result).containsExactly(NameSpec(), addressSpec, mandate).inOrder()
        assertThat(result.filterIsInstance<AutomaticTaxBillingAddressSpec>()).isEmpty()
    }

    @Test
    fun `resolve adds missing address after fields without mandate`() {
        val result = resolve(specs = listOf(NameSpec()))

        assertThat(result).containsExactly(
            NameSpec(),
            AutomaticTaxBillingAddressSpec(allowedCountryCodes = setOf("US", "CA")),
        ).inOrder()
    }

    @Test
    fun `resolve leaves country unchanged in Never`() {
        val countrySpec = CountrySpec(allowedCountryCodes = setOf("US"))

        val result = BillingAddressSpecResolver.resolve(
            specs = listOf(countrySpec),
            addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            requiresBillingAddressForAutomaticTax = true,
            allowedBillingCountries = setOf("US"),
        )

        assertThat(result).containsExactly(countrySpec)
    }

    @Test
    fun `resolve leaves country unchanged in tax-disabled Automatic`() {
        val countrySpec = CountrySpec(allowedCountryCodes = setOf("US"))

        val result = BillingAddressSpecResolver.resolve(
            specs = listOf(countrySpec),
            addressCollectionMode =
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            requiresBillingAddressForAutomaticTax = false,
            allowedBillingCountries = setOf("US"),
        )

        assertThat(result).containsExactly(countrySpec)
    }

    @Test
    fun `resolve replaces country with full address in Full`() {
        val result = BillingAddressSpecResolver.resolve(
            specs = listOf(CountrySpec(allowedCountryCodes = setOf("DE", "FR"))),
            addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            requiresBillingAddressForAutomaticTax = false,
            allowedBillingCountries = setOf("US"),
        )

        assertThat(result).containsExactly(AddressSpec(allowedCountryCodes = setOf("DE", "FR")))
    }

    private fun resolve(
        specs: List<FormItemSpec>,
    ): List<FormItemSpec> {
        return BillingAddressSpecResolver.resolve(
            specs = specs,
            addressCollectionMode =
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            requiresBillingAddressForAutomaticTax = true,
            allowedBillingCountries = setOf("US", "CA"),
        )
    }
}
