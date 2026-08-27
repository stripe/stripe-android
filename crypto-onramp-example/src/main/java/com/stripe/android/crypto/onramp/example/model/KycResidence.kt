package com.stripe.android.crypto.onramp.example.model

import com.stripe.android.crypto.onramp.model.IdType

/**
 * Represents the residence modes selectable in the example KYC form.
 *
 * The selected residence configures national ID collection, prefills the editable address
 * country, preserves the EU-specific flow, and determines the initial local source currency.
 */
internal enum class KycResidence(
    val displayName: String,
    val countryCode: String?,
    val nationalIdConfiguration: NationalIdConfiguration?,
) {
    UnitedStates(
        displayName = "United States",
        countryCode = "US",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.SocialSecurityNumber,
            label = "Social Security Number",
            defaultValue = "000000000",
        ),
    ),
    EuropeanUnion(
        displayName = "European Union",
        countryCode = null,
        nationalIdConfiguration = null,
    ),
    Canada(
        displayName = "Canada",
        countryCode = "CA",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.CanadianSocialInsuranceNumber,
            label = "Social Insurance Number (SIN)",
            defaultValue = "000000000",
        ),
    ),
    Colombia(
        displayName = "Colombia",
        countryCode = "CO",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.ColombianTaxIdentificationNumber,
            label = "Número de Identificación Tributaria (NIT)",
            defaultValue = "0000000000",
        ),
    ),
    Philippines(
        displayName = "Philippines",
        countryCode = "PH",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.PhilippinesTaxpayerIdentificationNumber,
            label = "Taxpayer Identification Number (TIN)",
            defaultValue = "000000000000",
        ),
    ),
    ;

    val followsEuFlow: Boolean
        get() = this == EuropeanUnion

    val requiresState: Boolean
        get() = when (this) {
            UnitedStates, Canada, Colombia, Philippines -> true
            EuropeanUnion -> false
        }

    val localCurrency: SourceCurrency
        get() = when (this) {
            UnitedStates -> SourceCurrency.USD
            EuropeanUnion -> SourceCurrency.EUR
            Canada -> SourceCurrency.CAD
            Colombia -> SourceCurrency.COP
            Philippines -> SourceCurrency.PHP
        }
}

internal data class NationalIdConfiguration(
    val type: IdType,
    val label: String,
    val defaultValue: String,
)
