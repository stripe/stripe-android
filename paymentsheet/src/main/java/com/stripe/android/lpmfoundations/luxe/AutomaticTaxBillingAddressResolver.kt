package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AutomaticTaxBillingAddressSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.MandateSpec

internal object AutomaticTaxBillingAddressResolver {
    /**
     * Resolves specs to one billing-address owner before transforming them into form elements.
     *
     * The configured specs supply an absent, country-only, or normal-address source. Merchant
     * collection mode and billing-sourced automatic tax select whether that source is preserved,
     * normalized to an existing visible-country address, or replaced by a country-only,
     * tax-minimum, or Full address. A replacement keeps the source position and allowed countries;
     * a missing address is inserted before the first mandate. The result never retains a separate
     * country owner beside an address selected for Full or tax-minimum collection.
     */
    fun resolve(
        specs: List<FormItemSpec>,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requiresBillingAddressForAutomaticTax: Boolean,
        allowedBillingCountries: Set<String>,
    ): List<FormItemSpec> {
        val addressSpec = specs.filterIsInstance<AddressSpec>().firstOrNull()
        val countrySpecs = specs.filterIsInstance<CountrySpec>()
        val source = when {
            addressSpec != null -> BillingAddressResolver.Source.NormalAddress(
                hidesCountry = addressSpec.hideCountry,
            )
            countrySpecs.isNotEmpty() -> BillingAddressResolver.Source.CountryOnly(
                allowedCountryCodes = countrySpecs.first().allowedCountryCodes,
            )
            else -> BillingAddressResolver.Source.Absent
        }
        val result = BillingAddressResolver.resolve(
            source = source,
            addressCollectionMode = addressCollectionMode,
            requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
            allowedBillingCountries = allowedBillingCountries,
        )

        return when (result) {
            BillingAddressResolver.Result.PreserveExisting -> specs
            BillingAddressResolver.Result.UseExistingNormalAddress -> specs.mapNotNull { spec ->
                when (spec) {
                    is CountrySpec -> null
                    is AddressSpec -> spec.copy(hideCountry = false)
                    else -> spec
                }
            }
            is BillingAddressResolver.Result.CountryOnly -> specs
            is BillingAddressResolver.Result.TaxMinimum -> specs.replaceCountryOrInsertAddress(
                AutomaticTaxBillingAddressSpec(result.allowedCountryCodes)
            )
            is BillingAddressResolver.Result.Full -> specs.replaceCountryOrInsertAddress(
                AddressSpec(allowedCountryCodes = result.allowedCountryCodes)
            )
        }
    }

    private fun List<FormItemSpec>.replaceCountryOrInsertAddress(
        addressSpec: FormItemSpec,
    ): List<FormItemSpec> {
        val firstCountryIndex = indexOfFirst { it is CountrySpec }
        if (firstCountryIndex >= 0) {
            return mapIndexedNotNull { index, spec ->
                when {
                    index == firstCountryIndex -> addressSpec
                    spec is CountrySpec -> null
                    else -> spec
                }
            }
        }

        val firstMandateIndex = indexOfFirst { it is MandateSpec }
            .takeIf { it >= 0 }
            ?: size
        return toMutableList().apply {
            add(firstMandateIndex, addressSpec)
        }
    }
}
