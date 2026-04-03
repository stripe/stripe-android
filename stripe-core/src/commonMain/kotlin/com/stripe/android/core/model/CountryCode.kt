package com.stripe.android.core.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@CommonParcelize
@Serializable
data class CountryCode(
    val value: String
) : CommonParcelable {
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
