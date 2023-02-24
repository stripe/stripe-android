package com.stripe.android.core.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Locale.getCountryCode(): CountryCode = CountryCode.create(this.country)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
@Serializable
data class CountryCode(
    val value: String
) : Parcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        val US = CountryCode("US")
        val CA = CountryCode("CA")
        val GB = CountryCode("GB")
        fun isUS(countryCode: CountryCode?) = countryCode == US
        fun isCA(countryCode: CountryCode?) = countryCode == CA
        fun isGB(countryCode: CountryCode?) = countryCode == GB

        fun create(value: String) = CountryCode(value.uppercase())
    }
}
