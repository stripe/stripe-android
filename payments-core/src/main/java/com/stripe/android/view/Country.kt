package com.stripe.android.view

import androidx.annotation.RestrictTo
import com.stripe.android.model.CountryCode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
data class Country(
    val code: CountryCode,
    val name: String
) {
    constructor(code: String, name: String) : this(CountryCode.create(code), name)

    /**
     * @return display value for [CountryTextInputLayout] text view
     */
    override fun toString(): String = name
}
