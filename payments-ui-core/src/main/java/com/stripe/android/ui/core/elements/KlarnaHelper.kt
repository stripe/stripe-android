package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.R
import java.util.Locale

internal object KlarnaHelper {
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
            R.string.stripe_paymentsheet_klarna_buy_now_pay_later
        } else {
            R.string.stripe_paymentsheet_klarna_pay_later
        }
}
