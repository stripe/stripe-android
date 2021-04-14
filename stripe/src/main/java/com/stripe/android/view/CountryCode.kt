package com.stripe.android.view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
internal class CountryCode(
    val twoLetters: String
) : Parcelable {
    internal fun isUS() = twoLetters.toUpperCase(Locale.getDefault()) == "US"
    fun isCA() = twoLetters.toUpperCase(Locale.getDefault()) == "CA"
    fun isGB() = twoLetters.toUpperCase(Locale.getDefault()) == "GB"

    companion object {
        fun isUS(countryCode: CountryCode?) = countryCode?.isUS() ?: false
        fun isCA(countryCode: CountryCode?) = countryCode?.isCA() ?: false
        fun isGB(countryCode: CountryCode?) = countryCode?.isGB() ?: false
    }
}