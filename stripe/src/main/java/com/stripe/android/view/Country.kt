package com.stripe.android.view

internal data class Country(
    val code: CountryCode,
    val name: String
) {
    constructor(twoLetter: String, name: String) : this(CountryCode(twoLetter), name)

    /**
     * @return display value for [CountryTextInputLayout] text view
     */
    override fun toString(): String = name
}
