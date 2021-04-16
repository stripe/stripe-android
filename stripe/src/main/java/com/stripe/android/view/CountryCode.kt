package com.stripe.android.view

import androidx.annotation.Size
import java.util.Locale

internal data class CountryCode(
    @Size(2) private val value: String,
) {
    val twoLetters: String
        get() = value.toUpperCase(Locale.getDefault())

    companion object {
        val US = CountryCode("US")
        val CA = CountryCode("CA")
        val GB = CountryCode("GB")
        fun isUS(countryCode: CountryCode?) = countryCode == US
        fun isCA(countryCode: CountryCode?) = countryCode == CA
        fun isGB(countryCode: CountryCode?) = countryCode == GB
    }
}
