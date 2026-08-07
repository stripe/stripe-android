package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class BillingAddressSourceResolverTest {
    @Test
    fun `absent source is preserved in Never`() {
        assertThat(resolve(ABSENT, NEVER, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `absent source is preserved in tax-disabled Automatic`() {
        assertThat(resolve(ABSENT, AUTOMATIC, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `absent source emits tax minimum in billing-tax Automatic`() {
        assertThat(resolve(ABSENT, AUTOMATIC, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.TaxMinimum(MERCHANT_COUNTRIES))
    }

    @Test
    fun `absent source emits full address in Full`() {
        assertThat(resolve(ABSENT, FULL, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.Full(MERCHANT_COUNTRIES))
    }

    @Test
    fun `country-only source remains country-only in Never`() {
        assertThat(resolve(COUNTRY_ONLY, NEVER, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.CountryOnly(LPM_COUNTRIES))
    }

    @Test
    fun `country-only source remains country-only in tax-disabled Automatic`() {
        assertThat(resolve(COUNTRY_ONLY, AUTOMATIC, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.CountryOnly(LPM_COUNTRIES))
    }

    @Test
    fun `country-only source emits tax minimum in billing-tax Automatic`() {
        assertThat(resolve(COUNTRY_ONLY, AUTOMATIC, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.TaxMinimum(LPM_COUNTRIES))
    }

    @Test
    fun `country-only source emits full address in Full`() {
        assertThat(resolve(COUNTRY_ONLY, FULL, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.Full(LPM_COUNTRIES))
    }

    @Test
    fun `visible normal address is preserved in Never`() {
        assertThat(resolve(NORMAL_ADDRESS, NEVER, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `visible normal address is preserved in tax-disabled Automatic`() {
        assertThat(resolve(NORMAL_ADDRESS, AUTOMATIC, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `visible normal address is reused in billing-tax Automatic`() {
        assertThat(resolve(NORMAL_ADDRESS, AUTOMATIC, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.UseExistingNormalAddress)
    }

    @Test
    fun `visible normal address is reused in Full`() {
        assertThat(resolve(NORMAL_ADDRESS, FULL, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.UseExistingNormalAddress)
    }

    @Test
    fun `hidden-country normal address is preserved in Never`() {
        assertThat(resolve(HIDDEN_COUNTRY_ADDRESS, NEVER, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `hidden-country normal address is preserved in tax-disabled Automatic`() {
        assertThat(resolve(HIDDEN_COUNTRY_ADDRESS, AUTOMATIC, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `hidden-country normal address provides country in billing-tax Automatic`() {
        assertThat(resolve(HIDDEN_COUNTRY_ADDRESS, AUTOMATIC, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.UseExistingNormalAddress)
    }

    @Test
    fun `hidden-country normal address provides country in Full`() {
        assertThat(resolve(HIDDEN_COUNTRY_ADDRESS, FULL, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.UseExistingNormalAddress)
    }

    private fun resolve(
        source: BillingAddressSourceResolver.Source,
        mode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requiresTaxAddress: Boolean,
    ): BillingAddressSourceResolver.Result {
        return BillingAddressSourceResolver.resolve(
            source = source,
            addressCollectionMode = mode,
            requiresBillingAddressForAutomaticTax = requiresTaxAddress,
            allowedBillingCountries = MERCHANT_COUNTRIES,
        )
    }

    private companion object {
        val MERCHANT_COUNTRIES = setOf("US", "CA")
        val LPM_COUNTRIES = setOf("DE", "FR")
        val ABSENT = BillingAddressSourceResolver.Source.Absent
        val COUNTRY_ONLY = BillingAddressSourceResolver.Source.CountryOnly(LPM_COUNTRIES)
        val NORMAL_ADDRESS = BillingAddressSourceResolver.Source.NormalAddress(
            hidesCountry = false,
        )
        val HIDDEN_COUNTRY_ADDRESS = BillingAddressSourceResolver.Source.NormalAddress(
            hidesCountry = true,
        )
        val NEVER = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        val AUTOMATIC = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        val FULL = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
    }
}
