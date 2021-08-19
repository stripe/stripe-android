package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import java.util.Locale

internal fun Locale.getCountryCode(): CountryCode = CountryCode.create(this.country)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
@Parcelize
data class CountryCode(
    val value: String,
) : Parcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
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
