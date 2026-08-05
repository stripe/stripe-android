package com.stripe.android.crypto.onramp.example

internal const val ONRAMP_SOURCE_AMOUNT_MINOR_UNITS = 300L
internal val ONRAMP_SOURCE_AMOUNT = ONRAMP_SOURCE_AMOUNT_MINOR_UNITS / MINOR_UNITS_PER_USD.toDouble()
internal const val ONRAMP_SOURCE_CURRENCY = "usd"
internal const val ONRAMP_SOURCE_CURRENCY_CODE = "USD"
internal const val ONRAMP_DESTINATION_CURRENCY = "usdc"
internal const val ONRAMP_DESTINATION_NETWORK = "base"

private const val MINOR_UNITS_PER_USD = 100
