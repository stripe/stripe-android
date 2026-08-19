package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AutomaticTaxBillingAddressSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.MandateSpec

internal object BillingAddressSpecResolver {
    /**
     * Reconciles authored country and address specs before element creation.
     *
     * Authored full addresses are preserved. Country specs are replaced in place, and a missing address is
     * inserted before the first mandate. The result contains at most one billing-country owner.
     */
    fun resolve(
        specs: List<FormItemSpec>,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requiresBillingAddressForAutomaticTax: Boolean,
        allowedBillingCountries: Set<String>,
    ): List<FormItemSpec> {
        val addressSpec = specs.filterIsInstance<AddressSpec>().firstOrNull()
        val countrySpec = specs.filterIsInstance<CountrySpec>().firstOrNull()
        val requiresTaxMinimum =
            addressCollectionMode ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic &&
                requiresBillingAddressForAutomaticTax

        if (addressSpec != null) {
            return if (
                addressSpec.hideCountry &&
                (
                    addressCollectionMode ==
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full ||
                        requiresTaxMinimum
                    )
            ) {
                specs.mapNotNull { spec ->
                    when (spec) {
                        is CountrySpec -> null
                        is AddressSpec -> spec.copy(hideCountry = false)
                        else -> spec
                    }
                }
            } else {
                specs
            }
        }

        val replacement = when {
            addressCollectionMode ==
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                AddressSpec(
                    allowedCountryCodes = countrySpec?.allowedCountryCodes ?: allowedBillingCountries,
                )
            }
            requiresTaxMinimum -> AutomaticTaxBillingAddressSpec(
                allowedCountryCodes = countrySpec?.allowedCountryCodes ?: allowedBillingCountries,
            )
            else -> return specs
        }

        return specs.replaceCountryOrInsertAddress(replacement)
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
