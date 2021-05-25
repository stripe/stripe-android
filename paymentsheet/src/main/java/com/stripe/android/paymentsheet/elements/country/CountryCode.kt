package com.stripe.android.paymentsheet.elements.country

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Locale

internal fun Locale.getCountryCode(): CountryCode = CountryCode.create(this.country)

@Parcelize
internal data class CountryCode private constructor(
    val value: String,
) : Parcelable {
    companion object {
        fun create(value: String) = CountryCode(value.toUpperCase(Locale.ROOT))
    }
}
