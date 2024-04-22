package com.stripe.android.uicore.address

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class AddressSchemas(
    private val schemaMap: Map<String, List<CountryAddressSchema>>,
    private val defaultCountryCode: String = "ZZ",
) : Parcelable {
    fun get(countryCode: String?): List<CountryAddressSchema>? {
        return countryCode?.let {
            schemaMap[it]
        } ?: schemaMap[defaultCountryCode]
    }

    internal fun elements(): AddressElements {
        return AddressElements(
            schemaMap = schemaMap,
            defaultCountryCode = defaultCountryCode,
        )
    }
}
