package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import org.junit.Test

class AutomaticTaxBillingAddressResolverTest {
    @Test
    fun `resolve replaces canonical country in place and preserves allowed countries`() {
        val result = resolve(
            specs = listOf(
                NameSpec(),
                CountrySpec(allowedCountryCodes = setOf("DE", "FR")),
                mandateSpec(),
            )
        )

        assertThat(result[0]).isInstanceOf(ResolvedFormItem.ExistingSpec::class.java)
        assertThat(result[1]).isEqualTo(
            ResolvedFormItem.BillingAddressSpec(
                allowedCountryCodes = setOf("DE", "FR"),
            )
        )
        assertThat(result[2]).isInstanceOf(ResolvedFormItem.ExistingSpec::class.java)
    }

    @Test
    fun `resolve inserts missing address before mandate`() {
        val result = resolve(
            specs = listOf(
                NameSpec(),
                mandateSpec(),
            )
        )

        assertThat(result.map { it::class.java }).containsExactly(
            ResolvedFormItem.ExistingSpec::class.java,
            ResolvedFormItem.BillingAddressSpec::class.java,
            ResolvedFormItem.ExistingSpec::class.java,
        ).inOrder()
    }

    @Test
    fun `resolve preserves a normal address without adding another address`() {
        val addressSpec = AddressSpec(allowedCountryCodes = setOf("GB"))

        val result = resolve(
            specs = listOf(
                NameSpec(),
                addressSpec,
                mandateSpec(),
            )
        )

        assertThat(result.filterIsInstance<ResolvedFormItem.BillingAddressSpec>()).isEmpty()
        assertThat(result.filterIsInstance<ResolvedFormItem.ExistingSpec>().map { it.spec })
            .containsExactly(NameSpec(), addressSpec, mandateSpec())
            .inOrder()
    }

    @Test
    fun `resolve removes duplicate canonical country beside normal address`() {
        val addressSpec = AddressSpec(allowedCountryCodes = setOf("GB"))

        val result = resolve(
            specs = listOf(
                CountrySpec(allowedCountryCodes = setOf("GB")),
                addressSpec,
            )
        )

        assertThat(result).containsExactly(ResolvedFormItem.ExistingSpec(addressSpec))
    }

    @Test
    fun `resolve leaves specs unchanged when automatic tax billing collection is disabled`() {
        val countrySpec = CountrySpec(allowedCountryCodes = setOf("US"))

        val result = AutomaticTaxBillingAddressResolver.resolve(
            specs = listOf(countrySpec),
            addressCollectionMode =
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            requiresBillingAddressForAutomaticTax = false,
            allowedBillingCountries = setOf("US"),
        )

        assertThat(result).containsExactly(ResolvedFormItem.ExistingSpec(countrySpec))
    }

    private fun resolve(
        specs: List<com.stripe.android.ui.core.elements.FormItemSpec>,
    ): List<ResolvedFormItem> {
        return AutomaticTaxBillingAddressResolver.resolve(
            specs = specs,
            addressCollectionMode =
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            requiresBillingAddressForAutomaticTax = true,
            allowedBillingCountries = setOf("US", "CA"),
        )
    }

    private fun mandateSpec(): MandateTextSpec {
        return MandateTextSpec(stringResId = R.string.stripe_sepa_mandate)
    }
}
