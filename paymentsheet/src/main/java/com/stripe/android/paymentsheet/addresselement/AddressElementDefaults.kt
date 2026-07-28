package com.stripe.android.paymentsheet.addresselement

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

/**
 * Extended list used when using Stripe-hosted autocomplete proxy endpoints.
 * These countries are enabled only for the Stripe-hosted endpoint to avoid exposing
 * merchant-owned API keys to broader usage.
 */
internal val AUTOCOMPLETE_STRIPE_HOSTED_DEFAULT_COUNTRIES: Set<String> =
    AUTOCOMPLETE_DEFAULT_COUNTRIES + setOf("IN", "JP", "MY", "NZ", "PH", "SG")
