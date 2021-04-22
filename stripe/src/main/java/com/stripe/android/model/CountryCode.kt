package com.stripe.android.model

import java.util.Locale

internal fun Locale.getCountryCode(): CountryCode = CountryCode.create(this.country)

internal data class CountryCode private constructor(
    val value: String,
) {
    companion object {
        val US = CountryCode("US")
        val CA = CountryCode("CA")
        val GB = CountryCode("GB")
        fun isUS(countryCode: CountryCode?) = countryCode == US
        fun isCA(countryCode: CountryCode?) = countryCode == CA
        fun isGB(countryCode: CountryCode?) = countryCode == GB

        fun create(value: String) = CountryCode(value.toUpperCase(Locale.ROOT))
    }
}
