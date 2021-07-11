package com.stripe.android.paymentsheet.elements

internal data class Country(
    val code: CountryCode,
    val name: String
) {
    /**
     * @return Displayable country name
     */
    override fun toString(): String = name
}
