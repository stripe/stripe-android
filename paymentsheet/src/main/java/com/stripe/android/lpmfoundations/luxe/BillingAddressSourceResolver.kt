package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet

internal object BillingAddressSourceResolver {
    sealed interface Source {
        data object Absent : Source

        data class CountryOnly(
            val allowedCountryCodes: Set<String>,
        ) : Source

        data class NormalAddress(
            val hidesCountry: Boolean,
        ) : Source
    }

    sealed interface Result {
        data object PreserveExisting : Result

        data object UseExistingNormalAddress : Result

        data class CountryOnly(
            val allowedCountryCodes: Set<String>,
        ) : Result

        data class TaxMinimum(
            val allowedCountryCodes: Set<String>,
        ) : Result

        data class Full(
            val allowedCountryCodes: Set<String>,
        ) : Result
    }

    /**
     * Selects one billing-country input before either adapter creates form elements.
     *
     * [source] describes the LPM's configured address input. [addressCollectionMode] and
     * [requiresBillingAddressForAutomaticTax] add merchant and billing-sourced tax requirements.
     * Country-only sources keep their allowlist when they are promoted. Normal addresses are
     * preserved, but Full or tax-minimum collection exposes a hidden country and removes a separate
     * country input. Missing sources remain missing unless Full or tax-minimum collection requires one.
     */
    fun resolve(
        source: Source,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requiresBillingAddressForAutomaticTax: Boolean,
        allowedBillingCountries: Set<String>,
    ): Result {
        val requiresTaxMinimum =
            addressCollectionMode ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic &&
                requiresBillingAddressForAutomaticTax

        return when (source) {
            Source.Absent -> when {
                addressCollectionMode ==
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    Result.Full(allowedBillingCountries)
                }
                requiresTaxMinimum -> Result.TaxMinimum(allowedBillingCountries)
                else -> Result.PreserveExisting
            }
            is Source.CountryOnly -> when {
                addressCollectionMode ==
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    Result.Full(source.allowedCountryCodes)
                }
                requiresTaxMinimum -> Result.TaxMinimum(source.allowedCountryCodes)
                else -> Result.CountryOnly(source.allowedCountryCodes)
            }
            is Source.NormalAddress -> when {
                addressCollectionMode ==
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full ||
                    requiresTaxMinimum -> Result.UseExistingNormalAddress
                else -> Result.PreserveExisting
            }
        }
    }
}
