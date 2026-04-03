package com.stripe.android.core.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@CommonParcelize
data class Country(
    val code: CountryCode,
    val name: String
) : CommonParcelable {
    constructor(code: String, name: String) : this(CountryCode.create(code), name)

    /**
     * @return display value for [CountryTextInputLayout] text view
     */
    override fun toString(): String = name
}
