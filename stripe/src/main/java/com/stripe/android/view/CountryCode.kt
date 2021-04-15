package com.stripe.android.view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
internal class CountryCode(
    private val letters: String,
    val twoLetters: String = letters.toUpperCase(Locale.getDefault())
) : Parcelable {
    internal fun isUS() = twoLetters == "US"
    internal fun isCA() = twoLetters == "CA"
    internal fun isGB() = twoLetters == "GB"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryCode

        if (twoLetters != other.twoLetters) return false

        return true
    }

    override fun hashCode(): Int {
        return twoLetters.hashCode()
    }

    companion object {
        fun isUS(countryCode: CountryCode?) = countryCode?.isUS() ?: false
        fun isCA(countryCode: CountryCode?) = countryCode?.isCA() ?: false
        fun isGB(countryCode: CountryCode?) = countryCode?.isGB() ?: false
    }
}
