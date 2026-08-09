package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class BillingAddressSourceResolverTest {
    @Test
    fun `absent source is preserved in Never`() {
        assertThat(resolve(source = ABSENT, mode = NEVER, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `absent source is preserved in tax-disabled Automatic`() {
        assertThat(resolve(source = ABSENT, mode = AUTOMATIC, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.PreserveExisting)
    }

    @Test
    fun `absent source emits tax minimum in billing-tax Automatic`() {
        assertThat(resolve(source = ABSENT, mode = AUTOMATIC, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.TaxMinimum(MERCHANT_COUNTRIES))
    }

    @Test
    fun `absent source emits full address in Full`() {
        assertThat(resolve(source = ABSENT, mode = FULL, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.Full(MERCHANT_COUNTRIES))
    }

    @Test
    fun `country-only source remains country-only in Never`() {
        assertThat(resolve(source = COUNTRY_ONLY, mode = NEVER, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.CountryOnly(LPM_COUNTRIES))
    }

    @Test
    fun `country-only source remains country-only in tax-disabled Automatic`() {
        assertThat(resolve(source = COUNTRY_ONLY, mode = AUTOMATIC, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.CountryOnly(LPM_COUNTRIES))
    }

    @Test
    fun `country-only source emits tax minimum in billing-tax Automatic`() {
        assertThat(resolve(source = COUNTRY_ONLY, mode = AUTOMATIC, requiresTaxAddress = true))
            .isEqualTo(BillingAddressSourceResolver.Result.TaxMinimum(LPM_COUNTRIES))
    }

    @Test
    fun `country-only source emits full address in Full`() {
        assertThat(resolve(source = COUNTRY_ONLY, mode = FULL, requiresTaxAddress = false))
            .isEqualTo(BillingAddressSourceResolver.Result.Full(LPM_COUNTRIES))
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
        val NEVER = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        val AUTOMATIC = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        val FULL = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
    }
}
