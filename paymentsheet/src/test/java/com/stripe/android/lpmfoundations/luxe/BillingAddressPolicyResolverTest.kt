package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class BillingAddressPolicyResolverTest {
    @Test
    fun `no payment method policy with Automatic resolves absent`() {
        assertThat(resolve(policy = null, mode = Mode.Automatic))
            .isEqualTo(ResolvedBillingAddress.Absent)
    }

    @Test
    fun `no payment method policy with Full resolves full address using merchant countries`() {
        assertThat(resolve(policy = null, mode = Mode.Full))
            .isEqualTo(ResolvedBillingAddress.Full(MERCHANT_COUNTRIES))
    }

    @Test
    fun `no payment method policy with Never resolves absent`() {
        assertThat(resolve(policy = null, mode = Mode.Never))
            .isEqualTo(ResolvedBillingAddress.Absent)
    }

    @Test
    fun `country-only policy with Automatic resolves country-only using policy countries`() {
        assertThat(resolve(policy = countryOnly(), mode = Mode.Automatic))
            .isEqualTo(ResolvedBillingAddress.CountryOnly(POLICY_COUNTRIES))
    }

    @Test
    fun `country-only policy with Full resolves full address using policy countries`() {
        assertThat(resolve(policy = countryOnly(), mode = Mode.Full))
            .isEqualTo(ResolvedBillingAddress.Full(POLICY_COUNTRIES))
    }

    @Test
    fun `country-only policy with Never resolves country-only using policy countries`() {
        assertThat(resolve(policy = countryOnly(), mode = Mode.Never))
            .isEqualTo(ResolvedBillingAddress.CountryOnly(POLICY_COUNTRIES))
    }

    @Test
    fun `full-address-if-allowed policy with Automatic resolves full address using policy countries`() {
        assertThat(resolve(policy = fullAddressIfAllowed(), mode = Mode.Automatic))
            .isEqualTo(ResolvedBillingAddress.Full(POLICY_COUNTRIES))
    }

    @Test
    fun `full-address-if-allowed policy with Full resolves full address using policy countries`() {
        assertThat(resolve(policy = fullAddressIfAllowed(), mode = Mode.Full))
            .isEqualTo(ResolvedBillingAddress.Full(POLICY_COUNTRIES))
    }

    @Test
    fun `full-address-if-allowed policy with Never resolves absent`() {
        assertThat(resolve(policy = fullAddressIfAllowed(), mode = Mode.Never))
            .isEqualTo(ResolvedBillingAddress.Absent)
    }

    @Test
    fun `suppressed policy with Automatic resolves suppressed`() {
        assertThat(resolve(policy = PaymentMethodBillingAddressPolicy.Suppressed, mode = Mode.Automatic))
            .isEqualTo(ResolvedBillingAddress.Suppressed)
    }

    @Test
    fun `suppressed policy with Full resolves suppressed`() {
        assertThat(resolve(policy = PaymentMethodBillingAddressPolicy.Suppressed, mode = Mode.Full))
            .isEqualTo(ResolvedBillingAddress.Suppressed)
    }

    @Test
    fun `suppressed policy with Never resolves suppressed`() {
        assertThat(resolve(policy = PaymentMethodBillingAddressPolicy.Suppressed, mode = Mode.Never))
            .isEqualTo(ResolvedBillingAddress.Suppressed)
    }

    @Test
    fun `automatic tax promotes absent to tax minimum using merchant countries`() {
        assertThat(resolveWithAutomaticTax(policy = null, mode = Mode.Automatic))
            .isEqualTo(ResolvedBillingAddress.TaxMinimum(MERCHANT_COUNTRIES))
    }

    @Test
    fun `automatic tax promotes country-only to tax minimum using policy countries`() {
        assertThat(resolveWithAutomaticTax(policy = countryOnly(), mode = Mode.Automatic))
            .isEqualTo(ResolvedBillingAddress.TaxMinimum(POLICY_COUNTRIES))
    }

    @Test
    fun `automatic tax preserves full address`() {
        assertThat(resolveWithAutomaticTax(policy = fullAddressIfAllowed(), mode = Mode.Automatic))
            .isEqualTo(ResolvedBillingAddress.Full(POLICY_COUNTRIES))
    }

    @Test
    fun `automatic tax preserves suppressed`() {
        assertThat(
            resolveWithAutomaticTax(
                policy = PaymentMethodBillingAddressPolicy.Suppressed,
                mode = Mode.Automatic,
            )
        ).isEqualTo(ResolvedBillingAddress.Suppressed)
    }

    private fun resolve(
        policy: PaymentMethodBillingAddressPolicy?,
        mode: Mode,
    ): ResolvedBillingAddress {
        return BillingAddressPolicyResolver(
            policy = policy,
            addressCollectionMode = mode,
            requiresBillingAddressForAutomaticTax = false,
            merchantAllowedCountryCodes = MERCHANT_COUNTRIES,
        ).resolve()
    }

    private fun resolveWithAutomaticTax(
        policy: PaymentMethodBillingAddressPolicy?,
        mode: Mode,
    ): ResolvedBillingAddress {
        return BillingAddressPolicyResolver(
            policy = policy,
            addressCollectionMode = mode,
            requiresBillingAddressForAutomaticTax = true,
            merchantAllowedCountryCodes = MERCHANT_COUNTRIES,
        ).resolve()
    }

    private fun countryOnly() = PaymentMethodBillingAddressPolicy.CountryOnly(POLICY_COUNTRIES)

    private fun fullAddressIfAllowed() =
        PaymentMethodBillingAddressPolicy.FullAddressIfAllowed(POLICY_COUNTRIES)

    private companion object {
        val POLICY_COUNTRIES = setOf("DE", "FR")
        val MERCHANT_COUNTRIES = setOf("US", "CA")
    }
}

private typealias Mode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
