package com.stripe.android.paymentsheet.elements.country

import java.util.Locale

internal fun Locale.getCountryCode(): CountryCode = CountryCode.create(this.country)

internal data class CountryCode private constructor(
    val value: String,
) {
    companion object {
        fun create(value: String) = CountryCode(value.uppercase())
    }
}
