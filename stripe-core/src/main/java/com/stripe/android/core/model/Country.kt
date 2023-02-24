package com.stripe.android.core.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class Country(
    val code: CountryCode,
    val name: String
) : Parcelable {
    constructor(code: String, name: String) : this(CountryCode.create(code), name)

    /**
     * @return display value for [CountryTextInputLayout] text view
     */
    override fun toString(): String = name
}
