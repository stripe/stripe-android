package com.stripe.android.crypto.onramp.example.ui.screens

import com.stripe.android.crypto.onramp.model.IdType

/**
 * Represents the residence modes selectable in the example KYC form.
 */
internal enum class KycResidence(
    val displayName: String,
    val countryCode: String?,
    val nationalIdConfiguration: NationalIdConfiguration?,
    val followsEuFlow: Boolean,
) {
    UnitedStates(
        displayName = "United States",
        countryCode = "US",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.SocialSecurityNumber,
            label = "Social Security Number",
        ),
        followsEuFlow = false,
    ),
    EuropeanUnion(
        displayName = "European Union",
        countryCode = null,
        nationalIdConfiguration = null,
        followsEuFlow = true,
    ),
    Canada(
        displayName = "Canada",
        countryCode = "CA",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.CanadianSocialInsuranceNumber,
            label = "Social Insurance Number (SIN)",
        ),
        followsEuFlow = false,
    ),
    Colombia(
        displayName = "Colombia",
        countryCode = "CO",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.ColombianTaxIdentificationNumber,
            label = "Número de Identificación Tributaria (NIT)",
        ),
        followsEuFlow = false,
    ),
    Philippines(
        displayName = "Philippines",
        countryCode = "PH",
        nationalIdConfiguration = NationalIdConfiguration(
            type = IdType.PhilippinesTaxpayerIdentificationNumber,
            label = "Taxpayer Identification Number (TIN)",
        ),
        followsEuFlow = false,
    ),
}

internal data class NationalIdConfiguration(
    val type: IdType,
    val label: String,
)
