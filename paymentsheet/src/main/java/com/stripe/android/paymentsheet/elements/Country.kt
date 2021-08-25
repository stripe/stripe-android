package com.stripe.android.paymentsheet.elements

import com.stripe.android.model.CountryCode

internal data class Country(
    val code: CountryCode,
    val name: String
) {
    /**
     * @return Displayable country name
     */
    override fun toString(): String = name
}
