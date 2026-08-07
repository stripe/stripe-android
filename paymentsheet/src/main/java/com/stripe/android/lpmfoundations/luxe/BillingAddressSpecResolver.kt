package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AutomaticTaxBillingAddressSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.MandateSpec

internal object BillingAddressSpecResolver {
    /**
     * Resolves [CountrySpec] and [AddressSpec] before transforming them into form elements.
     *
     * The configured specs supply an absent, country-only, or normal-address source. Merchant
     * collection mode and billing-sourced automatic tax select whether that source is preserved,
     * normalized to an existing visible-country address, or replaced by a country-only,
     * tax-minimum, or Full address. A replacement keeps the source position and allowed countries;
     * a missing address is inserted before the first mandate. Full and tax-minimum forms contain at
     * most one element reporting `billing_details[address][country]`.
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
            addressSpec != null -> BillingAddressSourceResolver.Source.NormalAddress(
                hidesCountry = addressSpec.hideCountry,
            )
            countrySpecs.isNotEmpty() -> BillingAddressSourceResolver.Source.CountryOnly(
                allowedCountryCodes = countrySpecs.first().allowedCountryCodes,
            )
            else -> BillingAddressSourceResolver.Source.Absent
        }
        val result = BillingAddressSourceResolver.resolve(
            source = source,
            addressCollectionMode = addressCollectionMode,
            requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
            allowedBillingCountries = allowedBillingCountries,
        )

        return when (result) {
            BillingAddressSourceResolver.Result.PreserveExisting -> specs
            BillingAddressSourceResolver.Result.UseExistingNormalAddress -> specs.mapNotNull { spec ->
                when (spec) {
                    is CountrySpec -> null
                    is AddressSpec -> spec.copy(hideCountry = false)
                    else -> spec
                }
            }
            is BillingAddressSourceResolver.Result.CountryOnly -> specs
            is BillingAddressSourceResolver.Result.TaxMinimum -> specs.replaceCountryOrInsertAddress(
                AutomaticTaxBillingAddressSpec(result.allowedCountryCodes)
            )
            is BillingAddressSourceResolver.Result.Full -> specs.replaceCountryOrInsertAddress(
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
