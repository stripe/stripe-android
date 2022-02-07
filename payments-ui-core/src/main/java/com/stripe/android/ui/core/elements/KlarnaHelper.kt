package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object KlarnaHelper {
    private val currencyToAllowedCountries = mapOf(
        "eur" to setOf("AT", "FI", "DE", "NL", "BE", "ES", "IT", "FR"),
        "dkk" to setOf("DK"),
        "nok" to setOf("NO"),
        "sek" to setOf("SE"),
        "gbp" to setOf("GB"),
        "usd" to setOf("US")
    )

    private val buyNowCountries = setOf("AT", "BE", "DE", "IT", "NL", "ES", "SE")

    fun getAllowedCountriesForCurrency(currencyCode: String?) =
        currencyToAllowedCountries[currencyCode].orEmpty()

    fun getKlarnaHeader(locale: Locale = Locale.getDefault()) =
        if (buyNowCountries.contains(locale.country)) {
            R.string.klarna_buy_now_pay_later
        } else {
            R.string.klarna_pay_later
        }
}
