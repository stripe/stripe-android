package com.stripe.android.model

internal data class Country(
    val code: CountryCode,
    val name: String
) {
    constructor(code: String, name: String) : this(CountryCode.create(code), name)

    /**
     * @return display value for [CountryTextInputLayout] text view
     */
    override fun toString(): String = name
}
