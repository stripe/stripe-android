package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AutomaticTaxBillingAddressSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.MandateSpec

internal object AutomaticTaxBillingAddressResolver {
    fun resolve(
        specs: List<FormItemSpec>,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requiresBillingAddressForAutomaticTax: Boolean,
        allowedBillingCountries: Set<String>,
    ): List<FormItemSpec> {
        val shouldCollectTaxBillingAddress =
            addressCollectionMode ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic &&
                requiresBillingAddressForAutomaticTax

        if (!shouldCollectTaxBillingAddress) {
            // Only automatic collection with billing-sourced tax needs a tax-minimum address.
            return specs
        }

        if (specs.any { it is AddressSpec }) {
            // A normal address remains the owner, so remove a separate country and expose its country field.
            return specs.mapNotNull { spec ->
                when {
                    spec is CountrySpec -> null
                    spec is AddressSpec && spec.hideCountry -> spec.copy(hideCountry = false)
                    else -> spec
                }
            }
        }

        val countrySpecIndex = specs.indexOfFirst { it is CountrySpec }
        if (countrySpecIndex >= 0) {
            val countrySpec = specs[countrySpecIndex] as CountrySpec
            return specs.mapIndexed { index, spec ->
                if (index == countrySpecIndex) {
                    AutomaticTaxBillingAddressSpec(
                        allowedCountryCodes = countrySpec.allowedCountryCodes,
                    )
                } else {
                    spec
                }
            }
        }

        // Without a country source, add the tax address before the mandate that must remain last.
        val resolvedSpecs = specs.toMutableList()
        val firstMandateIndex = specs.indexOfFirst { it is MandateSpec }
            .takeIf { it >= 0 }
            ?: specs.size
        resolvedSpecs.add(
            firstMandateIndex,
            AutomaticTaxBillingAddressSpec(
                allowedCountryCodes = allowedBillingCountries,
            )
        )
        return resolvedSpecs
    }
}
