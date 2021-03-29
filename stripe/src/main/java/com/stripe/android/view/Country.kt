package com.stripe.android.view

internal data class Country(
    val code: String,
    val name: String
) {

    /**
     * @return display value for [CountryTextInputLayout] text view
     */
    override fun toString(): String = name
}
