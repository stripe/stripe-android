package com.stripe.android.paymentsheet.addresselement

/**
 * Countries where autocomplete results have been validated for quality.
 * Merchants can override this with their own list if they want autocomplete
 * in additional countries, accepting responsibility for the experience there.
 */
internal val AUTOCOMPLETE_DEFAULT_COUNTRIES = setOf(
    "AU",
    "BE",
    "BR",
    "CA",
    "CH",
    "DE",
    "ES",
    "FR",
    "GB",
    "IE",
    "IN",
    "IT",
    "JP",
    "MX",
    "MY",
    "NL",
    "NO",
    "NZ",
    "PH",
    "PL",
    "RU",
    "SE",
    "SG",
    "TR",
    "US",
    "ZA"
)
