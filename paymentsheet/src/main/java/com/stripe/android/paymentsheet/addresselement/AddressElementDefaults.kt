package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.uicore.R

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
    "IT",
    "MX",
    "NO",
    "NL",
    "PL",
    "RU",
    "SE",
    "TR",
    "US",
    "ZA"
)

internal val AUTOCOMPLETE_ATTRIBUTION_DRAWABLE: (Boolean) -> Int? = { _ ->
    R.drawable.stripe_google_maps_logo
}
